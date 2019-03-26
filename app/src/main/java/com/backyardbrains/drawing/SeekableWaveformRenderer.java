package com.backyardbrains.drawing;

import android.support.annotation.NonNull;
import android.support.annotation.Size;
import com.backyardbrains.db.AnalysisDataSource;
import com.backyardbrains.db.entity.Train;
import com.backyardbrains.drawing.gl.GlMeasurementArea;
import com.backyardbrains.drawing.gl.GlSpikes;
import com.backyardbrains.ui.BaseFragment;
import com.backyardbrains.utils.AnalysisUtils;
import com.backyardbrains.utils.BYBUtils;
import com.backyardbrains.utils.JniUtils;
import com.backyardbrains.vo.SpikeIndexValue;
import com.crashlytics.android.Crashlytics;
import java.util.Arrays;
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

    private short[][] rmsSamples;
    private float measureSampleCount;

    private boolean measuring;
    private float measurementStartX;
    private float measurementEndX;

    private float prevFromSample, prevToSample;
    private float prevMeasurementStartX, prevMeasurementEndX;
    private int prevSelectedChannel;

    @SuppressWarnings("WeakerAccess") Train[][] spikeTrains;
    @SuppressWarnings("WeakerAccess") SpikeIndexValue[][][] valuesAndIndexes;

    public SeekableWaveformRenderer(@NonNull String filePath, @NonNull BaseFragment fragment) {
        super(fragment);

        setScrollEnabled();
        setMeasureEnabled(true);

        glMeasurementArea = new GlMeasurementArea();
        glSpikes = new GlSpikes();

        if (getAnalysisManager() != null) {
            getAnalysisManager().getSpikeTrains(filePath, new AnalysisDataSource.GetAnalysisCallback<Train[]>() {
                @Override public void onAnalysisLoaded(@NonNull Train[] result) {
                    // initialize arrays that will hold spike trains and spike data
                    init(result);
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
    @Override protected void setThreshold(float threshold) {
        super.setThreshold(threshold);

        JniUtils.resetThreshold();
    }

    /**
     * {@inheritDoc}
     */
    @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
        final int oldWidth = getSurfaceWidth();

        super.onSurfaceChanged(gl, width, height);

        // recalculate measurement start and end x coordinates
        measurementStartX = width * measurementStartX / oldWidth;
        measurementEndX = width * measurementEndX / oldWidth;
    }

    //private final Benchmark benchmark =
    //    new Benchmark("SPIKES_RETRIEVAL").warmUp(200).sessions(10).measuresPerSession(200).logBySession(false);

    /**
     * {@inheritDoc}
     */
    @Override protected void draw(GL10 gl, @NonNull short[][] samples, int selectedChannel,
        SignalDrawData signalDrawData, @NonNull EventsDrawData eventsDrawData, @NonNull FftDrawData fftDrawData,
        int surfaceWidth, int surfaceHeight, float glWindowWidth, float[] waveformScaleFactors,
        float[] waveformPositions, int drawStartIndex, int drawEndIndex, float scaleX, float scaleY,
        long lastFrameIndex) {
        // let's save start and end sample positions that are being drawn before triggering the actual draw
        final int toSample = (int) lastFrameIndex;
        final int fromSample = (int) Math.max(0, toSample - glWindowWidth);
        final boolean shouldQuerySamples = prevFromSample != fromSample || prevToSample != toSample;

        final float samplesToDraw = signalDrawData.sampleCounts[0] * .5f;
        final float drawScale = surfaceWidth > 0 ? samplesToDraw / surfaceWidth : 1f;

        if (spikeTrains != null && valuesAndIndexes != null) {
            if (drawSpikes()) {
                if (getAnalysisManager() != null) {
                    for (int i = 0; i < spikeTrains.length; i++) {
                        for (int j = 0; j < spikeTrains[i].length; j++) {
                            //benchmark.start();
                            if (spikeTrains[i][j] != null && shouldQuerySamples) {
                                valuesAndIndexes[i][j] =
                                    getAnalysisManager().getSpikesByTrainForRange(spikeTrains[i][j].getId(),
                                        spikeTrains[i][j].getChannel(), fromSample, toSample);
                            }
                            //benchmark.end();
                        }
                    }
                }
            }

            final int valuesAndIndexesCount = valuesAndIndexes.length;

            // draw measurement area
            if (measuring) {
                // calculate start and end measurement area draw coordinates
                final float measurementAreaDrawStart = measurementStartX;
                final float measurementAreaDrawEnd = measurementEndX;
                final boolean shouldRemeasure =
                    prevSelectedChannel != selectedChannel || prevMeasurementStartX != measurementAreaDrawStart
                        || prevMeasurementEndX != measurementAreaDrawEnd;
                // if start and end measurement area draw coordinates haven't changed from last draw don't waste resources recalculating
                if (shouldRemeasure || shouldQuerySamples) {
                    // convert measure start index to drawing plane
                    int measureStartIndex =
                        (int) BYBUtils.map(measurementAreaDrawStart, 0f, surfaceWidth, 0f, samplesToDraw);
                    // convert measure start index to sample plane
                    measureStartIndex = (int) BYBUtils.map(measureStartIndex, 0f, samplesToDraw - 1, 0f, glWindowWidth);
                    // convert measure end index to drawing plane
                    int measureEndIndex =
                        (int) BYBUtils.map(measurementAreaDrawEnd, 0f, surfaceWidth, 0f, samplesToDraw);
                    // convert measure end index to sample plane
                    measureEndIndex = (int) BYBUtils.map(measureEndIndex, 0f, samplesToDraw - 1, 0f, glWindowWidth);

                    int measureSampleCount = Math.abs(measureEndIndex - measureStartIndex);
                    // fill array of samples used for RMS calculation
                    if (rmsSamples == null || measureSampleCount != this.measureSampleCount) {
                        this.measureSampleCount = measureSampleCount;
                        rmsSamples = new short[valuesAndIndexesCount][measureSampleCount];
                    }

                    // calculate index for the first sample we take for measurement
                    int startIndex = Math.min(measureStartIndex, measureEndIndex);
                    final int measureFirstSampleIndex = drawStartIndex + startIndex;
                    final int diff = (int) (glWindowWidth - (toSample - fromSample));
                    if (diff > 0) startIndex -= diff;
                    startIndex += fromSample;

                    final float[] rms = new float[valuesAndIndexesCount];
                    final int[][] spikeCounts = new int[AnalysisUtils.MAX_SPIKE_TRAIN_COUNT][];
                    for (int channelIndex = 0; channelIndex < valuesAndIndexesCount; channelIndex++) {
                        // we need to check number of samples we're copying cause converting indices might have not been that precise
                        if (measureFirstSampleIndex + measureSampleCount > samples[channelIndex].length) {
                            measureSampleCount = samples[channelIndex].length - measureFirstSampleIndex;
                        }
                        System.arraycopy(samples[channelIndex], measureFirstSampleIndex, rmsSamples[channelIndex], 0,
                            measureSampleCount);
                        // calculate RMS
                        rms[channelIndex] =
                            AnalysisUtils.RMS(rmsSamples[channelIndex], measureSampleCount) * RMS_QUANTIFIER;
                        if (Float.isNaN(rms[channelIndex])) rms[channelIndex] = 0f;
                        for (int trainIndex = 0; trainIndex < valuesAndIndexes[channelIndex].length; trainIndex++) {
                            // init spike counts for first train
                            if (spikeCounts[trainIndex] == null) {
                                spikeCounts[trainIndex] = new int[valuesAndIndexesCount];
                                Arrays.fill(spikeCounts[trainIndex], -1);
                            }
                            if (valuesAndIndexes[channelIndex][trainIndex] != null) {
                                spikeCounts[trainIndex][channelIndex] = 0;
                                for (int spikeIndex = 0; spikeIndex < valuesAndIndexes[channelIndex][trainIndex].length;
                                    spikeIndex++) {
                                    if (startIndex <= valuesAndIndexes[channelIndex][trainIndex][spikeIndex].getIndex()
                                        && valuesAndIndexes[channelIndex][trainIndex][spikeIndex].getIndex()
                                        <= startIndex + measureSampleCount) {
                                        spikeCounts[trainIndex][channelIndex]++;
                                    }
                                }
                            }
                        }
                    }

                    onMeasure(rms, spikeCounts[0], spikeCounts[1], spikeCounts[2], selectedChannel, measureSampleCount);
                }

                //draw measurement area
                gl.glPushMatrix();
                gl.glScalef(drawScale, 1f, 1f);
                gl.glTranslatef(measurementAreaDrawStart, -MAX_GL_VERTICAL_HALF_SIZE, 0f);
                glMeasurementArea.draw(gl, measurementAreaDrawEnd - measurementAreaDrawStart, MAX_GL_VERTICAL_SIZE,
                    Colors.GRAY_LIGHT, Colors.GRAY_50);
                gl.glPopMatrix();

                prevMeasurementStartX = measurementAreaDrawStart;
                prevMeasurementEndX = measurementAreaDrawEnd;
            }
        }

        super.draw(gl, samples, selectedChannel, signalDrawData, eventsDrawData, fftDrawData, surfaceWidth,
            surfaceHeight, glWindowWidth, waveformScaleFactors, waveformPositions, drawStartIndex, drawEndIndex, scaleX,
            scaleY, lastFrameIndex);

        if (valuesAndIndexes != null) {
            if (drawSpikes()) {
                for (int i = 0; i < valuesAndIndexes.length; i++) {
                    if (valuesAndIndexes[i] != null) {
                        for (int j = 0; j < valuesAndIndexes[i].length; j++) {
                            if (valuesAndIndexes[i][j] != null) {
                                //benchmark.start();
                                int verticesCount =
                                    fillSpikesAndColorsBuffers(valuesAndIndexes[i][j], spikesVertices, spikesColors,
                                        glWindowWidth, fromSample, toSample, (long) samplesToDraw,
                                        Colors.SPIKE_TRAIN_COLORS[j]);
                                //benchmark.end();
                                if (verticesCount > 0) {
                                    gl.glPushMatrix();
                                    gl.glTranslatef(0f, waveformPositions[i], 0f);
                                    gl.glScalef(1f, waveformScaleFactors[i], 1f);
                                    glSpikes.draw(gl, spikesVertices, spikesColors, verticesCount);
                                    gl.glPopMatrix();
                                }
                            }
                        }
                    }
                }
            }
        }

        prevFromSample = fromSample;
        prevToSample = toSample;
        prevSelectedChannel = selectedChannel;
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

    //==============================================
    //  PRIVATE METHODS
    //==============================================

    // Initializes arrays that will hold spike trains and spike data
    @SuppressWarnings("WeakerAccess") void init(Train[] trains) {
        int channelCount = -1;
        int trainCount = -1;
        // let's first check number of channels
        for (Train train : trains) {
            if (train.getChannel() > channelCount) channelCount = train.getChannel();
            if (train.getOrder() > trainCount) trainCount = train.getOrder();
        }
        channelCount += 1; // channels are ordered from 0 up so we need to add 1 to have actual count
        trainCount += 1; // ordering starts from 0 up so we need to add 1 to have actual train count

        // create and populate arrays that holds spike trains
        spikeTrains = new Train[channelCount][];
        for (int i = 0; i < channelCount; i++) {
            spikeTrains[i] = new Train[trainCount];
        }
        // initialize spike train arrays
        for (Train train : trains) {
            spikeTrains[train.getChannel()][train.getOrder()] = train;
        }

        // create arrays that holds spike data
        valuesAndIndexes = new SpikeIndexValue[channelCount][trainCount][];
    }

    // Fills spike and color buffers preparing them for drawing. Number of vertices is returned.
    private int fillSpikesAndColorsBuffers(@NonNull SpikeIndexValue[] valueAndIndices, @NonNull float[] spikesVertices,
        @NonNull float[] spikesColors, float glWindowWidth, long fromSample, long toSample, long drawSampleCount,
        @Size(4) float[] color) {
        int verticesCounter = 0;
        try {
            if (valueAndIndices.length > 0) {
                int colorsCounter = 0;
                float scaleX = (float) drawSampleCount / glWindowWidth;
                long index;

                for (SpikeIndexValue valueAndIndex : valueAndIndices) {
                    if (fromSample <= valueAndIndex.getIndex() && valueAndIndex.getIndex() < toSample) {
                        index = toSample - fromSample < glWindowWidth ? (long) (valueAndIndex.getIndex() + glWindowWidth
                            - toSample) : valueAndIndex.getIndex() - fromSample;
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
        return getProcessingService() != null && getProcessingService().isPlaybackMode();
    }

    // Check whether audio is currently in the paused state
    private boolean getIsPaused() {
        return getProcessingService() == null || getProcessingService().isAudioPaused();
    }

    // Check whether audio is currently being sought
    private boolean getIsSeeking() {
        return getProcessingService() != null && getProcessingService().isAudioSeeking();
    }
}