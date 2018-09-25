/*
 * Backyard Brains Android App
 * Copyright (C) 2011 Backyard Brains
 * by Nathan Dotz <nate (at) backyardbrains.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.backyardbrains.drawing;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.ViewConfiguration;
import com.backyardbrains.BybApplication;
import com.backyardbrains.view.ZoomButton;
import java.util.concurrent.atomic.AtomicInteger;

import static android.view.MotionEvent.TOOL_TYPE_FINGER;
import static android.view.MotionEvent.TOOL_TYPE_MOUSE;
import static android.view.MotionEvent.TOOL_TYPE_STYLUS;
import static android.view.MotionEvent.TOOL_TYPE_UNKNOWN;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class InteractiveGLSurfaceView extends GLSurfaceView {

    @SuppressWarnings("unused") private static final String TAG = makeLogTag(InteractiveGLSurfaceView.class);

    private static final int LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();
    private static final int LONG_PRESS_X_OFFSET = 3;

    public static final int MODE_ZOOM_IN_H = 0;
    public static final int MODE_ZOOM_OUT_H = 1;
    public static final int MODE_ZOOM_IN_V = 2;
    public static final int MODE_ZOOM_OUT_V = 3;

    BaseWaveformRenderer renderer;

    protected ScaleGestureDetector scaleDetector;
    protected ScaleGestureDetector.OnScaleGestureListener scaleListener;
    protected GestureDetector scrollDetector;
    protected GestureDetector.SimpleOnGestureListener scrollListener = new GestureDetector.SimpleOnGestureListener() {

        @Override public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (renderer.isScrollEnabled()) {
                // if we are currently scrolling update the viewport and refresh the display
                if (scrolling) renderer.scroll(-distanceX);

                return scrolling;
            }

            return false;
        }
    };

    private boolean bZoomButtonsEnabled = false;
    float scalingFactor = 0.5f;
    float scalingFactorOut;
    float scalingFactorIn;

    // Implementation of long press
    private abstract static class LongPressRunnable implements Runnable {

        AtomicInteger eventX = new AtomicInteger();

        void setEventX(int x) {
            this.eventX.set(x);
        }
    }

    // Whether we are waiting for long press to start measurement
    boolean waitingForLongPress = false;
    // Whether we are currently scrolling
    boolean scrolling = true;

    private final Handler handler = new Handler();
    private final LongPressRunnable longPress = new LongPressRunnable() {
        @Override public void run() {
            if (waitingForLongPress) {
                waitingForLongPress = false;
                scrolling = false;
                renderer.startMeasurement(eventX.get());
            }
        }
    };

    public InteractiveGLSurfaceView(Context context) {
        this(context, null);
    }

    public InteractiveGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    /**
     * Sets the renderer associated with this view. Also starts the thread that will call the renderer, which in turn
     * causes the rendering to start.
     * <p>This method should be called once and only once in the life-cycle of a GLSurfaceView.
     * <p>The following GLSurfaceView methods can only be called <em>before</em> setRenderer is called:
     * <ul>
     *
     * <li>{@link #setEGLConfigChooser(boolean)}
     * <li>{@link #setEGLConfigChooser(EGLConfigChooser)}
     * <li>{@link #setEGLConfigChooser(int, int, int, int, int, int)}
     * </ul>
     * <p>
     * The following GLSurfaceView methods can only be called <em>after</em> setRenderer is called:
     * <ul>
     * <li>{@link #getRenderMode()}
     * <li>{@link #onPause()}
     * <li>{@link #onResume()}
     * <li>{@link #queueEvent(Runnable)}
     * <li>{@link #requestRender()}
     * <li>{@link #setRenderMode(int)}
     * </ul>
     *
     * @param renderer the renderer to use to perform OpenGL drawing.
     */
    public void setRenderer(@NonNull BaseWaveformRenderer renderer) {
        this.renderer = renderer;

        scaleListener = new ScaleListener(renderer);
        scaleDetector = new ScaleGestureDetector(getContext(), scaleListener);
        scrollDetector = new GestureDetector(getContext(), scrollListener);
        scrollDetector.setIsLongpressEnabled(false);

        setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        super.setRenderer(renderer);
    }

    @Override public final void setRenderer(Renderer renderer) {
        if (renderer instanceof BaseWaveformRenderer) {
            setRenderer((BaseWaveformRenderer) renderer);
            return;
        }

        throw new IllegalArgumentException("Renderer needs to be instance of BaseWaveformRenderer class");
    }

    @Override public void surfaceCreated(SurfaceHolder holder) {
        super.surfaceCreated(holder);
        setKeepScreenOn(true);
    }

    @Override public void surfaceDestroyed(SurfaceHolder holder) {
        if (bZoomButtonsEnabled) {
            bZoomButtonsEnabled = false;
            enableZoomButtonListeners(false);
        }

        setKeepScreenOn(false);
        super.surfaceDestroyed(holder);
    }

    @SuppressLint("ClickableViewAccessibility") @Override public boolean onTouchEvent(final MotionEvent event) {
        int pointerCount = event.getPointerCount();
        if (pointerCount > 0) {
            switch (event.getToolType(0)) {
                case TOOL_TYPE_UNKNOWN:
                case TOOL_TYPE_STYLUS:
                case TOOL_TYPE_MOUSE:
                    enableZoomButtons(true);
                    break;
                case TOOL_TYPE_FINGER:
                    enableZoomButtons(false);
                    break;
            }
        }
        if (renderer != null) {
            if (scaleDetector != null && scaleDetector.onTouchEvent(event) && scaleDetector.isInProgress()) {
                scrolling = false;
                if (renderer.isScrollEnabled()) renderer.endScroll();
                if (renderer.isMeasureEnabled()) {
                    waitingForLongPress = false;
                    handler.removeCallbacks(longPress);
                }
                return true;
            } else if (scrollDetector != null && pointerCount == 1 && !scrollDetector.onTouchEvent(event)) {
                // pass latest pointer x to long press runnable
                if (renderer.isMeasureEnabled()) longPress.setEventX((int) event.getX());
                // and manually handle the event.
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // start scrolling
                        scrolling = true;
                        if (renderer.isMeasureEnabled()) renderer.endMeasurement(event.getX());
                        if (renderer.isScrollEnabled()) renderer.startScroll();
                        // and start waiting for long-press
                        if (renderer.isMeasureEnabled()) {
                            waitingForLongPress = true;
                            handler.postDelayed(longPress, LONG_PRESS_TIMEOUT);
                            prevX = event.getX();
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (renderer.isMeasureEnabled()) {
                            // if we moved before long press, stop waiting for it
                            if (waitingForLongPress && Math.abs(prevX - event.getX()) > LONG_PRESS_X_OFFSET) {
                                waitingForLongPress = false;
                                handler.removeCallbacks(longPress);
                            }
                            // otherwise if we're not scrolling it means we are in measurement
                            if (!scrolling) renderer.measure(event.getX());

                            prevX = event.getX();
                        }
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        // reset all flags
                        scrolling = false;
                        if (renderer.isScrollEnabled()) renderer.endScroll();
                        if (renderer.isMeasureEnabled()) {
                            waitingForLongPress = false;
                            handler.removeCallbacks(longPress);
                        }
                        break;
                }
            }
        }

        return true;
    }

    float prevX;

    //==============================================
    //  PRIVATE METHODS
    //==============================================

    // Initializes the view
    private void init() {
        scalingFactorIn = 1 - scalingFactor;
        scalingFactorOut = 1 + scalingFactor;

        getHolder().setFormat(PixelFormat.RGBA_8888);

        if (getContext() != null) {
            boolean bHasTouch = ((BybApplication) getContext().getApplicationContext()).isTouchSupported();
            enableZoomButtons(!bHasTouch);
        }
    }

    void scaleRenderer(int zoomMode) {
        if (renderer != null && isScalingMode(zoomMode)) {
            float scaling = 1;
            if (isZoomIn(zoomMode)) {
                scaling = scalingFactorIn;
            } else if (isZoomOut(zoomMode)) {
                scaling = scalingFactorOut;
            }
            if (isScalingHorizontally(zoomMode)) {
                renderer.setGlWindowWidth((int) (renderer.getGlWindowWidth() * scaling));
            } else {
                renderer.setGlWindowHeight((int) (renderer.getGlWindowHeight() * scaling));
            }
        }
    }

    private boolean isZoomIn(int mode) {
        return mode == MODE_ZOOM_IN_H || mode == MODE_ZOOM_IN_V;
    }

    private boolean isZoomOut(int mode) {
        return mode == MODE_ZOOM_OUT_H || mode == MODE_ZOOM_OUT_V;
    }

    private boolean isScalingMode(int mode) {
        return isZoomIn(mode) || isZoomOut(mode);
    }

    private boolean isScalingHorizontally(int mode) {
        return (mode == MODE_ZOOM_IN_H || mode == MODE_ZOOM_OUT_H);
    }

    public void enableZoomButtons(boolean bEnable) {
        boolean bBroadcast = false;
        if (bEnable && !bZoomButtonsEnabled) {
            bZoomButtonsEnabled = true;
            enableZoomButtonListeners(true);
            bBroadcast = true;
        } else if (!bEnable && bZoomButtonsEnabled) {
            bZoomButtonsEnabled = false;
            enableZoomButtonListeners(false);
            bBroadcast = true;
        }
        if (getContext() != null && bBroadcast) {
            Intent i = new Intent();
            i.setAction("BYBShowZoomUI");
            i.putExtra("showUI", bZoomButtonsEnabled);
            getContext().sendBroadcast(i);
        }
    }

    private class ZoomButtonsListener extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("zoomMode")) {
                int zoomMode = intent.getIntExtra("zoomMode", MODE_ZOOM_IN_H);
                scaleRenderer(zoomMode);
            }
        }
    }

    private ZoomButtonsListener zoomButtonsListener;

    private void enableZoomButtonListeners(boolean reg) {
        if (getContext() != null) {
            if (reg) {
                IntentFilter intentFilter = new IntentFilter(ZoomButton.broadcastAction);
                zoomButtonsListener = new ZoomButtonsListener();
                getContext().registerReceiver(zoomButtonsListener, intentFilter);
            } else {
                getContext().unregisterReceiver(zoomButtonsListener);
            }
        }
    }
}
