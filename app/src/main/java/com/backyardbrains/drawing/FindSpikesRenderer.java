package com.backyardbrains.drawing;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.util.SparseArray;
import com.backyardbrains.BaseFragment;
import com.backyardbrains.data.SpikeValueAndIndex;
import com.backyardbrains.utils.ThresholdOrientation;
import com.crashlytics.android.Crashlytics;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.LOGE;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class FindSpikesRenderer extends SeekableWaveformRenderer {

    static final String TAG = makeLogTag(FindSpikesRenderer.class);

    private static final float[] WAVEFORM_COLOR = new float[] { .4f, .4f, .4f, .0f };

    private GlSpikes glSpikes;
    private final float[] spikesVertices = new float[GlSpikes.MAX_POINT_VERTICES];
    private final float[] spikesColors = new float[GlSpikes.MAX_COLOR_VERTICES];

    private int[] thresholds = new int[2];

    private float[] currentColor = BYBColors.getColorAsGlById(BYBColors.red);
    private float[] whiteColor = BYBColors.getColorAsGlById(BYBColors.white);

    private String filePath;

    private long spikeAnalysisId = -1;

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

    public FindSpikesRenderer(@NonNull BaseFragment fragment, @NonNull String filePath) {
        super(filePath, fragment);

        setMeasureEnabled(false);

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
    @Override protected void draw(GL10 gl, @NonNull short[] samples, @NonNull short[] waveformVertices,
        @NonNull SparseArray<String> markers, int surfaceWidth, int surfaceHeight, int glWindowWidth,
        int glWindowHeight, int drawStartIndex, int drawEndIndex, float scaleX, float scaleY, long lastSampleIndex) {

        super.draw(gl, samples, waveformVertices, markers, surfaceWidth, surfaceHeight, glWindowWidth, glWindowHeight,
            drawStartIndex, drawEndIndex, scaleX, scaleY, lastSampleIndex);

        if (getAnalysisManager() != null) {
            // retry getting spike analysis id until we have it
            if (spikeAnalysisId <= 0) {
                spikeAnalysisId = getAnalysisManager().getSpikeAnalysisId(filePath);
                if (spikeAnalysisId <= 0) return;
            }

            // let's save start and end sample positions that are being drawn before triggering the actual draw
            int toSample = (int) lastSampleIndex;
            int fromSample = Math.max(0, toSample - glWindowWidth);
            if (spikeAnalysisId > 0) {
                final SpikeValueAndIndex[] valuesAndIndices =
                    getAnalysisManager().getSpikesForRange(spikeAnalysisId, fromSample, toSample);
                int verticesCount =
                    fillSpikesAndColorsBuffers(valuesAndIndices, spikesVertices, spikesColors, glWindowWidth,
                        fromSample, toSample, (long) (waveformVertices.length * .5));
                glSpikes.draw(gl, spikesVertices, spikesColors, verticesCount);
            }
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

    // Fills spike and color buffers preparing them for drawing. Number of vertices is returned.
    private int fillSpikesAndColorsBuffers(@NonNull SpikeValueAndIndex[] valueAndIndices,
        @NonNull float[] spikesVertices, @NonNull float[] spikesColors, int glWindowWidth, long fromSample,
        long toSample, long returnCount) {
        int verticesCounter = 0;
        try {
            if (valueAndIndices.length > 0) {
                int colorsCounter = 0;
                float scaleX = (float) returnCount / glWindowWidth;
                long index;

                final int min = Math.min(thresholds[ThresholdOrientation.LEFT], thresholds[ThresholdOrientation.RIGHT]);
                final int max = Math.max(thresholds[ThresholdOrientation.LEFT], thresholds[ThresholdOrientation.RIGHT]);

                for (SpikeValueAndIndex valueAndIndex : valueAndIndices) {
                    if (fromSample <= valueAndIndex.getIndex() && valueAndIndex.getIndex() < toSample) {
                        if (toSample - fromSample < glWindowWidth) { // buffer contains 0 samples in front
                            index = valueAndIndex.getIndex() + glWindowWidth - toSample;
                        } else { // buffer only contains sample data (no 0 samples in front)
                            index = valueAndIndex.getIndex() - fromSample;
                        }
                        index = (long) (index * scaleX);
                        spikesVertices[verticesCounter++] = index;
                        float spikeValue = valueAndIndex.getValue();
                        spikesVertices[verticesCounter++] = spikeValue;
                        float[] colorToSet;
                        if (spikeValue >= min && spikeValue < max) {
                            colorToSet = currentColor;
                        } else {
                            colorToSet = whiteColor;
                        }
                        System.arraycopy(colorToSet, 0, spikesColors, colorsCounter, colorToSet.length);
                        colorsCounter += 4;
                    }
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            LOGE(TAG, e.getMessage());
            Crashlytics.logException(e);
        }

        return verticesCounter;
    }
}
