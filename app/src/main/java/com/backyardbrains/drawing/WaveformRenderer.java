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
import com.backyardbrains.drawing.gl.GlDashedHLine;
import com.backyardbrains.drawing.gl.GlEventMarker;
import com.backyardbrains.drawing.gl.GlHandle;
import com.backyardbrains.drawing.gl.GlHandleDragHelper;
import com.backyardbrains.drawing.gl.GlHandleDragHelper.Rect;
import com.backyardbrains.drawing.gl.GlWaveform;
import com.backyardbrains.dsp.SamplesWithEvents;
import com.backyardbrains.ui.BaseFragment;
import com.backyardbrains.utils.JniUtils;
import com.backyardbrains.utils.PrefUtils;
import com.crashlytics.android.Crashlytics;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.LOGE;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class WaveformRenderer extends BaseWaveformRenderer {

    private static final String TAG = makeLogTag(WaveformRenderer.class);

    private static final float DASH_SIZE = 30f;
    private static final int LINE_WIDTH = 1;

    private final GlHandleDragHelper waveformHandleDragHelper;
    private final GlHandleDragHelper thresholdHandleDragHelper;
    private final Rect rect = new Rect();

    private final GlWaveform glWaveform;
    private final GlHandle glHandle;
    private final GlDashedHLine glThresholdLine;
    private GlEventMarker glEventMarker;

    private float threshold;

    public WaveformRenderer(@NonNull BaseFragment fragment) {
        super(fragment);

        context = fragment.getContext();

        waveformHandleDragHelper = new GlHandleDragHelper(new GlHandleDragHelper.OnDragListener() {
            @Override public void onDragStart(int index) {
                setSelectedChannel(index);
            }

            @Override public void onDrag(int index, float dy) {
                moveGlWindowForSelectedChannel(dy);
            }

            @Override public void onDragStop(int index) {
                // ignore
            }
        });
        thresholdHandleDragHelper = new GlHandleDragHelper(new GlHandleDragHelper.OnDragListener() {
            @Override public void onDragStart(int index) {
                // ignore
            }

            @Override public void onDrag(int index, float dy) {
                updateThreshold(dy);
            }

            @Override public void onDragStop(int index) {
                // ignore
            }
        });

        glWaveform = new GlWaveform();
        glHandle = new GlHandle();
        glThresholdLine = new GlDashedHLine();
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

        waveformHandleDragHelper.resetDraggableAreas();
        waveformHandleDragHelper.setSurfaceHeight(height);

        thresholdHandleDragHelper.resetDraggableAreas();
        thresholdHandleDragHelper.setSurfaceHeight(height);
    }

    //=================================================
    //  TouchEnabledRenderer INTERFACE IMPLEMENTATIONS
    //=================================================

    /**
     * {@inheritDoc}
     */
    @Override public boolean onTouchEvent(MotionEvent event) {
        return waveformHandleDragHelper.onTouch(event) || thresholdHandleDragHelper.onTouch(event);
    }

    //=================================================
    //  BaseWaveformRenderer OVERRIDES
    //=================================================

    /**
     * {@inheritDoc}
     */
    @Override public void onChannelCountChange(int channelCount) {
        super.onChannelCountChange(channelCount);

        // we should reset draggable areas for both waveforms and thresholds
        waveformHandleDragHelper.resetDraggableAreas();
        thresholdHandleDragHelper.resetDraggableAreas();
    }

    /**
     * {@inheritDoc}
     */
    @Override public void setSelectedChannel(int selectedChannel) {
        super.setSelectedChannel(selectedChannel);

        thresholdHandleDragHelper.resetDraggableAreas();
    }

    /**
     * {@inheritDoc}
     */
    @Override public void onLoadSettings(@NonNull Context context) {
        super.onLoadSettings(context);

        setThreshold(PrefUtils.getThreshold(context, getClass()));
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
    @Override protected void draw(GL10 gl, @NonNull short[][] samples, int selectedChannel,
        @NonNull short[][] waveformVertices, int[] waveformVerticesCount, @NonNull SparseArray<String> events,
        int surfaceWidth, int surfaceHeight, float glWindowWidth, float[] waveformScaleFactors,
        float[] waveformPositions, int drawStartIndex, int drawEndIndex, float scaleX, float scaleY,
        long lastFrameIndex) {
        final float samplesToDraw = waveformVerticesCount[0] * .5f;
        final float drawScale = surfaceWidth > 0 ? samplesToDraw / surfaceWidth : 1f;
        final boolean showWaveformHandle = getChannelCount() > 1;
        final boolean isSignalAveraging = isSignalAveraging();
        final boolean isThresholdSignalAveraging = isThresholdAveragingTriggerType();
        boolean selected, showThresholdHandle;

        for (int i = 0; i < waveformVertices.length; i++) {
            selected = getSelectedChanel() == i;
            showThresholdHandle = selected && isSignalAveraging && isThresholdSignalAveraging;

            gl.glPushMatrix();
            gl.glTranslatef(0f, waveformPositions[i], 0f);

            // draw waveform
            gl.glPushMatrix();
            gl.glScalef(1f, waveformScaleFactors[i], 1f);
            glWaveform.draw(gl, waveformVertices[i], waveformVerticesCount[i], getWaveformColor(i));
            gl.glPopMatrix();

            if (showWaveformHandle) {
                // draw waveform handle
                gl.glPushMatrix();
                gl.glScalef(drawScale, scaleY, 1f);
                glHandle.draw(gl, getWaveformColor(i), selected);
                gl.glPopMatrix();

                // register waveform handle as draggable area with drag helper
                glHandle.getBorders(rect);
                waveformHandleDragHelper.registerDraggableArea(i, rect.x, rect.y + glYToSurfaceY(waveformPositions[i]),
                    rect.width, rect.height);
            }

            if (showThresholdHandle) {
                float scaledThreshold = threshold * waveformScaleFactors[i];
                LOGD(TAG, "SCALED THRESHOLD : " + scaledThreshold);
                if (scaledThreshold + waveformPositions[i] < -MAX_GL_VERTICAL_HALF_SIZE) {
                    scaledThreshold = -MAX_GL_VERTICAL_HALF_SIZE - waveformPositions[i];
                }
                if (scaledThreshold + waveformPositions[i] > MAX_GL_VERTICAL_HALF_SIZE) {
                    scaledThreshold = MAX_GL_VERTICAL_HALF_SIZE - waveformPositions[i];
                }
                // draw threshold line
                gl.glPushMatrix();
                gl.glScalef(drawScale, waveformScaleFactors[i], 1f);
                gl.glTranslatef(0f, threshold, 0f);
                glThresholdLine.draw(gl, 0f, samplesToDraw - 1, DASH_SIZE, LINE_WIDTH, Colors.RED);
                gl.glPopMatrix();
                // draw threshold handle
                gl.glPushMatrix();
                gl.glTranslatef(samplesToDraw - 1, scaledThreshold, 0f);
                gl.glScalef(-drawScale, scaleY, 1f);
                glHandle.draw(gl, Colors.RED, true);
                gl.glPopMatrix();

                // register threshold handle as draggable area with drag helper
                glHandle.getBorders(rect);
                thresholdHandleDragHelper.registerDraggableArea(i, surfaceWidth - rect.width,
                    rect.y + glYToSurfaceY(waveformPositions[i] + scaledThreshold), rect.width, rect.height);
            }

            gl.glPopMatrix();
        }

        // draw markers
        if (!isSignalAveraging) {
            for (int i = 0; i < events.size(); i++) {
                gl.glPushMatrix();
                gl.glTranslatef(events.keyAt(i), -MAX_GL_VERTICAL_HALF_SIZE, 0f);
                glEventMarker.draw(gl, events.valueAt(i), MAX_GL_VERTICAL_SIZE, drawScale, scaleY);
                gl.glPopMatrix();
            }
        }
    }

    //=================================================
    // PRIVATE AND PACKAGE-PRIVATE METHODS
    //=================================================

    @SuppressWarnings("WeakerAccess") void updateThreshold(float dy) {
        setThreshold(threshold - surfaceHeightToGlHeight(dy) / getWaveformScaleFactor());
    }

    // Returns the color of the waveform for the specified channel in rgba format. If color is not defined green is returned.
    private @Size(4) float[] getWaveformColor(int channel) {
        channel = channel % Colors.CHANNEL_COLORS.length;
        return Colors.CHANNEL_COLORS[channel];
    }

    private void setThreshold(float threshold) {
        if (threshold == 0) return;

        this.threshold = threshold;

        // pass new threshold to the c++ code
        JniUtils.setThreshold(threshold);

        // TODO: 29-Nov-18 WHEN IN PLAYBACK WE SHOULD ALSO RESET AVERAGED SIGNAL
    }
}