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
import com.backyardbrains.BaseFragment;
import com.backyardbrains.audio.ThresholdHelper;
import com.backyardbrains.utils.BYBUtils;
import com.backyardbrains.utils.PrefUtils;
import java.nio.FloatBuffer;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class ThresholdRenderer extends WaveformRenderer {

    private static final String TAG = makeLogTag(ThresholdRenderer.class);

    private float threshold; // in samples, which is also gl width

    private Callback callback;

    interface Callback extends BYBBaseRenderer.Callback {
        void onThresholdUpdate(int value);
    }

    public static class CallbackAdapter extends BYBBaseRenderer.CallbackAdapter implements Callback {
        @Override public void onThresholdUpdate(int value) {
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

    @Override public void setGlWindowVerticalSize(int newSize) {
        super.setGlWindowVerticalSize(newSize);

        updateThresholdHandle();
    }

    @Override protected boolean fillBuffer() {
        if (getAudioService() != null) {
            drawingBuffer = new short[getAudioService().getAverageBuffer().length];
            System.arraycopy(getAudioService().getAverageBuffer(), 0, drawingBuffer, 0, drawingBuffer.length);
            return true;
        }
        return false;
    }

    @Override public void onLoadSettings(@NonNull Context context) {
        adjustThresholdValue(PrefUtils.getThreshold(context, getClass()));

        super.onLoadSettings(context);
    }

    @Override public void onSaveSettings(@NonNull Context context) {
        super.onSaveSettings(context);

        PrefUtils.setThreshold(context, getClass(), threshold);
    }

    @Override protected FloatBuffer getWaveformBuffer(short[] shortArrayToDraw) {
        if (getGlWindowHorizontalSize() > shortArrayToDraw.length) setGlWindowHorizontalSize(shortArrayToDraw.length);

        float[] arr = new float[getGlWindowHorizontalSize() * 2]; // array to fill
        int j = 0; // index of arr
        try {
            int start = (shortArrayToDraw.length - getGlWindowHorizontalSize()) / 2;
            int end = (shortArrayToDraw.length + getGlWindowHorizontalSize()) / 2;
            for (int i = start; i < end && i < shortArrayToDraw.length; i++) {
                arr[j++] = i - start;
                arr[j++] = shortArrayToDraw[i];
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, e.getMessage());
        }
        return BYBUtils.getFloatBufferFromFloatArray(arr, arr.length);
    }

    private void updateThresholdHandle() {
        if (callback != null) callback.onThresholdUpdate(glHeightToPixelHeight(threshold));
    }

    private void adjustThresholdValue(float dy) {
        Log.d(TAG, "adjustThresholdValue " + dy);
        if (dy == 0) return;

        threshold = dy;

        if (getAudioService() != null && getAudioService().getTriggerHandler() != null) {
            getAudioService().getTriggerHandler().post(new Runnable() {
                @Override public void run() {
                    ((ThresholdHelper.TriggerHandler) getAudioService().getTriggerHandler()).setThreshold(threshold);
                }
            });
        }
    }
}
