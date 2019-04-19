package com.backyardbrains.drawing;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.drawing.gl.GlAveragingTriggerLine;
import com.backyardbrains.dsp.ProcessingBuffer;
import com.backyardbrains.dsp.SignalConfiguration;
import com.backyardbrains.dsp.SignalProcessor;
import com.backyardbrains.ui.BaseFragment;
import com.backyardbrains.utils.BYBUtils;
import com.backyardbrains.utils.EventUtils;
import com.backyardbrains.utils.GlUtils;
import com.backyardbrains.utils.PrefUtils;
import com.backyardbrains.utils.SignalAveragingTriggerType;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public abstract class BaseWaveformRenderer extends BaseRenderer
    implements SignalConfiguration.OnSignalPropertyChangeListener, TouchEnabledRenderer {

    private static final String TAG = makeLogTag(BaseWaveformRenderer.class);

    static final float MAX_GL_VERTICAL_SIZE = Short.MAX_VALUE * 40f;
    static final float MAX_GL_VERTICAL_HALF_SIZE = MAX_GL_VERTICAL_SIZE * .5f;

    private static final float MAX_GL_VERTICAL_SIXTH_SIZE = MAX_GL_VERTICAL_SIZE / 6f;
    private static final float MIN_WAVEFORM_SCALE_FACTOR = 1f;
    private static final float MAX_WAVEFORM_SCALE_FACTOR = 5000f;
    private static final float MIN_GL_WINDOW_WIDTH_IN_SECONDS = .0004f;
    private static final float AUTO_SCALE_PERCENT = .8f;

    // Lock used when reading/writing samples and events
    private static final Object lock = new Object();

    private final ProcessingBuffer processingBuffer;
    private final SignalConfiguration signalConfiguration;

    private final AtomicBoolean autoScale = new AtomicBoolean();

    private SignalDrawData signalDrawData;
    private EventsDrawData eventsDrawData;
    private FftDrawData fftDrawData;

    private MultichannelSignalDrawBuffer signalDrawBuffer =
        new MultichannelSignalDrawBuffer(SignalProcessor.DEFAULT_CHANNEL_COUNT, SignalProcessor.DEFAULT_FRAME_SIZE);
    private MultichannelSignalDrawBuffer visibleSignalDrawBuffer =
        new MultichannelSignalDrawBuffer(SignalProcessor.DEFAULT_CHANNEL_COUNT, SignalProcessor.DEFAULT_FRAME_SIZE);
    private MultichannelSignalDrawBuffer averagedSignalDrawBuffer =
        new MultichannelSignalDrawBuffer(SignalProcessor.DEFAULT_CHANNEL_COUNT,
            SignalProcessor.DEFAULT_AVERAGED_SAMPLE_BUFFER_SIZE);
    private FftDrawBuffer fftDrawBuffer =
        new FftDrawBuffer(SignalProcessor.DEFAULT_FFT_WINDOW_COUNT, SignalProcessor.DEFAULT_FFT_30HZ_WINDOW_SIZE);

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
    // Used for temporary storing event indices while copying data from processing buffer and it's preparation for drawing
    private final int[] eventIndices = new int[EventUtils.MAX_EVENT_COUNT];
    // Used for temporary storing event names while copying data from processing buffer and it's preparation for drawing
    private final String[] eventNames = new String[EventUtils.MAX_EVENT_COUNT];
    private float scaleX;
    private float scaleY;

    private boolean scrollEnabled;
    private boolean measureEnabled;

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
         * @param rms RMS value by channels of the selected part of drawn signal.
         * @param firstTrainSpikeCount Number of spikes belonging to first train by channels within selected part of drawn signal.
         * @param secondTrainSpikeCount Number of spikes belonging to second train by channels within selected part of drawn signal.
         * @param thirdTrainSpikeCount Number of spikes belonging to third train by channels within selected part of drawn signal.
         * @param selectedChannel Index of the currently selected channel.
         * @param sampleCount Number of spikes within selected part of drawn signal.
         */
        void onMeasure(@NonNull float[] rms, @Nullable int[] firstTrainSpikeCount,
            @Nullable int[] secondTrainSpikeCount, @Nullable int[] thirdTrainSpikeCount, int selectedChannel,
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
        signalConfiguration = SignalConfiguration.get();
        signalConfiguration.addOnSignalPropertyChangeListener(this);
        eventsDrawData = new EventsDrawData();
        fftDrawData = new FftDrawData();

        resetWaveformScaleFactorsAndPositions(signalConfiguration.getVisibleChannelCount());
    }

    /**
     * Cleans any occupied resources.
     */
    public void close() {
        signalConfiguration.removeOnSignalPropertyChangeListener(this);
    }

    //===========================================================
    //  OnSignalPropertyChangeListener INTERFACE IMPLEMENTATIONS
    //===========================================================

    /**
     * {@inheritDoc}
     *
     * @param sampleRate The new sample rate.
     */
    @Override public void onSampleRateChanged(int sampleRate) {
        LOGD(TAG, "setSampleRate(" + sampleRate + ")");

        synchronized (lock) {
            resetLocalSignalDrawBuffers(signalConfiguration.getChannelCount(),
                signalConfiguration.getVisibleChannelCount());
            fftDrawBuffer = new FftDrawBuffer(SignalProcessor.getProcessedFftWindowCount(),
                SignalProcessor.getProcessedFftWindowSize());
        }

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
    }

    /**
     * {@inheritDoc}
     *
     * @param channelCount The new number of channels.
     */
    @Override public void onChannelCountChanged(int channelCount) {
        LOGD(TAG, "setChannelCount(" + channelCount + ")");

        synchronized (lock) {
            final int visibleChannelCount = signalConfiguration.getVisibleChannelCount();
            resetLocalSignalDrawBuffers(channelCount, visibleChannelCount);
            resetLocalSignalDrawData(visibleChannelCount);
            resetWaveformScaleFactorsAndPositions(visibleChannelCount);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param channelConfig Array of booleans indicating which channel is on and which is off.
     */
    @Override public void onChannelConfigChanged(boolean[] channelConfig) {
        LOGD(TAG, "onChannelConfigChanged(" + Arrays.toString(channelConfig) + ")");

        synchronized (lock) {
            final int visibleChannelCount = signalConfiguration.getVisibleChannelCount();
            resetLocalSignalDrawBuffers(signalConfiguration.getChannelCount(), visibleChannelCount);
            resetLocalSignalDrawData(visibleChannelCount);
            resetWaveformScaleFactorsAndPositions(visibleChannelCount);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param signalAveraging Whether signal averaging is turned on/off.
     */
    @Override public void onSignalAveragingChanged(boolean signalAveraging) {
        final Context context = getContext();
        if (context != null) onSaveSettings(context);
        // we should reset buffers for averaged samples
        if (signalAveraging) resetAveragedSignal();
        if (context != null) onLoadSettings(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override public void onSignalAveragingTriggerTypeChanged(int triggerType) {
    }

    /**
     * {@inheritDoc}
     *
     * @param fftProcessing Whether fft processing is turned on/off.
     */
    @Override public void onFftProcessingChange(boolean fftProcessing) {
    }

    /**
     * Returns sample rate.
     */
    protected int getSampleRate() {
        return signalConfiguration.getSampleRate();
    }

    /**
     * Returns number of channels.
     */
    protected int getChannelCount() {
        return signalConfiguration.getChannelCount();
    }

    /**
     * Returns whether channel at specified {@code channelIndex} is visible or not.
     */
    protected boolean isChannelVisible(int channelIndex) {
        return signalConfiguration.isChannelVisible(channelIndex);
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

    /**
     * Registers a callback to be invoked on waveform scroll interaction.
     *
     * @param listener The callback that will be run. This value may be {@code null}.
     */
    public void setOnScrollListener(@Nullable OnScrollListener listener) {
        this.onScrollListener = listener;
    }

    /**
     * Registers a callback to be invoked on signal measurement.
     *
     * @param listener The callback that will be run. This value may be {@code null}.
     */
    public void setOnMeasureListener(@Nullable OnMeasureListener listener) {
        this.onMeasureListener = listener;
    }

    //==============================================
    //  SIGNAL AVERAGING
    //==============================================

    /**
     * Returns whether incoming signal is being averaged or not.
     */
    boolean isSignalAveraging() {
        return signalConfiguration.isSignalAveraging();
    }

    /**
     * Whether currently selected signal averaging type is of type {@link SignalAveragingTriggerType#THRESHOLD}.
     */
    boolean isThresholdAveragingTriggerType() {
        return signalConfiguration.getSignalAveragingTriggerType() == SignalAveragingTriggerType.THRESHOLD;
    }

    /**
     * Resets buffers for averaged samples
     */
    public void resetAveragedSignal() {
        int frameSize = (int) Math.floor((float) SignalProcessor.getProcessedAveragedSamplesCount());
        averagedSignalDrawBuffer =
            new MultichannelSignalDrawBuffer(signalConfiguration.getVisibleChannelCount(), frameSize);
    }

    //==============================================
    //  CHANNELS
    //==============================================

    /**
     * Returns whether incoming signal is passed through FFT.
     */
    boolean isFftProcessing() {
        return signalConfiguration.isFftProcessing();
    }

    //==============================================
    //  CHANNELS
    //==============================================

    /**
     * Returns index of the currently selected channel.
     */
    int getSelectedChanel() {
        return signalConfiguration.getSelectedChannel();
    }

    int getSurfaceWidth() {
        return surfaceWidth;
    }

    void setGlWindowWidth(float width) {
        if (width < 0) return;

        final int minGlWindowWidth = (int) (signalConfiguration.getSampleRate() * MIN_GL_WINDOW_WIDTH_IN_SECONDS);
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
        if (signalConfiguration.getVisibleChannelCount() <= 0) return;

        int selectedChannel = signalConfiguration.getSelectedChannel();
        if (scaleFactor < 0 || scaleFactor == waveformScaleFactors[selectedChannel]) return;
        waveformScaleFactors[selectedChannel] = scaleFactor;
        if (selectedChannel == 0) waveformScaleFactor = waveformScaleFactors[selectedChannel];
    }

    void setWaveformScaleFactor(float scaleFactor) {
        if (signalConfiguration.getVisibleChannelCount() <= 0) return;

        int selectedChannel = signalConfiguration.getSelectedChannel();
        if (scaleFactor < 0 || scaleFactor == waveformScaleFactors[selectedChannel]) return;
        scaleFactor *= waveformScaleFactors[selectedChannel];
        if (scaleFactor < MIN_WAVEFORM_SCALE_FACTOR) scaleFactor = MIN_WAVEFORM_SCALE_FACTOR;
        if (scaleFactor > MAX_WAVEFORM_SCALE_FACTOR) scaleFactor = MAX_WAVEFORM_SCALE_FACTOR;

        waveformScaleFactors[selectedChannel] = scaleFactor;
        if (selectedChannel == 0) waveformScaleFactor = waveformScaleFactors[selectedChannel];
    }

    float getWaveformScaleFactor() {
        return waveformScaleFactors[signalConfiguration.getSelectedChannel()];
    }

    void moveGlWindowForSelectedChannel(float dy) {
        if (signalConfiguration.getVisibleChannelCount() <= 0) return;

        // save new waveform position for currently selected channel that will be used when setting up projection on the next draw cycle
        waveformPositions[signalConfiguration.getSelectedChannel()] -= surfaceHeightToGlHeight(dy);
    }

    float surfaceYToGlY(float surfaceY) {
        return BYBUtils.map(surfaceY, surfaceHeight, 0, -MAX_GL_VERTICAL_HALF_SIZE, MAX_GL_VERTICAL_HALF_SIZE);
    }

    float surfaceHeightToGlHeight(float surfaceHeight) {
        return BYBUtils.map(surfaceHeight, 0f, this.surfaceHeight, 0f, MAX_GL_VERTICAL_SIZE);
    }

    int glYToSurfaceY(float glY) {
        return (int) BYBUtils.map(glY, -MAX_GL_VERTICAL_HALF_SIZE, MAX_GL_VERTICAL_HALF_SIZE, 0f, surfaceHeight);
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
     * This method should typically be called in {@link Activity#onStart()}. Subclasses
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
     * This method should typically be called in {@link Activity#onStart()}. Subclasses
     * should override this method if they need to save any renderer specific settings.
     */
    @CallSuper public void onSaveSettings(@NonNull Context context) {
        PrefUtils.setViewportWidth(context, getClass(), surfaceWidth);
        PrefUtils.setViewportHeight(context, getClass(), surfaceHeight);
        PrefUtils.setGlWindowHorizontalSize(context, getClass(), glWindowWidth);
        if (signalConfiguration.getVisibleChannelCount() > 0) {
            PrefUtils.setWaveformScaleFactor(context, getClass(), waveformScaleFactors[0]);
        }
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

        resetLocalSignalDrawData(signalConfiguration.getVisibleChannelCount());

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

            if (signalConfiguration.getVisibleChannelCount() <= 0) {
                gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
                return;
            }

            final int selectedChannel = signalConfiguration.getSelectedChannel();
            final boolean signalAveraging = signalConfiguration.isSignalAveraging();
            final int averagingTriggerType = signalConfiguration.getSignalAveragingTriggerType();

            // copy samples, averaged samples and events and fft to local buffers
            final int copiedEventsCount =
                processingBuffer.copy(signalDrawBuffer, averagedSignalDrawBuffer, eventIndices, eventNames,
                    fftDrawBuffer);

            signalDrawBuffer.copyReconfigured(visibleSignalDrawBuffer, signalConfiguration);

            // select buffer for drawing
            MultichannelSignalDrawBuffer tmpSampleDrawBuffer =
                signalAveraging ? averagedSignalDrawBuffer : visibleSignalDrawBuffer;

            // auto-scale before drawing if necessary
            if (autoScale.compareAndSet(true, false)) autoScale(tmpSampleDrawBuffer.getChannel(selectedChannel));

            final boolean surfaceSizeDirty = this.surfaceSizeDirty;
            final int surfaceWidth = this.surfaceWidth;
            final int surfaceHeight = this.surfaceHeight;
            final boolean glWindowWidthDirty = this.glWindowWidthDirty;
            final float glWindowWidth = this.glWindowWidth;
            System.arraycopy(waveformScaleFactors, 0, tempWaveformScaleFactors, 0, waveformScaleFactors.length);
            System.arraycopy(waveformPositions, 0, tempWaveformPositions, 0, waveformPositions.length);

            // let's reset dirty flags right away
            this.glWindowWidthDirty = false;

            final int frameCount = tmpSampleDrawBuffer.getFrameCount();
            final long lastSampleIndex = processingBuffer.getLastSampleIndex();

            // calculate necessary drawing parameters
            int drawStartIndex = (int) Math.max(frameCount - glWindowWidth, -glWindowWidth);
            if (drawStartIndex + glWindowWidth > frameCount) drawStartIndex = (int) (frameCount - glWindowWidth);
            final int drawEndIndex = (int) Math.min(drawStartIndex + glWindowWidth, frameCount);
            final float drawnSamplesCount = signalDrawData.sampleCounts[0] * .5f;

            // prepare signal data for drawing
            prepareSignalForDrawing(signalDrawData, eventsDrawData, tmpSampleDrawBuffer.getBuffer(), frameCount,
                eventIndices, copiedEventsCount, drawStartIndex, drawEndIndex, surfaceWidth);
            // prepare events for drawing
            addEventsToEventDrawData(eventsDrawData, eventNames, copiedEventsCount);
            // prepare FFT data for drawing
            prepareFftForDrawing(fftDrawData, fftDrawBuffer.getBuffer(), (int) drawnSamplesCount);

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

            // draw on surface
            draw(gl, tmpSampleDrawBuffer.getBuffer(), selectedChannel, signalDrawData, eventsDrawData, fftDrawData,
                surfaceWidth, surfaceHeight, glWindowWidth, tempWaveformScaleFactors, tempWaveformPositions,
                drawStartIndex, drawEndIndex, scaleX, scaleY, lastSampleIndex);

            // draw average triggering line
            if (signalAveraging && averagingTriggerType != SignalAveragingTriggerType.THRESHOLD) {
                final float drawScale = surfaceWidth > 0 ? drawnSamplesCount / surfaceWidth : 1f;
                gl.glPushMatrix();
                gl.glTranslatef(drawnSamplesCount * .5f, -MAX_GL_VERTICAL_HALF_SIZE + MAX_GL_VERTICAL_SIXTH_SIZE, 0f);
                glAveragingTrigger.draw(gl, getAveragingTriggerEventName(eventNames, copiedEventsCount),
                    MAX_GL_VERTICAL_SIXTH_SIZE * 4, drawScale, scaleY);
                gl.glPopMatrix();
            }

            // invoke callback that the surface has been drawn
            if (onDrawListener != null) onDrawListener.onDraw(glWindowWidth);

            //benchmark.end();
        }
    }

    /**
     * Prepares incoming signal data for drawing.
     *
     * @param outSignalData Signal data prepared for drawing.
     * @param outEvents Events mapped to signal data prepared for drawing.
     * @param inSamples Incoming signal data.
     * @param inFrameCount Number of incoming signal frames.
     * @param inEventIndices Indices of events mapped to incoming signal.
     * @param inEventCount Number of events.
     * @param fromSample Index of the first signal sample to take into account.
     * @param toSample Index of the last signal sample to take into account.
     * @param drawSurfaceWidth Width of the surface signal is being drawn to.
     */
    abstract protected void prepareSignalForDrawing(@NonNull SignalDrawData outSignalData, EventsDrawData outEvents,
        @NonNull short[][] inSamples, int inFrameCount, @NonNull int[] inEventIndices, int inEventCount, int fromSample,
        int toSample, int drawSurfaceWidth);

    /**
     * Adds events to event draw data and calculates their offset in regard to already drawn events.
     *
     * @param eventsDrawData Holds events indices and names mapped to incoming signal sample batch.
     * @param eventNames Names of the events processed with the incoming signal sample batch.
     * @param eventCount Number of the processed events.
     */
    abstract protected void addEventsToEventDrawData(@NonNull EventsDrawData eventsDrawData,
        @NonNull String[] eventNames, int eventCount);

    /**
     * Prepares incoming signal FFT data for FFT drawing.
     *
     * @param fftDrawData FFT data prepared for drawing.
     * @param fft Incoming FFT data.
     * @param drawSurfaceWidth Width of the surface FFT data is being drawn to.
     */
    abstract protected void prepareFftForDrawing(@NonNull FftDrawData fftDrawData, @NonNull float[][] fft,
        int drawSurfaceWidth);

    /**
     * Draws previously prepared data onto drawing surface.
     *
     * @param gl Instance of GL10 used for drawing
     * @param samples Incoming samples organized by channels
     * @param selectedChannel Currently selected channel
     * @param signalDrawData Incoming signal prepared for drawing
     * @param eventsDrawData Event markers prepared for drawing
     * @param fftDrawData Incoming signal passed through FFT prepared for drawing
     * @param surfaceWidth Width of the drawing surface
     * @param surfaceHeight Height of the drawing surface
     * @param glWindowWidth Width of the drawing window
     * @param waveformScaleFactors Scale factors that should be used when drawing incoming signal waveform organized by channels
     * @param waveformPositions Vertical positions of the waveform organized by channels
     * @param drawStartIndex Index of the first sample that should be drawn
     * @param drawEndIndex Index of the last sample that should be drawn
     * @param scaleX Scale factor of the drawing window width
     * @param scaleY Scale factor of the drawing window height
     * @param lastFrameIndex Index of the last incoming signal frame that should be drawn (used only during playback)
     */
    abstract protected void draw(GL10 gl, @NonNull short[][] samples, int selectedChannel,
        SignalDrawData signalDrawData, @NonNull EventsDrawData eventsDrawData, @NonNull FftDrawData fftDrawData,
        int surfaceWidth, int surfaceHeight, float glWindowWidth, float[] waveformScaleFactors,
        float[] waveformPositions, int drawStartIndex, int drawEndIndex, float scaleX, float scaleY,
        long lastFrameIndex);

    //
    private void reshape(GL10 gl, float drawnSamplesCount) {
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrthof(0f, drawnSamplesCount - 1, -MAX_GL_VERTICAL_HALF_SIZE, MAX_GL_VERTICAL_HALF_SIZE, -1f, 1f);
    }

    //
    @Nullable private String getAveragingTriggerEventName(String[] eventNames, int eventsCount) {
        if (signalConfiguration.getSignalAveragingTriggerType() == SignalAveragingTriggerType.ALL_EVENTS
            && eventsCount > 0) {
            return eventNames[eventsCount - 1];
        }
        return null;
    }

    //
    private void resetLocalSignalDrawBuffers(int channelCount, int visibleChannelCount) {
        int frameSize = (int) Math.floor((float) SignalProcessor.getProcessedSamplesCount());
        signalDrawBuffer = new MultichannelSignalDrawBuffer(channelCount, frameSize);
        visibleSignalDrawBuffer = new MultichannelSignalDrawBuffer(visibleChannelCount, frameSize);
        frameSize = (int) Math.floor((float) SignalProcessor.getProcessedAveragedSamplesCount());
        averagedSignalDrawBuffer = new MultichannelSignalDrawBuffer(visibleChannelCount, frameSize);
    }

    //
    private void resetLocalSignalDrawData(int visibleChannelCount) {
        int maxSamplesPerChannel = Math.max(surfaceWidth, surfaceHeight) * 8;
        if (signalDrawData == null || signalDrawData.channelCount != visibleChannelCount
            || signalDrawData.maxSamplesPerChannel < maxSamplesPerChannel) {
            signalDrawData = new SignalDrawData(visibleChannelCount, maxSamplesPerChannel);
        }
    }

    //
    private void resetWaveformScaleFactorsAndPositions(int visibleChannelCount) {
        waveformScaleFactors = new float[visibleChannelCount];
        for (int i = 0; i < visibleChannelCount; i++) {
            waveformScaleFactors[i] = waveformScaleFactor;
        }
        waveformPositions = new float[visibleChannelCount];
        float step = MAX_GL_VERTICAL_SIZE / (visibleChannelCount + 1);
        float prev = -MAX_GL_VERTICAL_HALF_SIZE;
        for (int i = 0; i < visibleChannelCount; i++) {
            waveformPositions[i] = prev + step;
            prev += step;
        }
        tempWaveformScaleFactors = new float[visibleChannelCount];
        tempWaveformPositions = new float[visibleChannelCount];
    }

    //==============================================
    //  SCROLLING
    //==============================================

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
     * Triggers {@link OnMeasureListener#onMeasure(float[], int[], int[], int[], int, int)} call.
     */
    final void onMeasure(float[] rms, int[] firstTrainSpikeCount, int[] secondTrainSpikeCount,
        int[] thirdTrainSpikeCount, int selectedChannel, int sampleCount) {
        if (onMeasureListener != null) {
            onMeasureListener.onMeasure(rms, firstTrainSpikeCount, secondTrainSpikeCount, thirdTrainSpikeCount,
                selectedChannel, sampleCount);
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
        return !signalConfiguration.isSignalAveraging();
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
