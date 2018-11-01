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
import com.backyardbrains.ui.BaseFragment;
import com.backyardbrains.dsp.SamplesWithEvents;
import com.backyardbrains.drawing.gl.GlEventMarker;
import com.backyardbrains.drawing.gl.GlWaveform;
import com.backyardbrains.utils.JniUtils;
import com.backyardbrains.utils.PrefUtils;
import com.crashlytics.android.Crashlytics;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.LOGE;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class WaveformRenderer extends BaseWaveformRenderer {

    private static final String TAG = makeLogTag(WaveformRenderer.class);

    private static final float[] DEFAULT_WAVEFORM_COLOR = new float[] { 0f, 1f, 0f, 1f };

    private GlWaveform glWaveform;
    private GlEventMarker glEventMarker;

    private float threshold;

    private OnThresholdChangeListener listener;

    /**
     * Interface definition for a callback to be invoked when threshold position or value are changed.
     */
    public interface OnThresholdChangeListener {
        /**
         * Listener that is invoked when threshold position is changed.
         *
         * @param position New threshold position.
         */
        void onThresholdPositionChange(int position);

        /**
         * Listener that is invoked when threshold value is changed.
         */
        void onThresholdValueChange(float value);
    }

    public WaveformRenderer(@NonNull BaseFragment fragment) {
        super(fragment);

        context = fragment.getContext();
    }

    /**
     * Registers a callback to be invoked when threshold position or value are changed.
     *
     * @param listener The callback that will be run. This value may be {@code null}.
     */
    public void setOnThresholdChangeListener(OnThresholdChangeListener listener) {
        this.listener = listener;
    }

    /**
     * Sets threshold to specified {@code y} value.
     */
    public void adjustThreshold(float y) {
        adjustThresholdValue(pixelHeightToGlHeight(y));
    }

    /**
     * {@inheritDoc}
     */
    @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        super.onSurfaceCreated(gl, config);

        glWaveform = new GlWaveform();
        glEventMarker = new GlEventMarker(context, gl);
    }

    /**
     * {@inheritDoc}
     */
    @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
        super.onSurfaceChanged(gl, width, height);

        updateThresholdHandle();
    }

    /**
     * {@inheritDoc}
     */
    @Override public void setGlWindowHeight(int newSize) {
        super.setGlWindowHeight(newSize);

        updateThresholdHandle();
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
    @Override protected void getWaveformVertices(@NonNull SamplesWithEvents samplesWithEvents, @NonNull short[] samples,
        @NonNull int[] eventIndices, @NonNull String[] eventNames, int eventCount,
        @NonNull SparseArray<String> eventsBuffer, int fromSample, int toSample, int drawSurfaceWidth) {
        if (isSignalAveraging()) {
            try {
                JniUtils.prepareForThresholdDrawing(samplesWithEvents, samples, eventIndices, eventCount, fromSample,
                    toSample, drawSurfaceWidth);
            } catch (ArrayIndexOutOfBoundsException e) {
                LOGE(TAG, e.getMessage());
                Crashlytics.logException(e);
            }
        } else {
            super.getWaveformVertices(samplesWithEvents, samples, eventIndices, eventNames, eventCount, eventsBuffer,
                fromSample, toSample, drawSurfaceWidth);
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
    @Override protected void draw(GL10 gl, @NonNull short[] samples, @NonNull short[] waveformVertices,
        int waveformVerticesCount, @NonNull SparseArray<String> events, int surfaceWidth, int surfaceHeight,
        int glWindowWidth, int glWindowHeight, int drawStartIndex, int drawEndIndex, float scaleX, float scaleY,
        long lastSampleIndex) {
        // draw waveform
        glWaveform.draw(gl, waveformVertices, waveformVerticesCount, getWaveformColor());
        // draw markers
        final float drawScale = (float) (waveformVerticesCount * .5) / surfaceWidth;
        final float verticalHalfSize = glWindowHeight * .5f;
        for (int i = 0; i < events.size(); i++) {
            glEventMarker.draw(gl, events.valueAt(i), events.keyAt(i), -verticalHalfSize, verticalHalfSize, drawScale,
                scaleY);
        }
    }

    /**
     * Returns the color of the waveform in rgba format. By default green is returned.
     */
    protected @Size(4) float[] getWaveformColor() {
        return DEFAULT_WAVEFORM_COLOR;
    }

    private void updateThresholdHandle() {
        if (listener != null) listener.onThresholdPositionChange(glHeightToPixelHeight(threshold));
    }

    private void adjustThresholdValue(float dy) {
        if (dy == 0) return;

        threshold = dy;

        if (listener != null) listener.onThresholdValueChange(threshold);
    }
}