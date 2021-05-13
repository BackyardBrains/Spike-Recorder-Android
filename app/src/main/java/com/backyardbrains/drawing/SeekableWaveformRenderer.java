package com.backyardbrains.drawing;

import android.content.Context;
import androidx.annotation.NonNull;
import com.backyardbrains.R;
import com.backyardbrains.analysis.RmsHelper;
import com.backyardbrains.db.AnalysisDataSource;
import com.backyardbrains.db.entity.Train;
import com.backyardbrains.drawing.gl.GlLabel;
import com.backyardbrains.drawing.gl.GlLabelWithCircle;
import com.backyardbrains.drawing.gl.GlMeasurementArea;
import com.backyardbrains.drawing.gl.GlSpikes;
import com.backyardbrains.ui.BaseFragment;
import com.backyardbrains.utils.BYBUtils;
import com.backyardbrains.utils.Formats;
import com.backyardbrains.utils.JniUtils;
import com.backyardbrains.utils.ViewUtils;
import com.backyardbrains.vo.SpikeIndexValue;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import java.util.Arrays;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.LOGE;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class SeekableWaveformRenderer extends WaveformRenderer {

    private static final String TAG = makeLogTag(SeekableWaveformRenderer.class);

    // Width of the RMS time label in DPs
    private final static float RMS_TIME_LABEL_WIDTH_DP = 108f;
    // Height of the RMS time label in DPs
    private final static float RMS_TIME_LABEL_HEIGHT_DP = 28f;
    // Bottom margin for the RMS time label
    private static final float RMS_TIME_LABEL_BOTTOM_MARGIN_DPI = 10f;
    // Width of the RMS and spike count labels
    private final static float INFO_LABEL_WIDTH_DP = 160f;
    // Height of the RMS and spike count labels
    private final static float INFO_LABEL_HEIGHT_DP = 19f;
    // Space between RMS and spike count info labels
    private final static float INFO_LABEL_Y_SPACE_DP = 4f;
    // Top margin for the RMS info label
    private final static float RMS_INFO_LABEL_TOP_MARGIN_DP = 120f;
    // Colors of the spikes by train
    private final static float[][] SPIKE_COUNT_INFO_LABEL_CIRCLE_COLORS =
        new float[][] { Colors.RED, Colors.YELLOW, Colors.GREEN };

    private final Context context;

    private final GlMeasurementArea glMeasurementArea;
    private final GlSpikes glSpikes;
    private GlLabel glLabel;
    private GlLabelWithCircle glLabelWithCircle;

    private final RmsHelper rmsHelper;

    private final float infoLabelWidth;
    private final float infoLabelHeight;
    private final float infoLabelMove;
    private final float rmsInfoLabelY;
    private final float rmsTimeWidth;
    private final float rmsTimeHeight;
    private final float rmsTimeBottomMargin;

    private SpikesDrawData[] spikesDrawData;

    private float rms;
    private int measureSampleCount;
    private final int[] spikeCounts = new int[] { -1, -1, -1 };
    private final float[] spikesPerSecond = new float[spikeCounts.length];

    private boolean measuring;
    private float measurementStartX;
    private float measurementEndX;

    private float prevFromSample, prevToSample;
    private float prevMeasurementStartX, prevMeasurementEndX;
    private int prevSelectedChannel;
    private boolean prevShouldDraw;

    @SuppressWarnings("WeakerAccess") Train[][] spikeTrains;
    @SuppressWarnings("WeakerAccess") SpikeIndexValue[][][] valuesAndIndexes;

    public SeekableWaveformRenderer(@NonNull String filePath, @NonNull BaseFragment fragment) {
        super(fragment);

        context = fragment.getContext();

        setScrollEnabled();
        setMeasureEnabled(true);

        glMeasurementArea = new GlMeasurementArea();
        glSpikes = new GlSpikes();

        rmsHelper = new RmsHelper();

        //noinspection ConstantConditions
        infoLabelWidth = ViewUtils.dpToPx(context.getResources(), INFO_LABEL_WIDTH_DP);
        infoLabelHeight = ViewUtils.dpToPx(context.getResources(), INFO_LABEL_HEIGHT_DP);
        infoLabelMove = ViewUtils.dpToPx(context.getResources(), INFO_LABEL_Y_SPACE_DP) + infoLabelHeight;
        rmsInfoLabelY = -(infoLabelHeight + ViewUtils.dpToPx(context.getResources(), RMS_INFO_LABEL_TOP_MARGIN_DP));
        rmsTimeWidth = ViewUtils.dpToPx(context.getResources(), RMS_TIME_LABEL_WIDTH_DP);
        rmsTimeHeight = ViewUtils.dpToPx(context.getResources(), RMS_TIME_LABEL_HEIGHT_DP);
        rmsTimeBottomMargin = ViewUtils.dpToPx(context.getResources(), RMS_TIME_LABEL_BOTTOM_MARGIN_DPI);

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

    /**
     * {@inheritDoc}
     */
    @Override protected void setThreshold(float threshold) {
        super.setThreshold(threshold);

        JniUtils.resetThreshold();
    }

    @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        super.onSurfaceCreated(gl, config);

        glLabel = new GlLabel(context, gl);
        glLabelWithCircle = new GlLabelWithCircle(context, gl);
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
    @Override protected void draw(GL10 gl, @NonNull short[][] samples, @NonNull SignalDrawData signalDrawData,
        @NonNull EventsDrawData eventsDrawData, @NonNull FftDrawData fftDrawData, int selectedChannel, int surfaceWidth,
        int surfaceHeight, float glWindowWidth, float[] waveformScaleFactors, float[] waveformPositions,
        int drawStartIndex, int drawEndIndex, float scaleX, float scaleY, long lastFrameIndex) {
        // let's save start and end sample positions that are being drawn before triggering the actual draw
        final int toSample = (int) lastFrameIndex;
        final int fromSample = (int) Math.max(0, toSample - glWindowWidth);
        final boolean shouldQuerySamples = prevFromSample != fromSample || prevToSample != toSample;
        final boolean shouldDraw = !isSignalAveraging() && !isFftProcessing();
        final int sampleRate = getSampleRate();

        if (prevShouldDraw && !shouldDraw) onMeasureEnd();

        if (shouldDraw) {
            if (spikeTrains != null && valuesAndIndexes != null) {
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
                    // convert measure start index to sample plane
                    int measureStartIndex =
                        (int) BYBUtils.map(measurementAreaDrawStart, 0f, surfaceWidth, 0f, glWindowWidth);
                    // convert measure end index to drawing plane
                    int measureEndIndex =
                        (int) BYBUtils.map(measurementAreaDrawEnd, 0f, surfaceWidth, 0f, glWindowWidth);

                    rms = rmsHelper.calculateRms(samples[selectedChannel], drawStartIndex, measureStartIndex,
                        measureEndIndex);
                    measureSampleCount = rmsHelper.getMeasureSampleCount();

                    // calculate index for the first sample we take for measurement
                    int startIndex = Math.min(measureStartIndex, measureEndIndex);
                    final int diff = (int) (glWindowWidth - (toSample - fromSample));
                    if (diff > 0) startIndex -= diff;
                    startIndex += fromSample;

                    if (valuesAndIndexes != null) {
                        // init spike counts if necessary
                        if (prevSelectedChannel != selectedChannel) Arrays.fill(spikeCounts, -1);
                        for (int trainIndex = 0; trainIndex < valuesAndIndexes[selectedChannel].length; trainIndex++) {
                            if (valuesAndIndexes[selectedChannel][trainIndex] != null) {
                                spikeCounts[trainIndex] = 0;
                                for (int spikeIndex = 0;
                                    spikeIndex < valuesAndIndexes[selectedChannel][trainIndex].length; spikeIndex++) {
                                    if (startIndex <= valuesAndIndexes[selectedChannel][trainIndex][spikeIndex].index
                                        && valuesAndIndexes[selectedChannel][trainIndex][spikeIndex].index
                                        <= startIndex + measureSampleCount) {
                                        spikeCounts[trainIndex]++;
                                    }
                                }
                                spikesPerSecond[trainIndex] =
                                    (spikeCounts[trainIndex] * sampleRate) / (float) measureSampleCount;
                                if (Float.isInfinite(spikesPerSecond[trainIndex]) || Float.isNaN(
                                    spikesPerSecond[trainIndex])) {
                                    spikesPerSecond[trainIndex] = 0f;
                                }
                            }
                        }
                    }

                    // give chance to anyone listening to react on new measure
                    onMeasure();
                }

                // draw measurement area
                gl.glPushMatrix();
                gl.glTranslatef(measurementAreaDrawStart, -MAX_GL_VERTICAL_HALF_SIZE, 0f);
                glMeasurementArea.draw(gl, measurementAreaDrawEnd - measurementAreaDrawStart, MAX_GL_VERTICAL_SIZE,
                    Colors.GRAY_LIGHT, Colors.GRAY_50);
                gl.glPopMatrix();

                prevMeasurementStartX = measurementAreaDrawStart;
                prevMeasurementEndX = measurementAreaDrawEnd;
                prevSelectedChannel = selectedChannel;
            }
        }

        super.draw(gl, samples, signalDrawData, eventsDrawData, fftDrawData, selectedChannel, surfaceWidth,
            surfaceHeight, glWindowWidth, waveformScaleFactors, waveformPositions, drawStartIndex, drawEndIndex, scaleX,
            scaleY, lastFrameIndex);

        if (shouldDraw) {
            if (valuesAndIndexes != null) {
                int samplesToDraw = (int) (signalDrawData.samples[0].length * .5f);
                float[] color;
                for (int i = 0; i < valuesAndIndexes.length; i++) {
                    if (valuesAndIndexes[i] != null) {
                        for (int j = 0; j < valuesAndIndexes[i].length; j++) {
                            if (valuesAndIndexes[i][j] != null) {
                                color = Colors.SPIKE_TRAIN_COLORS[j];
                                try {
                                    JniUtils.prepareForSpikesDrawing(spikesDrawData[j], valuesAndIndexes[i][j], color,
                                        color, Integer.MIN_VALUE, Integer.MAX_VALUE, fromSample, toSample,
                                        drawStartIndex, drawEndIndex, samplesToDraw, surfaceWidth);
                                } catch (Exception e) {
                                    LOGE(TAG, e.getMessage());
                                    FirebaseCrashlytics.getInstance().recordException(e);
                                }
                                if (spikesDrawData[j].vertexCount > 0) {
                                    gl.glPushMatrix();
                                    gl.glTranslatef(0f, waveformPositions[i], 0f);
                                    gl.glScalef(1f, waveformScaleFactors[i], 1f);
                                    glSpikes.draw(gl, spikesDrawData[j].vertices, spikesDrawData[j].colors,
                                        spikesDrawData[j].vertexCount);
                                    gl.glPopMatrix();
                                }
                            }
                        }
                    }
                }
            }

            if (measuring) {
                // draw RMS time label
                gl.glPushMatrix();
                gl.glTranslatef(0f, -MAX_GL_VERTICAL_HALF_SIZE, 0f);
                gl.glScalef(1f, scaleY, 1f);
                gl.glTranslatef((surfaceWidth - rmsTimeWidth) * .5f, rmsTimeBottomMargin, 0f);
                glLabel.draw(gl, rmsTimeWidth, rmsTimeHeight,
                    Formats.formatTime_s_msec(measureSampleCount / (float) sampleRate * 1000), Colors.WHITE,
                    Colors.BLUE_LIGHT);
                gl.glPopMatrix();

                gl.glPushMatrix();
                gl.glTranslatef(0f, MAX_GL_VERTICAL_HALF_SIZE, 0f);
                gl.glScalef(1f, scaleY, 1f);

                // draw RMS info label
                gl.glPushMatrix();
                gl.glTranslatef(surfaceWidth - infoLabelWidth, rmsInfoLabelY, 0f);
                glLabel.draw(gl, infoLabelWidth, infoLabelHeight,
                    String.format(context.getString(R.string.template_rms), rms), Colors.GREEN, Colors.BLACK);
                for (int trainIndex = 0; trainIndex < spikeCounts.length; trainIndex++) {
                    if (spikeCounts[trainIndex] >= 0) {
                        // draw spike count label
                        gl.glTranslatef(0f, infoLabelMove, 0f);
                        glLabelWithCircle.draw(gl, infoLabelWidth, infoLabelHeight,
                            String.format(context.getString(R.string.template_spike_count), spikeCounts[trainIndex],
                                spikesPerSecond[trainIndex]), Colors.GREEN, Colors.BLACK,
                            SPIKE_COUNT_INFO_LABEL_CIRCLE_COLORS[trainIndex]);
                    }
                }
                gl.glPopMatrix();

                gl.glPopMatrix();
            }
        }

        prevFromSample = fromSample;
        prevToSample = toSample;
        prevShouldDraw = shouldDraw;
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
        // create
        spikesDrawData = new SpikesDrawData[trainCount];
        for (int i = 0; i < trainCount; i++) spikesDrawData[i] = new SpikesDrawData(GlSpikes.MAX_SPIKES);
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