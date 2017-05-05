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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import com.backyardbrains.BackyardBrainsApplication;
import com.backyardbrains.view.BYBZoomButton;
import com.backyardbrains.view.BybScaleListener;
import com.backyardbrains.view.SingleFingerGestureDetector;

import static android.view.MotionEvent.TOOL_TYPE_FINGER;
import static android.view.MotionEvent.TOOL_TYPE_MOUSE;
import static android.view.MotionEvent.TOOL_TYPE_STYLUS;
import static android.view.MotionEvent.TOOL_TYPE_UNKNOWN;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class InteractiveGLSurfaceView extends GLSurfaceView {

    @SuppressWarnings("unused") private static final String TAG = makeLogTag(InteractiveGLSurfaceView.class);

    public static final int MODE_ZOOM_IN_H = 0;
    public static final int MODE_ZOOM_OUT_H = 1;
    public static final int MODE_MOVE = 2;
    public static final int MODE_ZOOM_IN_V = 3;
    public static final int MODE_ZOOM_OUT_V = 4;

    protected ScaleGestureDetector scaleDetector;
    protected ScaleGestureDetector.OnScaleGestureListener scaleListener;
    protected GestureDetector scrollDetector;
    protected GestureDetector.SimpleOnGestureListener scrollListener = new GestureDetector.SimpleOnGestureListener() {

        @Override public boolean onDown(MotionEvent e) {
            renderer.startAddToGlOffset();
            return true;
        }

        @Override public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            //float viewportOffsetX = distanceX * mCurrentViewport.width() / mContentRect.width();
            //float viewportOffsetY = -distanceY * mCurrentViewport.height() / mContentRect.height();
            // Updates the viewport, refreshes the display.
            renderer.addToGlOffset(-distanceX, distanceY);
            return true;
        }
    };
    protected SingleFingerGestureDetector singleFingerGestureDetector;

    BYBBaseRenderer renderer;

    private boolean bZoomButtonsEnabled = false;
    //	protected int nonTouchMode = -1;
    float startTouchX;
    float startTouchY;
    float minScalingDistance = 5;
    float scalingFactor = 0.5f;
    float scalingFactorOut;
    float scalingFactorIn;
    boolean bIsNonTouchScaling = false;

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
    public void setRenderer(@NonNull BYBBaseRenderer renderer) {
        this.renderer = renderer;

        scaleListener = new BybScaleListener(renderer);
        scaleDetector = new ScaleGestureDetector(getContext(), scaleListener);
        scrollDetector = new GestureDetector(getContext(), scrollListener);

        setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        super.setRenderer(renderer);
    }

    @Override public final void setRenderer(Renderer renderer) {
        if (renderer instanceof BYBBaseRenderer) {
            setRenderer((BYBBaseRenderer) renderer);
            return;
        }

        throw new IllegalArgumentException("Renderer needs to be instance of BYBBaseRenderer class");
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

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() > 0) {
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
        //Log.d(TAG, "onTouchEvent " + event.toString());
        if (renderer != null) {
            //if (singleFingerGestureDetector != null) {
            //    singleFingerGestureDetector.onTouchEvent(event);
            //    if (singleFingerGestureDetector.hasChanged()) {
            //        renderer.addToGlOffset(singleFingerGestureDetector.getDX(), singleFingerGestureDetector.getDY());
            //    }
            //}
            if (scaleDetector != null) {
                scaleDetector.onTouchEvent(event);
                if (scaleDetector.isInProgress()) return true;
            }
            if (scrollDetector != null) {
                if (!scrollDetector.onTouchEvent(event) && event.getAction() == MotionEvent.ACTION_UP) {
                    renderer.endAddToGlOffset();
                }
            }
        }
        return true;
    }

    // Initializes the view
    private void init() {
        //singleFingerGestureDetector = new SingleFingerGestureDetector(getContext());

        scalingFactorIn = 1 - scalingFactor;
        scalingFactorOut = 1 + scalingFactor;

        getHolder().setFormat(PixelFormat.RGBA_8888);

        if (getContext() != null) {
            boolean bHasTouch = ((BackyardBrainsApplication) getContext().getApplicationContext()).isTouchSupported();
            enableZoomButtons(!bHasTouch);
        }
    }

    private void scaleRenderer(int zoomMode) {
        if (renderer != null) {
            scaleRenderer(renderer.getSurfaceWidth() * 0.5f, zoomMode);
        }
    }

    private void scaleRenderer(float focusX, int zoomMode) {
        if (renderer != null && isScalingMode(zoomMode)) {
            float scaling = 1;
            if (isZoomIn(zoomMode)) {
                scaling = scalingFactorIn;
            } else if (isZoomOut(zoomMode)) {
                scaling = scalingFactorOut;
            }
            if (isScalingHorizontally(zoomMode)) {
                renderer.setGlWindowHorizontalSize((int) (renderer.getGlWindowHorizontalSize() * scaling));
                renderer.setScaleFocusX(focusX);
            } else {
                renderer.setGlWindowVerticalSize((int) (renderer.getGlWindowVerticalSize() * scaling));
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

    /*
        private void nonTouchScaling(MotionEvent event) {
            if(bZoomButtonsEnabled && renderer != null && isScalingMode()){
                final int action = event.getAction();
                float distanceX=0;
                float distanceY=0;
                if(bIsNonTouchScaling) {
                    distanceX = Math.abs(event.getX() - startTouchX);
                    distanceY = Math.abs(event.getY() - startTouchY);
                }
                switch (action & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        bIsNonTouchScaling = true;
                        startTouchX = event.getX();
                        startTouchY = event.getY();
                        break;
                    case MotionEvent.ACTION_UP:
                        if(bIsNonTouchScaling) {
                            if (isScalingHorizontally() && distanceX > minScalingDistance) {
                                    renderer.setGlWindowUnscaledHorizontalSize(distanceX);
                                    renderer.setScaleFocusX(distanceX / 2 + Math.min(event.getX(), startTouchX));
                            }else if (!isScalingHorizontally() && distanceY > minScalingDistance) {
                                    renderer.setGlWindowUnscaledVerticalSize(distanceY);
                            }else{
    //							renderer.setGlWindowHorizontalSize((int)(renderer.getGlWindowHorizontalSize()*zoomFactor));
    //							renderer.setScaleFocusX(startTouchX);
                                scaleRenderer(startTouchX);
                            }
                        }
                        renderer.hideScalingArea();
                        bIsNonTouchScaling = false;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if(bIsNonTouchScaling) {
                            if (isScalingHorizontally() && distanceX > minScalingDistance) {
                                renderer.showScalingAreaX(startTouchX, event.getX());
                            }else if (!isScalingHorizontally() && distanceY > minScalingDistance) {
                                renderer.showScalingAreaY(startTouchY, event.getY());
                            }
                        }
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        renderer.hideScalingArea();
                        bIsNonTouchScaling = false;
                        break;
                }
            }
        }
        //*/
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

    //	protected void showScalingInstructions() {
    //		if (context != null && isScalingMode()) {
    //			Intent i = new Intent();
    //			i.setAction("showScalingInstructions");
    //			context.sendBroadcast(i);
    //		}
    //	}
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
                IntentFilter intentFilter = new IntentFilter(BYBZoomButton.broadcastAction);
                zoomButtonsListener = new ZoomButtonsListener();
                getContext().registerReceiver(zoomButtonsListener, intentFilter);
            } else {
                getContext().unregisterReceiver(zoomButtonsListener);
            }
        }
    }
}
