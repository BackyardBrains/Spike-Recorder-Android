package com.backyardbrains.drawing;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.util.Log;
import android.util.SparseArray;
import com.backyardbrains.BaseFragment;
import com.backyardbrains.data.persistance.AnalysisDataSource;
import com.backyardbrains.data.persistance.entity.Spike;
import com.backyardbrains.utils.ThresholdOrientation;
import com.crashlytics.android.Crashlytics;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class FindSpikesRenderer extends SeekableWaveformRenderer {

    static final String TAG = makeLogTag(FindSpikesRenderer.class);

    private static final float[] WAVEFORM_COLOR = new float[] { .4f, .4f, .4f, .0f };

    private GlSpikes glSpikes;
    private float[] spikesVertices;
    private float[] spikesColors;

    @SuppressWarnings("WeakerAccess") Spike[] spikes;
    private int[] thresholds = new int[2];

    private float[] currentColor = BYBColors.getColorAsGlById(BYBColors.red);
    private float[] whiteColor = BYBColors.getColorAsGlById(BYBColors.white);

    private String filePath;

    private OnThresholdUpdateListener listener;

    /**
     * Interface definition for a callback to be invoked when threshold is updated.
     */
    public interface OnThresholdUpdateListener {
        /**
         * Listener that is invoked when threshold is updated.
         *
         * @param threshold Which threshold is updated. One of {@link ThresholdOrientation} values.
         * @param value New threshold value.
         */
        void onThresholdUpdate(@ThresholdOrientation int threshold, int value);
    }

    public FindSpikesRenderer(@NonNull BaseFragment fragment, @NonNull float[] preparedBuffer,
        @NonNull String filePath) {
        super(filePath, fragment, preparedBuffer);

        setScrollEnabled();

        this.filePath = filePath;

        glSpikes = new GlSpikes();

        updateThresholdHandles();
    }

    //=================================================
    //  PUBLIC AND PROTECTED METHODS
    //=================================================

    /**
     * Registers a callback to be invoked when threshold is updated.
     *
     * @param listener The callback that will be run. This value may be {@code null}.
     */
    public void setOnThresholdUpdateListener(@Nullable OnThresholdUpdateListener listener) {
        this.listener = listener;
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

    /**
     * {@inheritDoc}
     */
    @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
        super.onSurfaceChanged(gl, width, height);

        updateThresholdHandles();
    }

    /**
     * {@inheritDoc}
     */
    @Override public void setGlWindowHeight(int newSize) {
        super.setGlWindowHeight(Math.abs(newSize));

        updateThresholdHandles();
    }

    /**
     * {@inheritDoc}
     */
    @Override protected boolean drawSpikes() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override protected void draw(GL10 gl, @NonNull short[] samples, @NonNull float[] waveformVertices,
        @NonNull SparseArray<String> markers, int surfaceWidth, int surfaceHeight, int glWindowWidth,
        int glWindowHeight, int drawStartIndex, int drawEndIndex, float scaleX, float scaleY) {
        super.draw(gl, samples, waveformVertices, markers, surfaceWidth, surfaceHeight, glWindowWidth, glWindowHeight,
            drawStartIndex, drawEndIndex, scaleX, scaleY);
        if (getSpikes()) {
            //long start = System.currentTimeMillis();

            // let's save start and end sample positions that are being drawn before triggering the actual draw
            long toSample = getAudioService() != null ? getAudioService().getPlaybackProgress() : 0;
            long fromSample = Math.max(0, toSample - glWindowWidth);

            LOGD(TAG, "FROM: " + fromSample + ", TO: " + toSample);
            LOGD(TAG, "START: " + drawStartIndex + ", END: " + drawEndIndex);

            constructSpikesAndColorsBuffers(glWindowWidth, fromSample, toSample);
            glSpikes.draw(gl, spikesVertices, spikesColors, spikesVertices.length);

            //LOGD(TAG, (System.currentTimeMillis() - start) + " AFTER DRAWING SPIKES");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override @Size(4) protected float[] getWaveformColor() {
        return WAVEFORM_COLOR;
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
            if (listener != null) listener.onThresholdUpdate(threshold, glHeightToPixelHeight(thresholds[threshold]));
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

    private void constructSpikesAndColorsBuffers(int glWindowWidth, long fromSample, long toSample) {
        spikesVertices = null;
        spikesColors = null;

        float[] arr;
        float[] arr1;
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
                            index = toSample - fromSample < glWindowWidth ? spike.getIndex() + glWindowWidth - toSample
                                : spike.getIndex() - fromSample;
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

                    spikesVertices = new float[j];
                    System.arraycopy(arr, 0, spikesVertices, 0, spikesVertices.length);

                    spikesColors = new float[k];
                    System.arraycopy(arr1, 0, spikesColors, 0, spikesColors.length);
                } catch (ArrayIndexOutOfBoundsException e) {
                    Log.e(TAG, e.getMessage());
                    Crashlytics.logException(e);
                }
            }
        }
        if (spikesVertices == null) spikesVertices = new float[0];
        if (spikesColors == null) spikesColors = new float[0];
    }
}
