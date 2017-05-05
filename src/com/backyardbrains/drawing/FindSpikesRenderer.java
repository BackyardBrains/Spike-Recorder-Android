package com.backyardbrains.drawing;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;
import com.backyardbrains.utls.BYBGlUtils;
import com.backyardbrains.utls.BYBUtils;
import com.backyardbrains.BaseFragment;
import com.backyardbrains.analysis.BYBSpike;
import com.backyardbrains.utls.LogUtils;
import java.nio.FloatBuffer;
import javax.microedition.khronos.opengles.GL10;

public class FindSpikesRenderer extends BYBBaseRenderer {

    private static final String TAG = LogUtils.makeLogTag(FindSpikesRenderer.class);

    public static final int LEFT_THRESH_INDEX = 0;
    public static final int RIGHT_THRESH_INDEX = 1;

    private static final String[] THRESHOLDS_NAMES = { "LeftSpikesHandle", "RightSpikesHandle" };

    private float playHeadPosition = 0.5f;
    private BYBSpike[] spikes;
    private int[] thresholds = new int[2];

    private float[] currentColor = BYBColors.getColorAsGlById(BYBColors.red);
    private float[] whiteColor = BYBColors.getColorAsGlById(BYBColors.white);

    public FindSpikesRenderer(@NonNull BaseFragment fragment, @NonNull float[] preparedBuffer) {
        super(fragment, preparedBuffer);

        updateThresholdHandles();
    }

    @Override
    // ----------------------------------------------------------------------------------------
    public void setGlWindowVerticalSize(int newY) {
        super.setGlWindowVerticalSize(Math.abs(newY));
        updateThresholdHandles();
    }

    // ----------------------------------------------------------------------------------------
    private void updateThresholdHandles() {
        updateThresholdHandle(LEFT_THRESH_INDEX);
        updateThresholdHandle(RIGHT_THRESH_INDEX);
    }

    // ----------------------------------------------------------------------------------------
    public int getThresholdScreenValue(int index) {
        if (index >= 0 && index < 2) return glHeightToPixelHeight(thresholds[index]);

        return 0;
    }

    // ----------------------------------------------------------------------------------------
    private void updateThresholdHandle(int threshIndex) {
        if (threshIndex >= 0 && threshIndex < thresholds.length && getContext() != null) {
            Intent j = new Intent();
            j.setAction("BYBUpdateThresholdHandle");
            j.putExtra("name", THRESHOLDS_NAMES[threshIndex]);
            j.putExtra("pos", glHeightToPixelHeight(thresholds[threshIndex]));
            getContext().sendBroadcast(j);
        }
    }

    // ----------------------------------------------------------------------------------------
    public void setThreshold(int t, int index) {
        setThreshold(t, index, false);
    }

    // ----------------------------------------------------------------------------------------
    private void setThreshold(int t, int index, boolean bBroadcast) {
        if (index >= 0 && index < 2) {
            thresholds[index] = t;
            if (bBroadcast) updateThresholdHandle(index);
        }
    }

