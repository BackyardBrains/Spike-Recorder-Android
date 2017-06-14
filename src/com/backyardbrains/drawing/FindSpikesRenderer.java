package com.backyardbrains.drawing;

import android.support.annotation.NonNull;
import android.util.Log;
import com.backyardbrains.BaseFragment;
import com.backyardbrains.analysis.BYBSpike;
import com.backyardbrains.utils.BYBUtils;
import java.nio.FloatBuffer;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class FindSpikesRenderer extends SeekableWaveformRenderer {

    private static final String TAG = makeLogTag(FindSpikesRenderer.class);

    private FloatBuffer spikesBuffer;
    private FloatBuffer colorsBuffer;

    private BYBSpike[] spikes;
    private int[] thresholds = new int[2];

    private float[] currentColor = BYBColors.getColorAsGlById(BYBColors.red);
    private float[] whiteColor = BYBColors.getColorAsGlById(BYBColors.white);

    public interface Callback extends BYBBaseRenderer.Callback {
        void onThresholdUpdate(@ThresholdOrientation int threshold, int value);
    }

    public FindSpikesRenderer(@NonNull BaseFragment fragment, @NonNull float[] preparedBuffer) {
        super(fragment, preparedBuffer);

        updateThresholdHandles();
    }

    // ----------------------------------------------------------------------------------------
    @Override public void setGlWindowVerticalSize(int newY) {
        super.setGlWindowVerticalSize(Math.abs(newY));

        updateThresholdHandles();
    }

    @Override public void setCallback(BYBBaseRenderer.Callback callback) {
        if (!(callback instanceof Callback)) {
            throw new RuntimeException("Callback needs to be of type FindSpikesRenderer.Callback!");
        }

        super.setCallback(callback);
    }

    private Callback getCallback() {
        return (Callback) callback;
    }

    // ----------------------------------------------------------------------------------------
    private void updateThresholdHandles() {
        updateThresholdHandle(ThresholdOrientation.LEFT);
        updateThresholdHandle(ThresholdOrientation.RIGHT);
    }

    // ----------------------------------------------------------------------------------------
    public int getThresholdScreenValue(@ThresholdOrientation int threshold) {
        if (threshold >= 0 && threshold < 2) return glHeightToPixelHeight(thresholds[threshold]);

        return 0;
    }

    // ----------------------------------------------------------------------------------------
    private void updateThresholdHandle(@ThresholdOrientation int threshold) {
        if (threshold >= 0 && threshold < thresholds.length) {
            if (getCallback() != null) {
                getCallback().onThresholdUpdate(threshold, glHeightToPixelHeight(thresholds[threshold]));
            }
        }
    }

    // ----------------------------------------------------------------------------------------
    public void setThreshold(int t, @ThresholdOrientation int orientation) {
        setThreshold(t, orientation, false);
    }

    // ----------------------------------------------------------------------------------------
    private void setThreshold(int t, @ThresholdOrientation int orientation, boolean bBroadcast) {
        if (orientation == ThresholdOrientation.LEFT || orientation == ThresholdOrientation.RIGHT) {
            thresholds[orientation] = t;
            if (bBroadcast) updateThresholdHandle(orientation);
        }
    }

    // ----------------------------------------------------------------------------------------

    @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
        super.onSurfaceChanged(gl, width, height);
        updateThresholdHandles();
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
    @Override protected void drawingHandler(GL10 gl) {
        if (getSpikes()) {
            //long start = System.currentTimeMillis();

            setGlWindow(gl, getGlWindowHorizontalSize(), mBufferToDraws.length);

            final FloatBuffer linesBuffer = getWaveformBuffer(mBufferToDraws);
            constructSpikesAndColorsBuffers();

            if (linesBuffer != null && spikesBuffer != null && colorsBuffer != null) {
                gl.glMatrixMode(GL10.GL_MODELVIEW);
                gl.glLoadIdentity();

                gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
                gl.glLineWidth(1f);
                gl.glColor4f(0f, 1f, 0f, 1f);
                gl.glVertexPointer(2, GL10.GL_FLOAT, 0, linesBuffer);
                gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, linesBuffer.limit() / 2);
                gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
                //LOGD(TAG, (System.currentTimeMillis() - start) + " AFTER DRAWING WAVE");

                gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
                gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
                gl.glPointSize(10.0f);
                gl.glVertexPointer(2, GL10.GL_FLOAT, 0, spikesBuffer);
                gl.glColorPointer(4, GL10.GL_FLOAT, 0, colorsBuffer);
                gl.glDrawArrays(GL10.GL_POINTS, 0, spikesBuffer.limit() / 2);
                gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
                gl.glDisableClientState(GL10.GL_COLOR_ARRAY);

                //LOGD(TAG, (System.currentTimeMillis() - start) + " AFTER DRAWING SPIKES");
            }
        } else {
            super.drawingHandler(gl);
        }
    }

    // -----------------------------------------------------------------------------------------------------------------------------
    public void setCurrentColor(float[] color) {
        if (currentColor.length == color.length && currentColor.length == 4) {
            System.arraycopy(color, 0, currentColor, 0, currentColor.length);
        }
    }

    // -----------------------------------------------------------------------------------------------------------------------------
    private void constructSpikesAndColorsBuffers() {
        float[] arr;
        float[] spikeArr = null;
        float[] arr1;
        float[] colorsArr = null;
        if (spikes != null) {
            if (spikes.length > 0) {
                arr = new float[spikes.length * 2];
                arr1 = new float[spikes.length * 4];
                int j = 0; // index of arr
                int k = 0; // index of arr1
                int min = Math.min(thresholds[ThresholdOrientation.LEFT], thresholds[ThresholdOrientation.RIGHT]);
                int max = Math.max(thresholds[ThresholdOrientation.LEFT], thresholds[ThresholdOrientation.RIGHT]);
                try {
                    long index = 0;
                    for (BYBSpike spike : spikes) {
                        if (fromSample < spike.index && spike.index < toSample) {
                            index = toSample - fromSample < getGlWindowHorizontalSize() ?
                                spike.index + getGlWindowHorizontalSize() - toSample : spike.index - fromSample;
                            arr[j++] = index;
                            arr[j++] = spike.value;

                            float v = spike.value;
                            float[] colorToSet = whiteColor;
                            if (v >= min && v < max) colorToSet = currentColor;
                            for (int l = 0; l < 4; l++) {
                                arr1[k++] = colorToSet[l];
                            }
                        }
                    }

                    spikeArr = new float[j];
                    System.arraycopy(arr, 0, spikeArr, 0, spikeArr.length);

                    colorsArr = new float[k];
                    System.arraycopy(arr1, 0, colorsArr, 0, colorsArr.length);
                } catch (ArrayIndexOutOfBoundsException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }
        if (spikeArr == null) spikeArr = new float[0];
        if (colorsArr == null) colorsArr = new float[0];
        spikesBuffer = BYBUtils.getFloatBufferFromFloatArray(spikeArr, spikeArr.length);
        colorsBuffer = BYBUtils.getFloatBufferFromFloatArray(colorsArr, colorsArr.length);
    }
}
