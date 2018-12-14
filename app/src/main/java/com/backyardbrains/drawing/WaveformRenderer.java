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

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Size;
import android.util.SparseArray;
import android.view.MotionEvent;
import com.backyardbrains.drawing.gl.GlDraggableThreshold;
import com.backyardbrains.drawing.gl.GlDraggableWaveform;
import com.backyardbrains.drawing.gl.GlEventMarker;
import com.backyardbrains.drawing.gl.GlHandleDragHelper;
import com.backyardbrains.drawing.gl.GlHandleDragHelper.Rect;
import com.backyardbrains.dsp.SamplesWithEvents;
import com.backyardbrains.ui.BaseFragment;
import com.backyardbrains.utils.BYBUtils;
import com.backyardbrains.utils.JniUtils;
import com.backyardbrains.utils.PrefUtils;
import com.crashlytics.android.Crashlytics;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.LOGE;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class WaveformRenderer extends BaseWaveformRenderer {

    private static final String TAG = makeLogTag(WaveformRenderer.class);

    // Waveform channel colors
    private static final float[][] DEFAULT_WAVEFORM_COLOR =
        new float[][] { Colors.CHANNEL_1, Colors.CHANNEL_2, Colors.CHANNEL_3, Colors.CHANNEL_4, Colors.CHANNEL_5 };

    private final GlHandleDragHelper thresholdHandleDragHelper;

    private final GlDraggableWaveform glDraggableWaveform;
    private final GlDraggableThreshold glDraggableThreshold;

    private GlEventMarker glEventMarker;

    @SuppressWarnings("WeakerAccess") float threshold;

    public WaveformRenderer(@NonNull BaseFragment fragment) {
        super(fragment);

        context = fragment.getContext();

        glDraggableWaveform = new GlDraggableWaveform();
        glDraggableThreshold = new GlDraggableThreshold();

        thresholdHandleDragHelper = new GlHandleDragHelper(new GlHandleDragHelper.OnDragListener() {
            @Override public void onDragStart(int index) {
                // ignore
            }

            @Override public void onDrag(int index, float dy) {
                adjustThresholdValue(threshold - surfaceHeightToGlHeight(dy) / getWaveformScaleFactor());
            }

            @Override public void onDragStop(int index) {
            }
        });
    }

    //=================================================
    //  PUBLIC AND PROTECTED METHODS
    //=================================================

    /**
     * Returns the color of the waveform for the specified {@code channel} in rgba format. If color is not defined
     * green is returned.
     */
    protected @Size(4) float[] getWaveformColor(int channel) {
        channel = channel % 5;
        return DEFAULT_WAVEFORM_COLOR[DEFAULT_WAVEFORM_COLOR.length > channel ? channel : 0];
    }

    //=================================================
    //  Renderer INTERFACE IMPLEMENTATIONS
    //=================================================

    /**
     * {@inheritDoc}
     */
    @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        super.onSurfaceCreated(gl, config);

        glEventMarker = new GlEventMarker(context, gl);
    }

    /**
     * {@inheritDoc}
     */
    @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
        super.onSurfaceChanged(gl, width, height);

        thresholdHandleDragHelper.resetDraggableAreas();
        thresholdHandleDragHelper.setSurfaceHeight(height);

        //updateThresholdHandle();
    }

    //=================================================
    //  BaseWaveformRenderer OVERRIDES
    //=================================================

    /**
     * {@inheritDoc}
     */
    @Override public void onChannelCountChange(int channelCount) {
        super.onChannelCountChange(channelCount);

        thresholdHandleDragHelper.resetDraggableAreas();
    }

    /**
     * {@inheritDoc}
     */
    @Override public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event) || thresholdHandleDragHelper.onTouch(event);
    }

    @Override void setSelectedChannel(int selectedChannel) {
        super.setSelectedChannel(selectedChannel);

        thresholdHandleDragHelper.resetDraggableAreas();
    }

    /**
     * {@inheritDoc}
     */
    @Override public void onLoadSettings(@NonNull Context context) {
        super.onLoadSettings(context);

        adjustThresholdValue(PrefUtils.getThreshold(context, getClass()));
    }

    /**
     * {@inheritDoc}
     */
    @Override public void onSaveSettings(@NonNull Context context) {
        super.onSaveSettings(context);

        PrefUtils.setThreshold(context, getClass(), threshold);
    }

    /**
     * {@inheritDoc}
     */
    @Override protected void getWaveformVertices(@NonNull SamplesWithEvents samplesWithEvents,
        @NonNull short[][] samples, int frameCount, @NonNull int[] eventIndices, int eventCount, int fromSample,
        int toSample, int drawSurfaceWidth) {
        if (isSignalAveraging()) {
            try {
                JniUtils.prepareForThresholdDrawing(samplesWithEvents, samples, frameCount, eventIndices, eventCount,
                    fromSample, toSample, drawSurfaceWidth);
            } catch (ArrayIndexOutOfBoundsException e) {
                LOGE(TAG, e.getMessage());
                Crashlytics.logException(e);
            }
        } else {
            super.getWaveformVertices(samplesWithEvents, samples, frameCount, eventIndices, eventCount, fromSample,
                toSample, drawSurfaceWidth);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override protected void getEvents(@NonNull SamplesWithEvents samplesWithEvents, @NonNull String[] eventNames,
        int eventCount, @NonNull SparseArray<String> eventsBuffer) {
        // only process events if threshold is off
        if (!isSignalAveraging()) super.getEvents(samplesWithEvents, eventNames, eventCount, eventsBuffer);
    }

    /**
     * {@inheritDoc}
     */
    @Override protected void draw(GL10 gl, @NonNull short[][] samples, @NonNull short[][] waveformVertices,
        int[] waveformVerticesCount, @NonNull SparseArray<String> events, int surfaceWidth, int surfaceHeight,
        float glWindowWidth, float[] waveformScaleFactors, float[] waveformPositions, int drawStartIndex,
        int drawEndIndex, float scaleX, float scaleY, long lastSampleIndex) {
        final float samplesToDraw = waveformVerticesCount[0] * .5f;
        final float drawScale = surfaceWidth > 0 ? samplesToDraw / surfaceWidth : 1f;
        Rect rect = new Rect();

        // draw waveform
        for (int i = 0; i < waveformVertices.length; i++) {
            boolean showWaveformHandle = getChannelCount() > 1;
            boolean selected = getSelectedChanel() == i;
            boolean showThresholdHandle = selected && isSignalAveraging();
            glDraggableWaveform.draw(gl, waveformVertices[i], waveformVerticesCount[i], waveformScaleFactors[i],
                waveformPositions[i], drawScale, scaleY, getWaveformColor(i), selected, showWaveformHandle);
            if (showWaveformHandle) {
                glDraggableWaveform.getDragArea(rect);
                glHandleDragHelper.registerDraggableArea(i,
                    BYBUtils.map(rect.x, 0f, samplesToDraw - 1, 0f, surfaceWidth),
                    BYBUtils.map(rect.y, -MAX_GL_VERTICAL_HALF_SIZE, MAX_GL_VERTICAL_HALF_SIZE, 0f, surfaceHeight),
                    BYBUtils.map(rect.width, 0f, samplesToDraw - 1, 0f, surfaceWidth),
                    glHeightToSurfaceHeight(rect.height));
            }
            if (showThresholdHandle) {
                glDraggableThreshold.draw(gl, 0f, samplesToDraw - 1, threshold, waveformScaleFactors[i],
                    waveformPositions[i], drawScale, scaleY, Colors.RED);
                glDraggableThreshold.getDragArea(rect);
                thresholdHandleDragHelper.registerDraggableArea(i,
                    BYBUtils.map(rect.x, 0f, samplesToDraw - 1, 0f, surfaceWidth),
                    BYBUtils.map(rect.y, -MAX_GL_VERTICAL_HALF_SIZE, MAX_GL_VERTICAL_HALF_SIZE, 0f, surfaceHeight),
                    BYBUtils.map(rect.width, 0f, samplesToDraw - 1, 0f, surfaceWidth),
                    glHeightToSurfaceHeight(rect.height));
            }
        }

        // draw markers
        for (int i = 0; i < events.size(); i++) {
            glEventMarker.draw(gl, events.valueAt(i), events.keyAt(i), -MAX_GL_VERTICAL_HALF_SIZE,
                MAX_GL_VERTICAL_HALF_SIZE, drawScale, scaleY);
        }
    }

    //=================================================
    // PRIVATE METHODS
    //=================================================

    @SuppressWarnings("WeakerAccess") void adjustThresholdValue(float dy) {
        if (dy == 0) return;

        threshold = dy;

        // pass new threshold to the c++ code
        JniUtils.setThreshold(threshold);

        // TODO: 29-Nov-18 WHEN IN PLAYBACK WE SHOULD ALSO RESET AVERAGED SIGNAL
    }
}