    // ----------------------------------------------------------------------------------------
    @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
        super.onSurfaceChanged(gl, width, height);
        updateThresholdHandles();
    }

    // ----------------------------------------------------------------------------------------
    @Override public void onDrawFrame(GL10 gl) {
        if (!getAudioSamples()) return;
        if (!BYBUtils.isValidAudioBuffer(mBufferToDraws)) return;

        preDrawingHandler();
        BYBGlUtils.glClear(gl);
        drawingHandler(gl);
        postDrawingHandler(gl);
    }

    // ----------------------------------------------------------------------------------------
    private boolean getSpikes() {
        if (getAnalysisManager() != null) {
            spikes = getAnalysisManager().getSpikes();
            if (spikes.length > 0) return true;
        }
        spikes = null;

        return false;
    }

    // ----------------------------------------------------------------------------------------
    private boolean getAudioSamples() {
        if (getAnalysisManager() != null) {
            mBufferToDraws = getAnalysisManager().getReaderSamples();
            return true;
        }
        return false;
    }

    // ----------------------------------------------------------------------------------------
    public void setStartSample(float pos) {// normalized position
        playHeadPosition = pos;
    }

    // ----------------------------------------------------------------------------------------
    @Override protected void drawingHandler(GL10 gl) {
        setGlWindow(gl, getGlWindowHorizontalSize(), mBufferToDraws.length);

        final FloatBuffer mVertexBuffer = getWaveformBuffer(mBufferToDraws);
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glLineWidth(1f);
        gl.glColor4f(0f, 1f, 0f, 1f);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, mVertexBuffer);
        gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, mVertexBuffer.limit() / 2);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);

        if (!getSpikes()) return;

        final FloatBuffer spikesBuffer = getPointsFromSpikes();
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
        gl.glPointSize(10.0f);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, spikesBuffer);
        gl.glColorPointer(4, GL10.GL_FLOAT, 0, getColorsFloatBuffer());
        gl.glDrawArrays(GL10.GL_POINTS, 0, spikesBuffer.limit() / 2);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
    }

    // -----------------------------------------------------------------------------------------------------------------------------
    public void setCurrentColor(float[] color) {
        if (currentColor.length == color.length && currentColor.length == 4) {
            System.arraycopy(color, 0, currentColor, 0, currentColor.length);
        }
    }

    // -----------------------------------------------------------------------------------------------------------------------------
    private FloatBuffer getColorsFloatBuffer() {
        float[] arr = null;
        if (spikes != null) {
            if (spikes.length > 0) {
                arr = new float[spikes.length * 4];
                int j = 0; // index of arr
                int mn = Math.min(thresholds[0], thresholds[1]);
                int mx = Math.max(thresholds[0], thresholds[1]);
                try {
                    for (BYBSpike spike : spikes) {
                        float v = spike.value;
                        float[] colorToSet = whiteColor;
                        if (v >= mn && v < mx) colorToSet = currentColor;
                        for (int k = 0; k < 4; k++) {
                            arr[j++] = colorToSet[k];
                        }
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }
        if (arr == null) arr = new float[0];
        return BYBUtils.getFloatBufferFromFloatArray(arr, arr.length);
    }

    // -----------------------------------------------------------------------------------------------------------------------------
    private FloatBuffer getPointsFromSpikes() {
        float[] arr = null;
        if (spikes != null) {
            if (spikes.length > 0) {
                arr = new float[spikes.length * 2];
                int j = 0; // index of arr
                try {
                    for (BYBSpike spike : spikes) {
                        arr[j++] = spike.index;
                        arr[j++] = spike.value;
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }
        if (arr == null) arr = new float[0];
        return BYBUtils.getFloatBufferFromFloatArray(arr, arr.length);
    }

    // -----------------------------------------------------------------------------------------------------------------------------
    @Override protected FloatBuffer getWaveformBuffer(short[] shortArrayToDraw) {
        float[] arr = new float[shortArrayToDraw.length * 2]; // array to fill
        int j = 0; // index of arr
        try {
            for (int i = 0; i < shortArrayToDraw.length; i++) {
                arr[j++] = i;
                arr[j++] = shortArrayToDraw[i];
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, e.getMessage());
        }
        return BYBUtils.getFloatBufferFromFloatArray(arr, arr.length);
    }

    // -----------------------------------------------------------------------------------------------------------------------------
    @Override protected void setGlWindow(GL10 gl, final int samplesToShow, final int lengthOfSampleSet) {
        final int size = getGlWindowVerticalSize();
        int startSample = (int) Math.floor((lengthOfSampleSet - samplesToShow) * playHeadPosition);
        int endSample = startSample + samplesToShow;
        initGL(gl, startSample, endSample, -size / 2, size / 2);
    }
}
