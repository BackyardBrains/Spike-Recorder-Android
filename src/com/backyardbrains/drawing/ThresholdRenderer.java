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
import android.util.Log;
import android.util.SparseArray;
import com.backyardbrains.BaseFragment;
import com.backyardbrains.utils.PrefUtils;
import com.crashlytics.android.Crashlytics;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class ThresholdRenderer extends WaveformRenderer {

    private static final String TAG = makeLogTag(ThresholdRenderer.class);

    private float threshold;
    private float[] waveformVertices;

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

    public ThresholdRenderer(@NonNull BaseFragment fragment, @NonNull float[] preparedBuffer) {
        super(fragment, preparedBuffer);
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
     * Resets threshold to last saved value.
     */
    public void refreshThreshold() {
        adjustThresholdValue(threshold);
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
        adjustThresholdValue(PrefUtils.getThreshold(context, getClass()));

        super.onLoadSettings(context);
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
    @NonNull @Override protected float[] getWaveformVertices(@NonNull short[] samples, @NonNull String[] markers,
        @NonNull SparseArray<String> markerBuffer, int glWindowWidth, int drawStartIndex, int drawEndIndex) {
        if (glWindowWidth > samples.length) setGlWindowWidth(samples.length);
        glWindowWidth = getGlWindowWidth();

        int size = glWindowWidth * 2;
        if (waveformVertices == null || waveformVertices.length != size) waveformVertices = new float[size];
        int j = 0; // index of arr
        try {
            int start = (int) ((samples.length - glWindowWidth) * .5);
            int end = (int) ((samples.length + glWindowWidth) * .5);
            for (int i = start; i < end && i < samples.length; i++) {
                waveformVertices[j++] = i - start;
                waveformVertices[j++] = samples[i];
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, e.getMessage());
            Crashlytics.logException(e);
        }
        return waveformVertices;
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
