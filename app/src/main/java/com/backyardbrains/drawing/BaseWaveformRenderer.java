package com.backyardbrains.drawing;

import android.content.Context;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import com.backyardbrains.drawing.gl.GlAveragingTriggerLine;
import com.backyardbrains.dsp.ProcessingBuffer;
import com.backyardbrains.dsp.SamplesWithEvents;
import com.backyardbrains.dsp.SignalProcessor;
import com.backyardbrains.ui.BaseFragment;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.BYBUtils;
import com.backyardbrains.utils.EventUtils;
import com.backyardbrains.utils.GlUtils;
import com.backyardbrains.utils.JniUtils;
import com.backyardbrains.utils.PrefUtils;
import com.backyardbrains.utils.SignalAveragingTriggerType;
import com.crashlytics.android.Crashlytics;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.LOGE;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public abstract class BaseWaveformRenderer extends BaseRenderer
    implements ProcessingBuffer.OnSignalPropertyChangeListener, TouchEnabledRenderer {

    private static final String TAG = makeLogTag(BaseWaveformRenderer.class);

    private static final float MAX_GL_VERTICAL_SIZE = Short.MAX_VALUE * 40f;
    static final float MAX_GL_VERTICAL_HALF_SIZE = MAX_GL_VERTICAL_SIZE * .5f;

    private static final float MAX_GL_VERTICAL_THIRD_SIZE = MAX_GL_VERTICAL_SIZE * .3f;
    private static final float MIN_WAVEFORM_SCALE_FACTOR = 1f;
    private static final float MAX_WAVEFORM_SCALE_FACTOR = 5000f;
    private static final float MIN_GL_WINDOW_WIDTH_IN_SECONDS = .0004f;
    private static final float AUTO_SCALE_PERCENT = .8f;

    // Lock used when reading/writing samples and events
    private static final Object lock = new Object();

    private final ProcessingBuffer processingBuffer;
    private final SparseArray<String> eventsBuffer;

    private final AtomicBoolean autoScale = new AtomicBoolean();

    @SuppressWarnings("WeakerAccess") SamplesWithEvents samplesWithEvents;

    private DrawMultichannelBuffer samplesBuffer =
        new DrawMultichannelBuffer(SignalProcessor.DEFAULT_CHANNEL_COUNT, SignalProcessor.DEFAULT_FRAME_SIZE);
    private DrawMultichannelBuffer averagedSamplesBuffer =
        new DrawMultichannelBuffer(SignalProcessor.DEFAULT_CHANNEL_COUNT,
            SignalProcessor.DEFAULT_AVERAGED_SAMPLE_BUFFER_SIZE);
    private int[] eventIndices = new int[EventUtils.MAX_EVENT_COUNT];
    private String[] eventNames = new String[EventUtils.MAX_EVENT_COUNT];

    private int surfaceWidth;
    private int surfaceHeight;
    private boolean surfaceSizeDirty;
    private float glWindowWidth = GlUtils.DEFAULT_GL_WINDOW_HORIZONTAL_SIZE;
    private boolean glWindowWidthDirty;
    private float waveformScaleFactor = GlUtils.DEFAULT_WAVEFORM_SCALE_FACTOR;
    private float[] waveformScaleFactors;
    private float[] tempWaveformScaleFactors;
    private float[] waveformPositions;
    private float[] tempWaveformPositions;
    private float scaleX;
    private float scaleY;

    private boolean scrollEnabled;
    private boolean measureEnabled;

    private boolean signalAveraging;
    private @SignalAveragingTriggerType int averagingTriggerType;

    private int sampleRate = AudioUtils.DEFAULT_SAMPLE_RATE;
    private int channelCount = SignalProcessor.DEFAULT_CHANNEL_COUNT;

    private int selectedChannel = 0;

    private OnDrawListener onDrawListener;
    private OnScrollListener onScrollListener;
    private OnMeasureListener onMeasureListener;

    private GlAveragingTriggerLine glAveragingTrigger;
    protected Context context;

    /**
     * Interface definition for a callback to be invoked on every surface redraw.
     */
    public interface OnDrawListener {
        /**
         * Listener that is invoked when surface is redrawn.
         *
         * @param drawSurfaceWidth Draw surface width.
         */
        void onDraw(float drawSurfaceWidth);
    }

    /**
     * Interface definition for a callback to be invoked while draw surface is scrolled.
     */
    public interface OnScrollListener {

        /**
         * Listener that is invoked when draw surface scroll starts.
         */
        void onScrollStart();

        /**
         * Listener that is invoked while draw surface is being scrolled.
         *
         * @param dx Delta x from the previous method call.
         */
        void onScroll(float dx);

        /**
         * Listener that is invoked when draw surface scroll ends.
         */
        void onScrollEnd();
    }

    /**
     * Interface definition for a callback to be invoked while drawn signal is being measured.
     */
    public interface OnMeasureListener {

        /**
         * Listener that is invoked when signal measurement starts.
         */
        void onMeasureStart();

        /**
         * Listener that is invoked while drawn signal is being measured.
         *
         * @param rms RMS value of the selected part of drawn signal.
         * @param firstTrainSpikeCount Number of spikes belonging to first train within selected part of drawn signal.
         * @param secondTrainSpikeCount Number of spikes belonging to second train within selected part of drawn signal.
         * @param thirdTrainSpikeCount Number of spikes belonging to third train within selected part of drawn signal.
         * @param sampleCount Number of spikes within selected part of drawn signal.
         */
        void onMeasure(float rms, int firstTrainSpikeCount, int secondTrainSpikeCount, int thirdTrainSpikeCount,
            int sampleCount);

        /**
         * Listener that is invoked when signal measurement ends.
         */
        void onMeasureEnd();
    }

    //==============================================
    //  CONSTRUCTOR & SETUP
    //==============================================

    BaseWaveformRenderer(@NonNull BaseFragment fragment) {
        super(fragment);

        context = fragment.getContext();

        processingBuffer = ProcessingBuffer.get();
        processingBuffer.setOnSignalPropertyChangeListener(this);
        eventsBuffer = new SparseArray<>(EventUtils.MAX_EVENT_COUNT);

        resetWaveformScaleFactorsAndPositions(channelCount);
    }

    /**
     * Cleans any occupied resources.
     */
    public void close() {
        processingBuffer.setOnSignalPropertyChangeListener(null);
    }

    //===========================================================
    //  OnSignalPropertyChangeListener INTERFACE IMPLEMENTATIONS
    //===========================================================

    /**
     * {@inheritDoc}
     *
     * @param sampleRate The new sample rate.
     */
    @Override public void onSampleRateChange(int sampleRate) {
        if (this.sampleRate < 0 || this.sampleRate == sampleRate) return;

        //setGlWindowWidth(glWindowWidth);

        LOGD(TAG, "setSampleRate(" + sampleRate + ")");
        final int minGlWindowWidth = (int) (sampleRate * MIN_GL_WINDOW_WIDTH_IN_SECONDS);
        final int maxGlWindowWidth = SignalProcessor.getMaxProcessedSamplesCount();

        // recalculate width of the GL window
        float newSize = glWindowWidth;
        if (newSize < minGlWindowWidth) newSize = minGlWindowWidth;
        if (newSize > maxGlWindowWidth) newSize = maxGlWindowWidth;
        // save new GL window
        glWindowWidth = newSize;
        // set GL window size dirty so we can recalculate projection
        glWindowWidthDirty = true;

        this.sampleRate = sampleRate;
    }

    /**
     * Returns sample rate.
     */
    protected int getSampleRate() {
        return sampleRate;
    }

    /**
     * {@inheritDoc}
     *
     * @param channelCount The new number of channels.
     */
    @Override public void onChannelCountChange(int channelCount) {
        if (this.channelCount < 1 || this.channelCount == channelCount) return;

        synchronized (lock) {
            resetLocalBuffers(channelCount);
            resetWaveformScaleFactorsAndPositions(channelCount);

            int frameSize = (int) Math.floor((float) SignalProcessor.getProcessedSamplesCount());
            samplesBuffer = new DrawMultichannelBuffer(channelCount, frameSize);
            frameSize = (int) Math.floor((float) SignalProcessor.getProcessedAveragedSamplesCount());
            averagedSamplesBuffer = new DrawMultichannelBuffer(channelCount, frameSize);

            // let's reset selected channel to 0
            setSelectedChannel(0);
        }

        this.channelCount = channelCount;
    }

    /**
     * Returns number of channels.
     */
    protected int getChannelCount() {
        return channelCount;
    }

    //==============================================
    // PUBLIC AND PROTECTED METHODS
    //==============================================

    /**
     * Registers a callback to be invoked on every surface redraw.
     *
     * @param listener The callback that will be run. This value may be {@code null}.
     */
    public void setOnDrawListener(@Nullable OnDrawListener listener) {
        this.onDrawListener = listener;
    }

    //==============================================
    //  SIGNAL AVERAGING
    //==============================================

    /**
     * Returns whether incoming signal is being averaged or not.
     */
    boolean isSignalAveraging() {
        return signalAveraging;
    }

    /**
     * Sets whether incoming signal should be averaged or not.
     */
    public void setSignalAveraging(boolean signalAveraging) {
        this.signalAveraging = signalAveraging;

        // we should reset buffers for averaged samples
        if (signalAveraging) resetAveragedSignal();
    }

    /**
     * Resets buffers for averaged samples
     */
    public void resetAveragedSignal() {
        int frameSize = (int) Math.floor((float) SignalProcessor.getProcessedAveragedSamplesCount());
        averagedSamplesBuffer = new DrawMultichannelBuffer(channelCount, frameSize);
    }

    /**
     * Sets type for signal averaging. Can be one of {@link SignalAveragingTriggerType}.
     */
    public void setAveragingTriggerType(@SignalAveragingTriggerType int averagingTriggerType) {
        this.averagingTriggerType = averagingTriggerType;
    }

    //==============================================
    //  CHANNELS
    //==============================================

    /**
     * Returns currently selected channel.
     */
    int getSelectedChanel() {
        return selectedChannel;
    }

    /**
     * Sets currently selected channel.
     */
    public void setSelectedChannel(int selectedChannel) {
        if (getChannelCount() <= selectedChannel) return;

        // pass selected channel to native code
        JniUtils.setSelectedChannel(selectedChannel);

        this.selectedChannel = selectedChannel;
    }

    int getSurfaceWidth() {
        return surfaceWidth;
    }

    void setGlWindowWidth(float width) {
        if (width < 0) return;

        final int minGlWindowWidth = (int) (sampleRate * MIN_GL_WINDOW_WIDTH_IN_SECONDS);
        final int maxGlWindowWidth = SignalProcessor.getMaxProcessedSamplesCount();

        if (width < minGlWindowWidth) width = minGlWindowWidth;
        if (width > maxGlWindowWidth) width = maxGlWindowWidth;
        // save new GL windows width
        glWindowWidth = width;
        // set GL window size dirty so we can recalculate projection
        glWindowWidthDirty = true;
    }

    public float getGlWindowWidth() {
        return glWindowWidth;
    }

    private void initWaveformScaleFactor(float scaleFactor) {
        if (scaleFactor < 0 || scaleFactor == waveformScaleFactors[selectedChannel]) return;
        waveformScaleFactors[selectedChannel] = scaleFactor;
        if (selectedChannel == 0) waveformScaleFactor = waveformScaleFactors[selectedChannel];
    }

    void setWaveformScaleFactor(float scaleFactor) {
        if (scaleFactor < 0 || scaleFactor == waveformScaleFactors[selectedChannel]) return;
        scaleFactor *= waveformScaleFactors[selectedChannel];
        if (scaleFactor < MIN_WAVEFORM_SCALE_FACTOR) scaleFactor = MIN_WAVEFORM_SCALE_FACTOR;
        if (scaleFactor > MAX_WAVEFORM_SCALE_FACTOR) scaleFactor = MAX_WAVEFORM_SCALE_FACTOR;

        waveformScaleFactors[selectedChannel] = scaleFactor;
        if (selectedChannel == 0) waveformScaleFactor = waveformScaleFactors[selectedChannel];
    }

    float getWaveformScaleFactor() {
        return waveformScaleFactors[selectedChannel];
    }

    void moveGlWindowForSelectedChannel(float dy) {
        // save new waveform position for currently selected channel that will be used when setting up projection on the next draw cycle
        waveformPositions[selectedChannel] -= surfaceHeightToGlHeight(dy);
    }

    public float surfaceYToGlY(float surfaceY) {
        return BYBUtils.map(surfaceY, surfaceHeight, 0, -MAX_GL_VERTICAL_HALF_SIZE, MAX_GL_VERTICAL_HALF_SIZE);
    }

    float surfaceHeightToGlHeight(float surfaceHeight) {
        return BYBUtils.map(surfaceHeight, 0f, this.surfaceHeight, 0f, MAX_GL_VERTICAL_SIZE);
    }

    int glYToSurfaceY(float glY) {
        return (int) BYBUtils.map(glY, -MAX_GL_VERTICAL_HALF_SIZE, MAX_GL_VERTICAL_HALF_SIZE, surfaceHeight, 0f);
    }

    int glHeightToSurfaceHeight(float glHeight) {
        return (int) BYBUtils.map(glHeight, 0f, MAX_GL_VERTICAL_SIZE, 0f, surfaceHeight);
    }

    //==============================================
    //  SETTINGS
    //==============================================

    /**
     * Called to ask renderer to load it's local settings so it can render inital state correctly. It is the counterpart
     * to {@link #onSaveSettings(Context)}.
     *
     * This method should typically be called in {@link android.app.Activity#onStart Activity.onStart}. Subclasses
     * should override this method if they need to load any renderer specific settings.
     */
    @CallSuper public void onLoadSettings(@NonNull Context context) {
        surfaceWidth = PrefUtils.getViewportWidth(context, getClass());
        surfaceHeight = PrefUtils.getViewportHeight(context, getClass());
        surfaceSizeDirty = true;
        setGlWindowWidth(PrefUtils.getGlWindowHorizontalSize(context, getClass()));
        initWaveformScaleFactor(PrefUtils.getWaveformScaleFactor(context, getClass()));
    }

    /**
     * Called to ask renderer to save it's local settings so they can be retrieved when renderer is recreated. It is the
     * counterpart to {@link #onLoadSettings(Context)}.
     *
     * This method should typically be called in {@link android.app.Activity#onStart Activity.onStop}. Subclasses
     * should override this method if they need to save any renderer specific settings.
     */
    @CallSuper public void onSaveSettings(@NonNull Context context) {
        PrefUtils.setViewportWidth(context, getClass(), surfaceWidth);
        PrefUtils.setViewportHeight(context, getClass(), surfaceHeight);
        PrefUtils.setGlWindowHorizontalSize(context, getClass(), glWindowWidth);
        PrefUtils.setWaveformScaleFactor(context, getClass(), waveformScaleFactors[0]);
    }

    //==============================================
    //  Renderer INTERFACE IMPLEMENTATIONS
    //==============================================

    /**
     * {@inheritDoc}
     */
    @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        LOGD(TAG, "onSurfaceCreated()");
        gl.glClearColor(0f, 0f, 0f, 1.0f);
        gl.glClearDepthf(1.0f);
        gl.glDepthFunc(GL10.GL_LEQUAL);
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glEnable(GL10.GL_LINE_SMOOTH);
        gl.glEnable(GL10.GL_POINT_SMOOTH);
        gl.glDisable(GL10.GL_DITHER);
        gl.glHint(GL10.GL_LINE_SMOOTH_HINT, GL10.GL_NICEST);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);

        // we draw signal averaging trigger line with this object when triggering by events
        glAveragingTrigger = new GlAveragingTriggerLine(context, gl);
    }

    /**
     * {@inheritDoc}
     */
    @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
        LOGD(TAG, "onSurfaceCreated()");

        // save new surface width and height
        surfaceWidth = width;
        surfaceHeight = height;
        // set surface size dirty so we can recalculate scale
        surfaceSizeDirty = true;

        resetLocalBuffers(channelCount);

        gl.glViewport(0, 0, width, height);
        reshape(gl, surfaceWidth);
    }

    //private final Benchmark benchmark = new Benchmark("RENDERER_DRAW_TEST").warmUp(500)
    //    .sessions(10)
    //    .measuresPerSession(500)
    //    .logBySession(false)
    //    .listener(() -> {
    //        //EventBus.getDefault().post(new ShowToastEvent("PRESS BACK BUTTON!!!!"));
    //    });

    /**
     * {@inheritDoc}
     */
    @Override public void onDrawFrame(GL10 gl) {
        synchronized (lock) {
            //benchmark.start();

            // copy samples, averaged samples and events to local buffers
            final int copiedEventsCount =
                processingBuffer.copy(samplesBuffer, averagedSamplesBuffer, eventIndices, eventNames);
            // select buffer for drawing
            DrawMultichannelBuffer tmpSampleBuffer = signalAveraging ? averagedSamplesBuffer : samplesBuffer;

            // it's possible that channel counts of incoming samples and draw buffer are out of sync because this is
            // executed in background thread so if that's the case, let's just skip this draw cycle
            if (tmpSampleBuffer.getChannelCount() != samplesWithEvents.channelCount) return;

            // auto-scale before drawing if necessary
            if (autoScale.compareAndSet(true, false)) autoScale(tmpSampleBuffer.getChannel(selectedChannel));

            final boolean surfaceSizeDirty = this.surfaceSizeDirty;
            final int surfaceWidth = this.surfaceWidth;
            final int surfaceHeight = this.surfaceHeight;
            final boolean glWindowWidthDirty = this.glWindowWidthDirty;
            final float glWindowWidth = this.glWindowWidth;
            System.arraycopy(waveformScaleFactors, 0, tempWaveformScaleFactors, 0, waveformScaleFactors.length);
            System.arraycopy(waveformPositions, 0, tempWaveformPositions, 0, waveformPositions.length);
            final boolean signalAveraging = this.signalAveraging;
            final int averagingTriggerType = this.averagingTriggerType;

            // let's reset dirty flags right away
            this.glWindowWidthDirty = false;

            final int frameCount = tmpSampleBuffer.getFrameCount();
            final long lastSampleIndex = processingBuffer.getLastSampleIndex();

            // calculate necessary drawing parameters
            int drawStartIndex = (int) Math.max(frameCount - glWindowWidth, -glWindowWidth);
            if (drawStartIndex + glWindowWidth > frameCount) drawStartIndex = (int) (frameCount - glWindowWidth);
            final int drawEndIndex = (int) Math.min(drawStartIndex + glWindowWidth, frameCount);

            // construct waveform vertices and populate eventIndices buffer
            getWaveformVertices(samplesWithEvents, tmpSampleBuffer.getBuffer(), frameCount, eventIndices,
                copiedEventsCount, drawStartIndex, drawEndIndex, surfaceWidth);
            getEvents(samplesWithEvents, eventNames, copiedEventsCount, eventsBuffer);

            final float drawnSamplesCount = samplesWithEvents.sampleCountM[0] * .5f;
            // calculate scale x and scale y
            if (surfaceSizeDirty || glWindowWidthDirty) {
                scaleX = drawnSamplesCount > 0 ? glWindowWidth / drawnSamplesCount : 1f;
            }
            if (surfaceSizeDirty) {
                scaleY = surfaceHeight > 0 ? MAX_GL_VERTICAL_SIZE / surfaceHeight : 1f;
            }

            // init surface before drawing
            if (surfaceSizeDirty || glWindowWidthDirty) reshape(gl, drawnSamplesCount);

            // setup drawing surface and switch to Model-View matrix
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
            gl.glMatrixMode(GL10.GL_MODELVIEW);
            gl.glLoadIdentity();

            // draw on surface
            draw(gl, tmpSampleBuffer.getBuffer(), samplesWithEvents.samplesM, samplesWithEvents.sampleCountM,
                eventsBuffer, surfaceWidth, surfaceHeight, glWindowWidth, tempWaveformScaleFactors,
                tempWaveformPositions, drawStartIndex, drawEndIndex, scaleX, scaleY, lastSampleIndex);

            // draw average triggering line
            if (signalAveraging && averagingTriggerType != SignalAveragingTriggerType.THRESHOLD) {
                final float drawScale = drawnSamplesCount / surfaceWidth;
                glAveragingTrigger.draw(gl, getAveragingTriggerEventName(eventNames, copiedEventsCount),
                    drawnSamplesCount * .5f, -MAX_GL_VERTICAL_THIRD_SIZE, MAX_GL_VERTICAL_THIRD_SIZE, drawScale,
                    scaleY);
            }

            // invoke callback that the surface has been drawn
            if (onDrawListener != null) onDrawListener.onDraw(glWindowWidth);

            //benchmark.end();
        }
    }

    protected void getWaveformVertices(@NonNull SamplesWithEvents samplesWithEvents, @NonNull short[][] samples,
        int frameCount, @NonNull int[] eventIndices, int eventCount, int fromSample, int toSample,
        int drawSurfaceWidth) {
        //benchmark.start();
        try {
            JniUtils.prepareForDrawing(samplesWithEvents, samples, frameCount, eventIndices, eventCount, fromSample,
                toSample, drawSurfaceWidth);
        } catch (Exception e) {
            LOGE(TAG, e.getMessage());
            Crashlytics.logException(e);
        }
        //benchmark.end();
    }

    protected void getEvents(@NonNull SamplesWithEvents samplesWithEvents, @NonNull String[] eventNames, int eventCount,
        @NonNull SparseArray<String> eventsBuffer) {
        eventsBuffer.clear();
        int indexBase = eventCount - samplesWithEvents.eventCount;
        if (indexBase >= 0) {
            for (int i = 0; i < samplesWithEvents.eventCount; i++) {
                eventsBuffer.put(samplesWithEvents.eventIndices[i], eventNames[indexBase + i]);
            }
        }
    }

    abstract protected void draw(GL10 gl, @NonNull short[][] samples, @NonNull short[][] waveformVertices,
        int[] waveformVerticesCount, @NonNull SparseArray<String> events, int surfaceWidth, int surfaceHeight,
        float glWindowWidth, float[] waveformScaleFactors, float[] waveformPositions, int drawStartIndex,
        int drawEndIndex, float scaleX, float scaleY, long lastFrameIndex);

    private void reshape(GL10 gl, float drawnSamplesCount) {
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrthof(0f, drawnSamplesCount - 1, -MAX_GL_VERTICAL_HALF_SIZE, MAX_GL_VERTICAL_HALF_SIZE, -1f, 1f);
    }

    @Nullable private String getAveragingTriggerEventName(String[] eventNames, int eventsCount) {
        if (averagingTriggerType == SignalAveragingTriggerType.ALL_EVENTS && eventsCount > 0) {
            return eventNames[eventsCount - 1];
        }
        return null;
    }

    private void resetLocalBuffers(int channelCount) {
        int maxSamplesPerChannel = Math.max(surfaceWidth, surfaceHeight) * 8;
        if (samplesWithEvents == null || samplesWithEvents.channelCount != channelCount
            || samplesWithEvents.maxSamplesPerChannel < maxSamplesPerChannel) {
            LOGD(TAG, "CHANNEL COUNT: " + channelCount);
            LOGD(TAG, "MAX SAMPLES PER CHANNEL: " + maxSamplesPerChannel);
            samplesWithEvents = new SamplesWithEvents(channelCount, maxSamplesPerChannel);
        }
    }

    private void resetWaveformScaleFactorsAndPositions(int channelCount) {
        waveformScaleFactors = new float[channelCount];
        for (int i = 0; i < channelCount; i++) {
            waveformScaleFactors[i] = waveformScaleFactor;
        }
        waveformPositions = new float[channelCount];
        float step = MAX_GL_VERTICAL_SIZE / (channelCount + 1);
        float prev = -MAX_GL_VERTICAL_HALF_SIZE;
        for (int i = 0; i < channelCount; i++) {
            waveformPositions[i] = prev + step;
            prev += step;
        }
        tempWaveformScaleFactors = new float[channelCount];
        tempWaveformPositions = new float[channelCount];
    }

    //==============================================
    //  SCROLLING
    //==============================================

    /**
     * Registers a callback to be invoked on waveform scroll interaction.
     *
     * @param listener The callback that will be run. This value may be {@code null}.
     */
    public void setOnScrollListener(@Nullable OnScrollListener listener) {
        this.onScrollListener = listener;
    }

    /**
     * Called when user starts scrolling the GL surface. This method is called only if {@link #isScrollEnabled()} returns {@code true}.
     */
    protected void startScroll() {
    }

    /**
     * Called repeatedly while user scrolls the GL surface. This method is called only if {@link #isScrollEnabled()} returns {@code true}.
     */
    protected void scroll(float dx) {
    }

    /**
     * Called when user stops scrolling the GL surface. This method is called only if {@link #isScrollEnabled()} returns {@code true}.
     */
    protected void endScroll() {
    }

    /**
     * Triggers {@link OnScrollListener#onScrollStart()} call.
     */
    final void onScrollStart() {
        if (onScrollListener != null) onScrollListener.onScrollStart();
    }

    /**
     * Triggers {@link OnScrollListener#onScroll(float)} call.
     */
    final void onScroll(float dx) {
        if (onScrollListener != null) onScrollListener.onScroll(dx);
    }

    /**
     * Triggers {@link OnScrollListener#onScrollEnd()} call.
     */
    final void onScrollEnd() {
        if (onScrollListener != null) onScrollListener.onScrollEnd();
    }

    /**
     * Whether scrolling of the surface view is enabled.
     */
    boolean isScrollEnabled() {
        return this.scrollEnabled;
    }

    /**
     * Enables scrolling of the surface view.
     */
    void setScrollEnabled() {
        this.scrollEnabled = true;
    }

    //==============================================
    //  MEASUREMENT
    //==============================================

    /**
     * Registers a callback to be invoked on signal measurement.
     *
     * @param listener The callback that will be run. This value may be {@code null}.
     */
    public void setOnMeasureListener(@Nullable OnMeasureListener listener) {
        this.onMeasureListener = listener;
    }

    /**
     * Called when user start GL surface measurement. This method is called only if {@link #isMeasureEnabled()} returns {@code true}.
     */
    protected void startMeasurement(float x) {
    }

    /**
     * Called repeatedly while GL surface is being measured. This method is called only if {@link #isMeasureEnabled()} returns {@code true}.
     */
    protected void measure(float x) {
    }

    /**
     * Called when user stop GL surface measurement. This method is called only if {@link #isMeasureEnabled()} returns {@code true}.
     */
    protected void endMeasurement(float x) {
    }

    /**
     * Triggers {@link OnMeasureListener#onMeasureStart()} call.
     */
    final void onMeasureStart() {
        if (onMeasureListener != null) onMeasureListener.onMeasureStart();
    }

    /**
     * Triggers {@link OnMeasureListener#onMeasure(float, int, int, int, int)} call.
     */
    final void onMeasure(float rms, int firstTrainSpikeCount, int secondTrainSpikeCount, int thirdTrainSpikeCount,
        int sampleCount) {
        if (onMeasureListener != null) {
            onMeasureListener.onMeasure(rms, firstTrainSpikeCount, secondTrainSpikeCount, thirdTrainSpikeCount,
                sampleCount);
        }
    }

    /**
     * Triggers {@link OnMeasureListener#onMeasureEnd()} call.
     */
    final void onMeasureEnd() {
        if (onMeasureListener != null) onMeasureListener.onMeasureEnd();
    }

    /**
     * Whether measurement of the signal is enabled.
     */
    boolean isMeasureEnabled() {
        return measureEnabled;
    }

    /**
     * Sets whether measurement of the signal will be enabled.
     */
    void setMeasureEnabled(boolean enabled) {
        this.measureEnabled = enabled;
    }

    //==============================================
    //  AUTO-SCALE
    //==============================================

    /**
     * Called when drawing surface is double-tapped. This method is called only if {@link #isAutoScaleEnabled()} returns {@code true}.
     */
    void autoScale() {
        autoScale.set(true);
    }

    /**
     * Whether auto-scale of the signal on double-tap is enabled.
     */
    boolean isAutoScaleEnabled() {
        return !signalAveraging;
    }

    // Does actual auto-scaling
    private void autoScale(@NonNull short[] samples) {
        int max = 0, min = 0;
        for (short sample : samples) {
            if (max < sample) max = sample;
            if (min > sample) min = sample;
        }

        if (max != 0 && min != 0) {
            final int maxY;
            if (Math.abs(max) >= Math.abs(min)) {
                maxY = Math.abs(max) * 2;
            } else {
                maxY = Math.abs(min) * 2;
            }
            if (-maxY > GlUtils.DEFAULT_MIN_DETECTED_PCM_VALUE) {
                initWaveformScaleFactor(MAX_GL_VERTICAL_SIZE / maxY * AUTO_SCALE_PERCENT);
            }
        }
    }
}
