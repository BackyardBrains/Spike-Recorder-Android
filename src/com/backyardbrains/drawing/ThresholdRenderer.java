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

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;
import com.backyardbrains.BYBGlUtils;
import com.backyardbrains.BYBUtils;
import com.backyardbrains.BaseFragment;
import com.backyardbrains.audio.TriggerAverager.TriggerHandler;
import java.nio.FloatBuffer;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utls.LogUtils.LOGD;
import static com.backyardbrains.utls.LogUtils.makeLogTag;

public class ThresholdRenderer extends WaveformRenderer {

    private static final String TAG = makeLogTag(ThresholdRenderer.class);

    private float threshold; // in sample value range, which happens to be also gl values

    public ThresholdRenderer(@NonNull BaseFragment fragment, @NonNull float[] preparedBuffer) {
        super(fragment, preparedBuffer);
    }

    // ---------------------------------------------------------------------------------------------
    @Override public void setGlWindowVerticalSize(int newY) {
        super.setGlWindowVerticalSize(newY);
        LOGD(TAG, "setGlWindowVerticalSize()");
        updateThresholdHandle();
    }

    private void updateThresholdHandle() {
        if (getContext() != null) {
            Intent i = new Intent();
            i.setAction("BYBUpdateThresholdHandle");
            i.putExtra("pos", getThresholdScreenValue());
            i.putExtra("name", "OsciloscopeHandle");
            getContext().sendBroadcast(i);
        }
    }

    // ----------------------------------------------------------------------------------------
    private boolean getCurrentAverage() {
        if (getAudioService() != null) {
            mBufferToDraws = getAudioService().getAverageBuffer();
            return true;
        }
        return false;
    }

    // ----------------------------------------------------------------------------------------
    @Override public void onDrawFrame(GL10 gl) {
        if (!getCurrentAverage()) return;
        if (!BYBUtils.isValidAudioBuffer(mBufferToDraws)) return;

        preDrawingHandler();
        BYBGlUtils.glClear(gl);
        drawingHandler(gl);
        postDrawingHandler(gl);
    }

    // ---------------------------------------------------------------------------------------------
    @Override protected FloatBuffer getWaveformBuffer(short[] shortArrayToDraw) {
        if (glWindowHorizontalSize > shortArrayToDraw.length) setGlWindowHorizontalSize(shortArrayToDraw.length);

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

    // ---------------------------------------------------------------------------------------------
    @Override protected void setmVText() {
        final float yPerDiv = threshold;
        super.setmVText(yPerDiv);
    }

    // ---------------------------------------------------------------------------------------------
    private int getThresholdScreenValue() {
        return glHeightToPixelHeight(threshold);
    }

    // ---------------------------------------------------------------------------------------------
    private void adjustThresholdValue(float dy) {
        Log.d(TAG, "adjustThresholdValue " + dy);
        if (dy == 0) {
            return;
        }
        threshold = dy;

        if (getAudioService() != null && getAudioService().getTriggerHandler() != null) {
            getAudioService().getTriggerHandler().post(new Runnable() {
                @Override public void run() {
                    ((TriggerHandler) getAudioService().getTriggerHandler()).setThreshold(threshold);
                }
            });
        }
    }

    // ---------------------------------------------------------------------------------------------
    public void adjustThreshold(float y) {
        adjustThresholdValue(pixelHeightToGlHeight(y));
    }

    public void saveThreshold(float y) {

    }

    // ---------------------------------------------------------------------------------------------
    public void refreshThreshold() {
        adjustThresholdValue(threshold);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- SETTINGS
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override public void readSettings(SharedPreferences settings, String TAG) {
        if (settings != null) {
            super.readSettings(settings, TAG);
            adjustThresholdValue(settings.getFloat(TAG + "_threshold", threshold));
            Log.w(TAG, "loadsetting threshold: " + threshold);
        }
    }

    // ----------------------------------------------------------------------------------------
    @Override public void saveSettings(SharedPreferences settings, String TAG) {
        if (settings != null) {
            super.saveSettings(settings, TAG);
            final SharedPreferences.Editor editor = settings.edit();
            Log.w(TAG, "savesetting threshold: " + threshold);
            editor.putFloat(TAG + "_threshold", threshold);
            editor.apply();
        }
    }
}
