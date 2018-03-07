package com.backyardbrains.drawing;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import com.backyardbrains.BaseFragment;
import com.backyardbrains.data.persistance.AnalysisDataSource;
import com.backyardbrains.data.persistance.entity.Spike;
import com.backyardbrains.utils.BYBUtils;
import com.backyardbrains.utils.ThresholdOrientation;
import com.crashlytics.android.Crashlytics;
import java.nio.FloatBuffer;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class FindSpikesRenderer extends SeekableWaveformRenderer {

    static final String TAG = makeLogTag(FindSpikesRenderer.class);

    private long fromSample;
    private long toSample;

    private FloatBuffer spikesBuffer;
    private FloatBuffer colorsBuffer;

    @SuppressWarnings("WeakerAccess") Spike[] spikes;
    private int[] thresholds = new int[2];

    private float[] currentColor = BYBColors.getColorAsGlById(BYBColors.red);
    private float[] whiteColor = BYBColors.getColorAsGlById(BYBColors.white);

    private Callback callback;
    private String filePath;

    interface Callback extends BYBBaseRenderer.Callback {
        void onThresholdUpdate(@ThresholdOrientation int threshold, int value);
    }

    public static class CallbackAdapter extends BYBBaseRenderer.CallbackAdapter implements Callback {
        @Override public void onThresholdUpdate(@ThresholdOrientation int threshold, int value) {
        }
    }

    public FindSpikesRenderer(@NonNull BaseFragment fragment, @NonNull float[] preparedBuffer,
        @NonNull String filePath) {
        super(fragment, preparedBuffer);

        this.filePath = filePath;

        updateThresholdHandles();
    }

    //=================================================
    //  PUBLIC AND PROTECTED METHODS
    //=================================================

    public void setCallback(@Nullable Callback callback) {
        super.setCallback(callback);

        this.callback = callback;
    }

    public int getThresholdScreenValue(@ThresholdOrientation int threshold) {
        if (threshold >= 0 && threshold < 2) return glHeightToPixelHeight(thresholds[threshold]);

        return 0;
    }

    public void setThreshold(int t, @ThresholdOrientation int orientation) {
        setThreshold(t, orientation, false);
    }

    public void setCurrentColor(float[] color) {
        if (currentColor.length == color.length && currentColor.length == 4) {
            System.arraycopy(color, 0, currentColor, 0, currentColor.length);
        }
    }

    @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
        super.onSurfaceChanged(gl, width, height);

        updateThresholdHandles();
    }

    @Override public void onDrawFrame(GL10 gl) {
        // let's save start and end sample positions that are being drawn before triggering the actual draw
        //toSample = getAudioService() != null ? getAudioService().getPlaybackProgress() : 0;
        //fromSample = Math.max(0, toSample - getGlWindowHorizontalSize());
        //LOGD(TAG, "from: " + fromSample + ", to: " + toSample + ", horizontal: " + getGlWindowHorizontalSize());

        super.onDrawFrame(gl);
    }

    @Override public void setGlWindowVerticalSize(int newSize) {
        super.setGlWindowVerticalSize(Math.abs(newSize));

        updateThresholdHandles();
    }

    @Override protected void drawingHandler(GL10 gl) {
        if (getSpikes()) {
            //long start = System.currentTimeMillis();

            int glWindowHorizontalSize = getGlWindowHorizontalSize();

            // let's save start and end sample positions that are being drawn before triggering the actual draw
            toSample = getAudioService() != null ? getAudioService().getPlaybackProgress() : 0;
            fromSample = Math.max(0, toSample - glWindowHorizontalSize);

            constructSpikesAndColorsBuffers(glWindowHorizontalSize);
            final FloatBuffer linesBuffer = getWaveformBuffer(drawingBuffer, glWindowHorizontalSize);

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

    //=================================================
    //  PRIVATE METHODS
    //=================================================

    private void updateThresholdHandles() {
        updateThresholdHandle(ThresholdOrientation.LEFT);
        updateThresholdHandle(ThresholdOrientation.RIGHT);
    }

    private void updateThresholdHandle(@ThresholdOrientation int threshold) {
        if (threshold >= 0 && threshold < thresholds.length) {
            if (callback != null) callback.onThresholdUpdate(threshold, glHeightToPixelHeight(thresholds[threshold]));
        }
    }

    private void setThreshold(int t, @ThresholdOrientation int orientation, boolean broadcast) {
        if (orientation == ThresholdOrientation.LEFT || orientation == ThresholdOrientation.RIGHT) {
            thresholds[orientation] = t;
            if (broadcast) updateThresholdHandle(orientation);
        }
    }

    private boolean getSpikes() {
        if (spikes != null && spikes.length > 0) return true;

        if (getAnalysisManager() != null) {
            getAnalysisManager().getSpikes(filePath, new AnalysisDataSource.GetAnalysisCallback<Spike[]>() {
                @Override public void onAnalysisLoaded(@NonNull Spike[] result) {
                    LOGD(TAG, "SPIKES RETURNED: " + result.length);
                    FindSpikesRenderer.this.spikes = result;
                }

                @Override public void onDataNotAvailable() {
                    spikes = null;
                }
            });
        }

        return false;
    }

    private void constructSpikesAndColorsBuffers(int glWindowHorizontalSize) {
        float[] arr;
        float[] spikeArr = null;
        float[] arr1;
        float[] colorsArr = null;
        if (spikes != null) {
            if (spikes.length > 0) {
                final int min = Math.min(thresholds[ThresholdOrientation.LEFT], thresholds[ThresholdOrientation.RIGHT]);
                final int max = Math.max(thresholds[ThresholdOrientation.LEFT], thresholds[ThresholdOrientation.RIGHT]);

                arr = new float[spikes.length * 2];
                arr1 = new float[spikes.length * 4];
                int j = 0, k = 0; // j as index of arr, k as index of arr1
                try {
                    long index;
                    for (Spike spike : spikes) {
                        if (fromSample < spike.getIndex() && spike.getIndex() < toSample) {
                            index = toSample - fromSample < glWindowHorizontalSize ?
                                spike.getIndex() + glWindowHorizontalSize - toSample : spike.getIndex() - fromSample;
                            arr[j++] = index;
                            arr[j++] = spike.getValue();

                            float v = spike.getValue();
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
                    Crashlytics.logException(e);
                }
            }
        }
        if (spikeArr == null) spikeArr = new float[0];
        if (colorsArr == null) colorsArr = new float[0];
        spikesBuffer = BYBUtils.getFloatBufferFromFloatArray(spikeArr, spikeArr.length);
        colorsBuffer = BYBUtils.getFloatBufferFromFloatArray(colorsArr, colorsArr.length);
    }
}
