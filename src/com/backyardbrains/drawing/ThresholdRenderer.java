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
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;
import com.backyardbrains.BYBGlUtils;
import com.backyardbrains.BYBUtils;
import com.backyardbrains.audio.TriggerAverager.TriggerHandler;
import java.nio.FloatBuffer;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utls.LogUtils.makeLogTag;

public class ThresholdRenderer extends WaveformRenderer {

    private static final String TAG = makeLogTag(ThresholdRenderer.class);

    private float threshold;    // in sample value range, which happens to be also gl values
    private float tempThreshold;
    private boolean bIsFirstFrame;
    AdjustThresholdListener adjustThresholdListener;

    public ThresholdRenderer(@NonNull Context context, @NonNull float[] preparedBuffer) {
        super(context, preparedBuffer);

        registerAdjustThresholdReceiver(true);
        bIsFirstFrame = true;
    }

    // ---------------------------------------------------------------------------------------------
    @Override public void close() {
        registerAdjustThresholdReceiver(false);
    }

    // ---------------------------------------------------------------------------------------------
    @Override public void setGlWindowVerticalSize(int newY) {
        super.setGlWindowVerticalSize(newY);
        if (bIsFirstFrame) {
            bIsFirstFrame = false;
            //			defaultThresholdValue();
        }
        updateThresholdHandle();
    }

    protected void updateThresholdHandle() {
        if (context != null) {
            Intent i = new Intent();
            i.setAction("BYBUpdateThresholdHandle");
            i.putExtra("pos", getThresholdScreenValue());
            i.putExtra("name", "OsciloscopeHandle");
            context.sendBroadcast(i);
        }
    }

    // ----------------------------------------------------------------------------------------
    protected boolean getCurrentAverage() {
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
    public int getThresholdScreenValue() {
        return glHeightToPixelHeight(threshold); // thresholdPixelHeight;
    }

    // ---------------------------------------------------------------------------------------------
    //	@Override
    //	protected void setGlWindow(GL10 gl, final int samplesToShow, final int lengthOfSampleSet) {
    //		final int size = getGlWindowVerticalSize();
    //		initGL(gl, (lengthOfSampleSet - samplesToShow) / 2, (lengthOfSampleSet + samplesToShow) / 2, -size / 2, size / 2);
    //	}
    // ---------------------------------------------------------------------------------------------
    public float getThresholdValue() {
        return threshold;
    }

    // ---------------------------------------------------------------------------------------------
    public void adjustThresholdValue(float dy) {
        Log.d(TAG, "adjustThresholdValue " + dy);
        if (dy == 0) {
            return;
        }
        threshold = dy;
        tempThreshold = dy;

        if (context != null) {
            if (getAudioService() != null) {
                getAudioService().getTriggerHandler().post(new Runnable() {
                    @Override public void run() {
                        ((TriggerHandler) getAudioService().getTriggerHandler()).setThreshold(threshold);
                    }
                });
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    public void adjustThreshold(float y) {
        adjustThresholdValue(pixelHeightToGlHeight(y));
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- BROADCAST RECEIVERS CLASS
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private class AdjustThresholdListener extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "AdjustThresholdListener onReceive: ");
            if (intent.hasExtra("name")) {
                if (intent.getStringExtra("name").equals("OsciloscopeHandle")) {
                    if (intent.hasExtra("y")) {
                        tempThreshold = pixelHeightToGlHeight(intent.getFloatExtra("y", getThresholdScreenValue()));
                    }
                    if (intent.hasExtra("action")) {
                        if (intent.getStringExtra("action").equals("up")) {
                            adjustThresholdValue(tempThreshold);
                        }
                    }
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- BROADCAST RECEIVERS TOGGLES
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private void registerAdjustThresholdReceiver(boolean reg) {
        if (reg && context != null) {
            IntentFilter intentFilter = new IntentFilter("BYBThresholdHandlePos");
            adjustThresholdListener = new AdjustThresholdListener();
            context.registerReceiver(adjustThresholdListener, intentFilter);
        } else {
            context.unregisterReceiver(adjustThresholdListener);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- SETTINGS
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override public void readSettings(SharedPreferences settings, String TAG) {
        if (settings != null) {
            super.readSettings(settings, TAG);
            adjustThresholdValue(settings.getFloat(TAG + "_threshold", threshold));
            //updateThresholdHandle();
            Log.w(TAG, "loadsetting threshold: " + threshold);
            bIsFirstFrame = settings.getBoolean(TAG + "_bIsFirstFrame", bIsFirstFrame);
        }
    }

    // ----------------------------------------------------------------------------------------
    @Override public void saveSettings(SharedPreferences settings, String TAG) {
        if (settings != null) {
            super.saveSettings(settings, TAG);
            final SharedPreferences.Editor editor = settings.edit();
            Log.w(TAG, "savesetting threshold: " + threshold);
            editor.putFloat(TAG + "_threshold", threshold);
            editor.putBoolean(TAG + "_bIsFirstFrame", bIsFirstFrame);
            editor.apply();
        }
    }
}
