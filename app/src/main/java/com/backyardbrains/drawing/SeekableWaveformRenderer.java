package com.backyardbrains.drawing;

import android.support.annotation.NonNull;
import android.support.annotation.Size;
import android.util.SparseArray;
import com.backyardbrains.BaseFragment;
import com.backyardbrains.data.SpikeIndexValue;
import com.backyardbrains.data.persistance.AnalysisDataSource;
import com.backyardbrains.data.persistance.entity.Train;
import com.backyardbrains.drawing.gl.GlMeasurementArea;
import com.backyardbrains.drawing.gl.GlSpikes;
import com.backyardbrains.utils.AnalysisUtils;
import com.backyardbrains.utils.Benchmark;
import com.backyardbrains.utils.GlUtils;
import com.crashlytics.android.Crashlytics;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.LOGE;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class SeekableWaveformRenderer extends WaveformRenderer {

    private static final String TAG = makeLogTag(SeekableWaveformRenderer.class);

    // Root mean square quantifier used when analyzing selected spikes
    private static final float RMS_QUANTIFIER = 0.005f;

    private final GlMeasurementArea glMeasurementArea;
    private final GlSpikes glSpikes;
    private final float[] spikesVertices = new float[GlSpikes.MAX_POINT_VERTICES];
    private final float[] spikesColors = new float[GlSpikes.MAX_COLOR_VERTICES];

    private short[] rmsSamples;
    private float measureSampleCount;

    private boolean measuring;
    private float measurementStartX;
    private float measurementEndX;

    @SuppressWarnings("WeakerAccess") Train[] spikeTrains;
    @SuppressWarnings("WeakerAccess") SpikeIndexValue[][] valuesAndIndexes;

    public SeekableWaveformRenderer(@NonNull String filePath, @NonNull BaseFragment fragment) {
        super(fragment);

        setScrollEnabled();
        setMeasureEnabled(true);

        glMeasurementArea = new GlMeasurementArea();
        glSpikes = new GlSpikes();

        if (getAnalysisManager() != null) {
            getAnalysisManager().getSpikeTrains(filePath, new AnalysisDataSource.GetAnalysisCallback<Train[]>() {
                @Override public void onAnalysisLoaded(@NonNull Train[] result) {
                    spikeTrains = result;
                    valuesAndIndexes = new SpikeIndexValue[spikeTrains.length][];
                }

                @Override public void onDataNotAvailable() {
                    spikeTrains = null;
                }
            });
        }
    }

    //==============================================
    //  PUBLIC AND PROTECTED METHODS
    //==============================================

    // FIXME: 11-Apr-18 THIS IS A HACK FOR NOW SO THAT SUBCLASSES CAN TELL THE PARENT NOT TO DRAW SPIKES IF NECESSARY
    protected boolean drawSpikes() {
        return !isSignalAveraging();
    }

    /**
     * {@inheritDoc}
     */
    @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
        final int w = getSurfaceWidth();

        super.onSurfaceChanged(gl, width, height);

        // recalculate measurement start and end x coordinates
        measurementStartX = width * measurementStartX / w;
        measurementEndX = width * measurementEndX / w;
    }

    private final Benchmark benchmark =
        new Benchmark("SPIKES_RETRIEVAL").warmUp(200).sessions(10).measuresPerSession(200).logBySession(false);

    /**
     * {@inheritDoc}
     */
    @Override protected void draw(GL10 gl, @NonNull short[] samples, @NonNull short[] waveformVertices,
        int waveformVerticesCount, @NonNull SparseArray<String> events, int surfaceWidth, int surfaceHeight,
        int glWindowWidth, int glWindowHeight, int drawStartIndex, int drawEndIndex, float scaleX, float scaleY,
        long lastSampleIndex) {
        // let's save start and end sample positions that are being drawn before triggering the actual draw
        int toSample = (int) lastSampleIndex;
        int fromSample = Math.max(0, toSample - glWindowWidth);
        if (drawSpikes()) {
            if (getAnalysisManager() != null && spikeTrains != null) {
                for (int i = 0; i < spikeTrains.length; i++) {
                    //benchmark.start();
                    valuesAndIndexes[i] =
                        getAnalysisManager().getSpikesByTrainForRange(spikeTrains[i].getId(), fromSample, toSample);
                    //benchmark.end();
                }
            }
        }

        long drawSampleCount = (long) (waveformVerticesCount * .5);

        // draw measurement area
        if (measuring) {
            // calculate start and end measurement area draw coordinates
            float drawScale = (float) drawSampleCount / surfaceWidth;
            final float measurementAreaDrawStart = measurementStartX * drawScale;
            final float measurementAreaDrawEnd = measurementEndX * drawScale;
            final int measureStartIndex =
                (int) (scaleX < 1 ? measurementAreaDrawStart : measurementAreaDrawStart * scaleX);
            final int measureEndIndex = (int) (scaleX < 1 ? measurementAreaDrawEnd : measurementAreaDrawEnd * scaleX);
            final int measureSampleCount = Math.abs(measureEndIndex - measureStartIndex);
            // fill array of samples used for RMS calculation
            if (rmsSamples == null || measureSampleCount != this.measureSampleCount) {
                this.measureSampleCount = measureSampleCount;
                rmsSamples = new short[measureSampleCount];
            }
            final int startIndex = Math.min(measureStartIndex, measureEndIndex);
            final int measureFirstSampleIndex = drawStartIndex + startIndex;
            System.arraycopy(samples, measureFirstSampleIndex, rmsSamples, 0, measureSampleCount);
            // calculate RMS
            final float rms = AnalysisUtils.RMS(rmsSamples, measureSampleCount) * RMS_QUANTIFIER;

            int[] spikeCounts = new int[] { -1, -1, -1 };
            if (spikeTrains != null && valuesAndIndexes.length > 0) {
                for (int i = 0; i < valuesAndIndexes.length; i++) {
                    spikeCounts[i] = 0;
                    for (int j = 0; j < valuesAndIndexes[i].length; j++) {
                        if (fromSample + startIndex <= valuesAndIndexes[i][j].getIndex()
                            && valuesAndIndexes[i][j].getIndex() <= fromSample + startIndex + measureSampleCount) {
                            spikeCounts[i]++;
                        }
                    }
                }
            }

            onMeasure(Float.isNaN(rms) ? 0f : rms, spikeCounts[0], spikeCounts[1], spikeCounts[2], measureSampleCount);

            //draw measurement area
            glMeasurementArea.draw(gl, measurementAreaDrawStart, measurementAreaDrawEnd, -glWindowHeight * .5f,
                glWindowHeight * .5f);
        }

        super.draw(gl, samples, waveformVertices, waveformVerticesCount, events, surfaceWidth, surfaceHeight,
            glWindowWidth, glWindowHeight, drawStartIndex, drawEndIndex, scaleX, scaleY, lastSampleIndex);

        if (drawSpikes()) {
            if (spikeTrains != null && valuesAndIndexes.length > 0) {
                for (int i = 0; i < valuesAndIndexes.length; i++) {
                    //benchmark.start();
                    int verticesCount =
                        fillSpikesAndColorsBuffers(valuesAndIndexes[i], spikesVertices, spikesColors, glWindowWidth,
                            fromSample, toSample, drawSampleCount, GlUtils.SPIKE_TRAIN_COLORS[i]);
                    //benchmark.end();
                    glSpikes.draw(gl, spikesVertices, spikesColors, verticesCount);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override protected void startScroll() {
        if (getIsPaused() && !getIsSeeking()) onScrollStart();
    }

    /**
     * {@inheritDoc}
     */
    @Override protected void scroll(float dx) {
        if (getIsPaused()) onScroll(dx * getGlWindowWidth() / getSurfaceWidth());
    }

    /**
     * {@inheritDoc}
     */
    @Override protected void endScroll() {
        if (getIsPaused()) onScrollEnd();
    }

    /**
     * {@inheritDoc}
     */
    @Override protected void startMeasurement(float x) {
        if (getIsPaused()) {
            measurementStartX = x;
            measurementEndX = x;
            measuring = true;

            onMeasureStart();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override protected void measure(float x) {
        if (getIsPlaybackMode() && getIsPaused()) measurementEndX = x;
    }

    /**
     * {@inheritDoc}
     */
    @Override protected void endMeasurement(float x) {
        if (getIsPaused()) {
            measuring = false;
            measurementStartX = 0;
            measurementEndX = 0;

            onMeasureEnd();
        }
    }

    // Fills spike and color buffers preparing them for drawing. Number of vertices is returned.
    private int fillSpikesAndColorsBuffers(@NonNull SpikeIndexValue[] valueAndIndices, @NonNull float[] spikesVertices,
        @NonNull float[] spikesColors, int glWindowWidth, long fromSample, long toSample, long drawSampleCount,
        @Size(4) float[] color) {
        int verticesCounter = 0;
        try {
            if (valueAndIndices.length > 0) {
                int colorsCounter = 0;
                float scaleX = (float) drawSampleCount / glWindowWidth;
                long index;

                for (SpikeIndexValue valueAndIndex : valueAndIndices) {
                    if (fromSample <= valueAndIndex.getIndex() && valueAndIndex.getIndex() < toSample) {
                        index =
                            toSample - fromSample < glWindowWidth ? valueAndIndex.getIndex() + glWindowWidth - toSample
                                : valueAndIndex.getIndex() - fromSample;
                        index = (long) (index * scaleX);
                        spikesVertices[verticesCounter++] = index;
                        spikesVertices[verticesCounter++] = valueAndIndex.getValue();
                        System.arraycopy(color, 0, spikesColors, colorsCounter, color.length);
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

    // Check whether service is currently in playback mode
    private boolean getIsPlaybackMode() {
        return getAudioService() != null && getAudioService().isPlaybackMode();
    }

    // Check whether audio is currently in the paused state
    private boolean getIsPaused() {
        return getAudioService() == null || getAudioService().isAudioPaused();
    }

    // Check whether audio is currently being sought
    private boolean getIsSeeking() {
        return getAudioService() != null && getAudioService().isAudioSeeking();
    }
}