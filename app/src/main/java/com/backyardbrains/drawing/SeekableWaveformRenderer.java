package com.backyardbrains.drawing;

import android.support.annotation.NonNull;
import android.support.annotation.Size;
import android.util.SparseArray;
import com.backyardbrains.BaseFragment;
import com.backyardbrains.data.SpikeValueAndIndex;
import com.backyardbrains.data.persistance.AnalysisDataSource;
import com.backyardbrains.data.persistance.entity.Train;
import com.backyardbrains.data.processing.ProcessingBuffer;
import com.backyardbrains.utils.AnalysisUtils;
import com.crashlytics.android.Crashlytics;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.LOGE;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class SeekableWaveformRenderer extends WaveformRenderer {

    private static final String TAG = makeLogTag(SeekableWaveformRenderer.class);

    private static final float RMS_QUANTIFIER = 0.005f;

    private final GlMeasurementArea glMeasurementArea;
    private final GlSpikes glSpikes;
    private final float[] spikesVertices = new float[ProcessingBuffer.MAX_BUFFER_SIZE * 2];
    private final float[] spikesColors = new float[ProcessingBuffer.MAX_BUFFER_SIZE * 4];
    private final float[][] colors = new float[3][];

    private short[] rmsSamples;
    private float drawSampleCount;

    private boolean measuring;
    private float measurementStartX;
    private float measurementEndX;

    @SuppressWarnings("WeakerAccess") Train[] spikeTrains;
    @SuppressWarnings("WeakerAccess") SpikeValueAndIndex[][] valuesAndIndexes;

    public SeekableWaveformRenderer(@NonNull String filePath, @NonNull BaseFragment fragment,
        @NonNull float[] preparedBuffer) {
        super(fragment, preparedBuffer);

        setScrollEnabled();
        setMeasureEnabled(true);

        glMeasurementArea = new GlMeasurementArea();
        glSpikes = new GlSpikes();

        // populate train colors
        colors[0] = new float[] { 1f, 0f, 0f, 1f };
        colors[1] = new float[] { 1f, 1f, 0f, 1f };
        colors[2] = new float[] { 0f, 1f, 1f, 1f };

        if (getAnalysisManager() != null) {
            getAnalysisManager().getSpikeTrains(filePath, new AnalysisDataSource.GetAnalysisCallback<Train[]>() {
                @Override public void onAnalysisLoaded(@NonNull Train[] result) {
                    spikeTrains = result;
                    valuesAndIndexes = new SpikeValueAndIndex[spikeTrains.length][];
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

    // FIXME: 11-Apr-18 THIS IS A HACK FOR NOW SO THAT SUBCLASSES CAN TELL THE PARENT NOT TO DRAW SPIKES
    protected boolean drawSpikes() {
        return true;
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

    /**
     * {@inheritDoc}
     */
    @Override protected void draw(GL10 gl, @NonNull short[] samples, @NonNull float[] waveformVertices,
        @NonNull SparseArray<String> markers, int surfaceWidth, int surfaceHeight, int glWindowWidth,
        int glWindowHeight, int drawStartIndex, int drawEndIndex, float scaleX, float scaleY, long lastSampleIndex) {

        // let's save start and end sample positions that are being drawn before triggering the actual draw
        int toSample = (int) lastSampleIndex;
        int fromSample = Math.max(0, toSample - glWindowWidth);
        if (drawSpikes()) {
            if (getAnalysisManager() != null && spikeTrains != null) {
                for (int i = 0; i < spikeTrains.length; i++) {
                    valuesAndIndexes[i] =
                        getAnalysisManager().getSpikesByTrainForRange(spikeTrains[i].getId(), fromSample, toSample);
                }
            }
        }

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

            // draw measurement area
            glMeasurementArea.draw(gl, measurementStartX * scaleX, measurementEndX * scaleX, -glWindowHeight * .5f,
                glWindowHeight * .5f);
        }

        super.draw(gl, samples, waveformVertices, markers, surfaceWidth, surfaceHeight, glWindowWidth, glWindowHeight,
            drawStartIndex, drawEndIndex, scaleX, scaleY, lastSampleIndex);

        if (drawSpikes()) {
            if (spikeTrains != null && valuesAndIndexes.length > 0) {
                for (int i = 0; i < valuesAndIndexes.length; i++) {
                    int verticesCount =
                        fillSpikesAndColorsBuffers(valuesAndIndexes[i], spikesVertices, spikesColors, glWindowWidth,
                            fromSample, toSample, colors[i]);
                    glSpikes.draw(gl, spikesVertices, spikesColors, verticesCount);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override protected void startScroll() {
        if (getIsPlaybackMode() && getIsNotPlaying() && !getIsSeeking()) onScrollStart();
    }

    /**
     * {@inheritDoc}
     */
    @Override protected void scroll(float dx) {
        if (getIsPlaybackMode() && getIsNotPlaying()) onScroll(dx * getGlWindowWidth() / getSurfaceWidth());
    }

    /**
     * {@inheritDoc}
     */
    @Override protected void endScroll() {
        if (getIsPlaybackMode() && getIsNotPlaying()) onScrollEnd();
    }

    /**
     * {@inheritDoc}
     */
    @Override protected void startMeasurement(float x) {
        if (getIsPlaybackMode() && getIsNotPlaying()) {
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
        if (getIsPlaybackMode() && getIsNotPlaying()) measurementEndX = x;
    }

    /**
     * {@inheritDoc}
     */
    @Override protected void endMeasurement(float x) {
        if (getIsPlaybackMode() && getIsNotPlaying()) {
            measuring = false;
            measurementStartX = 0;
            measurementEndX = 0;

            onMeasureEnd();
        }
    }

    // Fills spike and color buffers preparing them for drawing. Number of vertices is returned.
    private int fillSpikesAndColorsBuffers(@NonNull SpikeValueAndIndex[] valueAndIndices,
        @NonNull float[] spikesVertices, @NonNull float[] spikesColors, int glWindowWidth, long fromSample,
        long toSample, @Size(4) float[] color) {
        int verticesCounter = 0;
        try {
            if (valueAndIndices.length > 0) {
                int colorsCounter = 0;
                long index;

                for (SpikeValueAndIndex valueAndIndex : valueAndIndices) {
                    if (fromSample <= valueAndIndex.getIndex() && valueAndIndex.getIndex() < toSample) {
                        index =
                            toSample - fromSample < glWindowWidth ? valueAndIndex.getIndex() + glWindowWidth - toSample
                                : valueAndIndex.getIndex() - fromSample;
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
    private boolean getIsNotPlaying() {
        return getAudioService() == null || !getAudioService().isAudioPlaying();
    }

    // Check whether audio is currently being sought
    private boolean getIsSeeking() {
        return getAudioService() != null && getAudioService().isAudioSeeking();
    }
}