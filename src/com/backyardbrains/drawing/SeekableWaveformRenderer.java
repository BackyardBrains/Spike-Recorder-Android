package com.backyardbrains.drawing;

import android.support.annotation.NonNull;
import android.support.annotation.Size;
import android.util.SparseArray;
import com.backyardbrains.BaseFragment;
import com.backyardbrains.data.persistance.entity.Spike;
import com.backyardbrains.utils.AnalysisUtils;
import com.crashlytics.android.Crashlytics;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.LOGE;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class SeekableWaveformRenderer extends WaveformRenderer {

    private static final String TAG = makeLogTag(SeekableWaveformRenderer.class);

    private static final float RMS_QUANTIFIER = 0.005f;

    private final String filePath;

    private GlMeasurementArea glMeasurementArea;
    private GlSpikes glSpikes;

    private float[] spikesVertices;
    private float[] spikesColors;

    private float[] redColor = BYBColors.getColorAsGlById(BYBColors.red);
    private float[] yellowColor = BYBColors.getColorAsGlById(BYBColors.yellow);
    private float[] greenColor = BYBColors.getColorAsGlById(BYBColors.green);

    private short[] rmsSamples;
    private float drawSampleCount;

    private boolean measuring;
    private float measurementStartX;
    private float measurementEndX;

    private Callback callback;

    public interface Callback extends BYBBaseRenderer.Callback {

        void onMeasurementStart();

        void onMeasure(float rms, int rmsSampleCount);

        void onMeasurementEnd();
    }

    public SeekableWaveformRenderer(@NonNull String filePath, @NonNull BaseFragment fragment,
        @NonNull float[] preparedBuffer) {
        super(fragment, preparedBuffer);

        this.filePath = filePath;

        glMeasurementArea = new GlMeasurementArea();
        glSpikes = new GlSpikes();
    }

    //==============================================
    //  PUBLIC AND PROTECTED METHODS
    //==============================================

    public void setCallback(Callback callback) {
        super.setCallback(callback);

        this.callback = callback;
    }

    protected boolean drawSpikes() {
        return true;
    }

    @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
        final int w = getSurfaceWidth();

        super.onSurfaceChanged(gl, width, height);

        // recalculate measurement start and end x coordinates
        measurementStartX = width * measurementStartX / w;
        measurementEndX = width * measurementEndX / w;
    }

    @Override protected void draw(GL10 gl, @NonNull short[] samples, @NonNull float[] waveformVertices,
        @NonNull SparseArray<String> markers, int surfaceWidth, int surfaceHeight, int glWindowWidth,
        int glWindowHeight, int drawStartIndex, int drawEndIndex, float scaleX, float scaleY) {

        // draw measurement area
        if (measuring) {
            // calculate necessary measurement parameters
            final int drawSampleCount = drawEndIndex - drawStartIndex;
            final float coefficient = drawSampleCount * 1f / surfaceWidth;
            final int measureStartIndex = Math.round(measurementStartX * coefficient);
            final int measureEndIndex = Math.round(measurementEndX * coefficient);
            final int measureSampleCount = Math.abs(measureEndIndex - measureStartIndex);
            // fill array of samples used for RMS calculation
            if (rmsSamples == null || drawSampleCount != this.drawSampleCount) {
                this.drawSampleCount = drawSampleCount;
                rmsSamples = new short[drawSampleCount];
            }
            final int startIndex = Math.min(measureStartIndex, measureEndIndex);
            final int measureFirstSampleIndex = drawStartIndex + startIndex;
            System.arraycopy(samples, measureFirstSampleIndex, rmsSamples, 0, measureSampleCount);
            // calculate RMS
            final float rms = AnalysisUtils.RMS(rmsSamples, measureSampleCount) * RMS_QUANTIFIER;

            if (callback != null) callback.onMeasure(Float.isNaN(rms) ? 0f : rms, measureSampleCount);

            // draw measurement area
            glMeasurementArea.draw(gl, measurementStartX * scaleX, measurementEndX * scaleX, -glWindowHeight * .5f,
                glWindowHeight * .5f);
        }

        super.draw(gl, samples, waveformVertices, markers, surfaceWidth, surfaceHeight, glWindowWidth, glWindowHeight,
            drawStartIndex, drawEndIndex, scaleX, scaleY);

        if (drawSpikes()) {
            // let's save start and end sample positions that are being drawn before triggering the actual draw
            int toSample = getAudioService() != null ? (int) getAudioService().getPlaybackProgress() : 0;
            int fromSample = Math.max(0, toSample - glWindowWidth);
            // draw spikes
            Spike[][] spikes;
            if (getAnalysisManager() != null) {
                spikes = getAnalysisManager().getSpikesByTrainsForRange(filePath, fromSample, toSample);
            } else {
                spikes = new Spike[0][];
            }
            if (spikes.length > 0) {
                for (int i = 0; i < spikes.length; i++) {
                    constructSpikesAndColorsBuffers(spikes[i], glWindowWidth, fromSample, toSample,
                        i == 0 ? redColor : i == 1 ? yellowColor : greenColor);
                    glSpikes.draw(gl, spikesVertices, spikesColors);
                }
            }
        }
    }

    @Override protected void onMeasurementStart(float x) {
        measurementStartX = x;
        measurementEndX = x;
        measuring = true;

        if (callback != null) callback.onMeasurementStart();
    }

    @Override protected void onMeasure(float x) {
        measurementEndX = x;
    }

    @Override protected void onMeasurementEnd(float x) {
        measuring = false;
        measurementStartX = 0;
        measurementEndX = 0;

        if (callback != null) callback.onMeasurementEnd();
    }

    private void constructSpikesAndColorsBuffers(@NonNull Spike[] spikes, int glWindowWidth, long fromSample,
        long toSample, @Size(4) float[] color) {
        spikesVertices = null;
        spikesColors = null;

        try {
            if (spikes.length > 0) {
                float[] arr = new float[spikes.length * 2];
                float[] arr1 = new float[spikes.length * 4];
                int i = 0, j = 0; // j as index of arr, k as index of arr1
                long index;

                for (Spike spike : spikes) {
                    if (fromSample <= spike.getIndex() && spike.getIndex() <= toSample) {
                        index = toSample - fromSample < glWindowWidth ? spike.getIndex() + glWindowWidth - toSample
                            : spike.getIndex() - fromSample;
                        arr[i++] = index;
                        arr[i++] = spike.getValue();

                        for (int k = 0; k < 4; k++) {
                            arr1[j++] = color[k];
                        }
                    }
                }

                spikesVertices = new float[i];
                System.arraycopy(arr, 0, spikesVertices, 0, spikesVertices.length);

                spikesColors = new float[j];
                System.arraycopy(arr1, 0, spikesColors, 0, spikesColors.length);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            LOGE(TAG, e.getMessage());
            Crashlytics.logException(e);
        }

        if (spikesVertices == null) spikesVertices = new float[0];
        if (spikesColors == null) spikesColors = new float[0];
    }
}