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

    private Callback callback;

    interface Callback extends BYBBaseRenderer.Callback {
        void onThresholdPositionChange(int position);

        void onThresholdValueChange(float value);
    }

    public static class CallbackAdapter extends BYBBaseRenderer.CallbackAdapter implements Callback {
        @Override public void onThresholdPositionChange(int position) {
        }

        @Override public void onThresholdValueChange(float value) {

        }
    }

    public ThresholdRenderer(@NonNull BaseFragment fragment, @NonNull float[] preparedBuffer) {
        super(fragment, preparedBuffer);
    }

    public void setCallback(Callback callback) {
        super.setCallback(callback);

        this.callback = callback;
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

    @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
        super.onSurfaceChanged(gl, width, height);

        updateThresholdHandle();
    }

    @Override public void setGlWindowHeight(int newSize) {
        super.setGlWindowHeight(newSize);

        updateThresholdHandle();
    }

    @Override public void onLoadSettings(@NonNull Context context) {
        adjustThresholdValue(PrefUtils.getThreshold(context, getClass()));

        super.onLoadSettings(context);
    }

    @Override public void onSaveSettings(@NonNull Context context) {
        super.onSaveSettings(context);

        PrefUtils.setThreshold(context, getClass(), threshold);
    }

    @NonNull @Override protected float[] updateWaveformBuffer(@NonNull short[] samples, @NonNull String[] markers,
        @NonNull SparseArray<String> markerBuffer, int glWindowWidth) {
        if (glWindowWidth > samples.length) setGlWindowWidth(samples.length);
        glWindowWidth = getGlWindowWidth();

        float[] arr = new float[glWindowWidth * 2]; // array to fill
        int j = 0; // index of arr
        try {
            int start = (int) ((samples.length - glWindowWidth) * .5);
            int end = (int) ((samples.length + glWindowWidth) * .5);
            for (int i = start; i < end && i < samples.length; i++) {
                arr[j++] = i - start;
                arr[j++] = samples[i];
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, e.getMessage());
            Crashlytics.logException(e);
        }
        return arr;
    }

    private void updateThresholdHandle() {
        if (callback != null) callback.onThresholdPositionChange(glHeightToPixelHeight(threshold));
    }

    private void adjustThresholdValue(float dy) {
        Log.d(TAG, "adjustThresholdValue " + dy);
        if (dy == 0) return;

        threshold = dy;

        if (callback != null) callback.onThresholdValueChange(threshold);
    }
}
