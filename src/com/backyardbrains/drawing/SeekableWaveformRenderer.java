package com.backyardbrains.drawing;

import android.support.annotation.NonNull;
import android.util.SparseArray;
import com.backyardbrains.BaseFragment;
import com.backyardbrains.utils.AnalysisUtils;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class SeekableWaveformRenderer extends WaveformRenderer {

    private static final String TAG = makeLogTag(SeekableWaveformRenderer.class);

    private static final float RMS_QUANTIFIER = 0.005f;

    private GlMeasurementArea glMeasurementArea;

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

    public SeekableWaveformRenderer(@NonNull BaseFragment fragment, @NonNull float[] preparedBuffer) {
        super(fragment, preparedBuffer);

        glMeasurementArea = new GlMeasurementArea();
    }

    //==============================================
    //  PUBLIC AND PROTECTED METHODS
    //==============================================

    public void setCallback(Callback callback) {
        super.setCallback(callback);

        this.callback = callback;
    }

    @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
        final int w = getSurfaceWidth();

        super.onSurfaceChanged(gl, width, height);

        // recalculate measurement start and end x coordinate
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
            LOGD(TAG, "START: " + measureStartIndex + ", END: " + measureEndIndex);
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
}