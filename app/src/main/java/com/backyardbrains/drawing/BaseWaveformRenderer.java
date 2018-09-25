package com.backyardbrains.drawing;

import android.content.Context;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import com.backyardbrains.BaseFragment;
import com.backyardbrains.data.processing.ProcessingBuffer;
import com.backyardbrains.data.processing.SamplesWithEvents;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.BYBUtils;
import com.backyardbrains.utils.Benchmark;
import com.backyardbrains.utils.EventUtils;
import com.backyardbrains.utils.GlUtils;
import com.backyardbrains.utils.JniUtils;
import com.backyardbrains.utils.PrefUtils;
import com.crashlytics.android.Crashlytics;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.LOGE;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public abstract class BaseWaveformRenderer extends BaseRenderer {

    private static final String TAG = makeLogTag(BaseWaveformRenderer.class);

    private static final int PCM_MAXIMUM_VALUE = Short.MAX_VALUE * 40;
    private static final int MIN_GL_VERTICAL_SIZE = 400;
    private static final float MIN_GL_WINDOW_WIDTH_IN_SECONDS = .0004f;
    private static final float AUTO_SCALE_FACTOR = 1.5f;

    private final ProcessingBuffer processingBuffer;
    private final SparseArray<String> eventsBuffer;

    private DrawBuffer sampleBuffer;
    private short[] samples;
    private DrawBuffer averagedSamplesBuffer;
    private short[] averagedSamples;
    private int[] eventIndices = new int[EventUtils.MAX_EVENT_COUNT];
    private String[] eventNames = new String[EventUtils.MAX_EVENT_COUNT];
    @SuppressWarnings("WeakerAccess") SamplesWithEvents samplesWithEvents;

    private int surfaceWidth;
    private int surfaceHeight;
    private boolean surfaceSizeDirty;
    private int glWindowWidth = GlUtils.DEFAULT_GL_WINDOW_HORIZONTAL_SIZE;
    private int glWindowHeight = GlUtils.DEFAULT_GL_WINDOW_VERTICAL_SIZE;
    private boolean glWindowWidthDirty;
    private boolean glWindowHeightDirty;
    private float scaleX;
    private float scaleY;

    private boolean scrollEnabled;
    private boolean measureEnabled;

    private boolean autoScale;

    private boolean signalAveraging;

    private int sampleRate = AudioUtils.SAMPLE_RATE;
    private float minDetectedPCMValue = GlUtils.DEFAULT_MIN_DETECTED_PCM_VALUE;

    private OnDrawListener onDrawListener;
    private OnScrollListener onScrollListener;
    private OnMeasureListener onMeasureListener;

    /**
     * Interface definition for a callback to be invoked on every surface redraw.
     */
    public interface OnDrawListener {
        /**
         * Listener that is invoked when surface is redrawn.
         *
         * @param drawSurfaceWidth Draw surface width.
         * @param drawSurfaceHeight Draw surface height.
         */
        void onDraw(int drawSurfaceWidth, int drawSurfaceHeight);
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

    public BaseWaveformRenderer(@NonNull BaseFragment fragment) {
        super(fragment);

        processingBuffer = ProcessingBuffer.get();
        eventsBuffer = new SparseArray<>(EventUtils.MAX_EVENT_COUNT);
    }

    /**
     * Cleans any occupied resources.
     */
    public void close() {
    }

    //==============================================
    //  PUBLIC AND PROTECTED METHODS
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
     * Sets current sample rate that should be used when calculating rendering parameters.
     */
    public void setSampleRate(int sampleRate) {
        if (this.sampleRate < 0 || this.sampleRate == sampleRate) return;

        LOGD(TAG, "setSampleRate(" + sampleRate + ")");
        final int minGlWindowWidth = (int) (sampleRate * MIN_GL_WINDOW_WIDTH_IN_SECONDS);
        final int maxGlWindowWidth = processingBuffer.getSize();

        // recalculate width of the GL window
        int newSize = glWindowWidth;
        if (newSize < minGlWindowWidth) newSize = minGlWindowWidth;
        if (newSize > maxGlWindowWidth) newSize = maxGlWindowWidth;
        // save new GL window
        glWindowWidth = newSize;
        // set GL window size dirty so we can recalculate projection
        glWindowWidthDirty = true;

        this.sampleRate = sampleRate;
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
     * Returns whether incoming signal is being averaged or not.
     */
    boolean isSignalAveraging() {
        return signalAveraging;
    }

    /**
     * Resets buffers for averaged samples
     */
    public void resetAveragedSignal() {
        if (processingBuffer != null) {
            averagedSamplesBuffer = new DrawBuffer(processingBuffer.getThresholdBufferSize());
            averagedSamples = new short[processingBuffer.getThresholdBufferSize()];
        }
    }

    public void setGlWindowWidth(int newSize) {
        if (newSize < 0) return;

        final int minGlWindowWidth = (int) (sampleRate * MIN_GL_WINDOW_WIDTH_IN_SECONDS);
        final int maxGlWindowWidth = processingBuffer.getSize();

        if (newSize < minGlWindowWidth) newSize = minGlWindowWidth;
        if (newSize > maxGlWindowWidth) newSize = maxGlWindowWidth;
        // save new GL windows width
        glWindowWidth = newSize;
        // set GL window size dirty so we can recalculate projection
        glWindowWidthDirty = true;
    }

    public int getGlWindowWidth() {
        return glWindowWidth;
    }

    public void setGlWindowHeight(int newSize) {
        if (newSize < 0 || newSize == glWindowHeight) return;
        if (newSize < MIN_GL_VERTICAL_SIZE) newSize = MIN_GL_VERTICAL_SIZE;
        if (newSize > PCM_MAXIMUM_VALUE) newSize = PCM_MAXIMUM_VALUE;

        // save new GL windows height
        glWindowHeight = newSize;
        // set GL window size dirty so we can recalculate projection
        glWindowHeightDirty = true;
    }

    public int getGlWindowHeight() {
        return glWindowHeight;
    }

    @SuppressWarnings("WeakerAccess") public int getSurfaceWidth() {
        return surfaceWidth;
    }

    public int getSurfaceHeight() {
        return surfaceHeight;
    }

    public float pixelHeightToGlHeight(float pxHeight) {
        return BYBUtils.map(pxHeight, surfaceHeight, 0, -glWindowHeight / 2, glWindowHeight / 2);
    }

    int glHeightToPixelHeight(float glHeight) {
        if (surfaceHeight <= 0) LOGD(TAG, "Checked height and size was less than or equal to zero");

        return BYBUtils.map(glHeight, -glWindowHeight / 2, glWindowHeight / 2, surfaceHeight, 0);
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
        setGlWindowHeight(PrefUtils.getGlWindowVerticalSize(context, getClass()));
        //setAutoScaleEnabled(PrefUtils.getAutoScale(context, getClass()));
        minDetectedPCMValue = PrefUtils.getMinimumDetectedPcmValue(context, getClass());
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
        PrefUtils.setGlWindowVerticalSize(context, getClass(), glWindowHeight);
        //PrefUtils.setAutoScale(context, getClass(), autoScale);
        PrefUtils.setMinimumDetectedPcmValue(context, getClass(), minDetectedPCMValue);
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

        int max = Math.max(width, height);
        if (samplesWithEvents == null || samplesWithEvents.samples.length < max) {
            samplesWithEvents = new SamplesWithEvents(max * 5);
        }

        gl.glViewport(0, 0, width, height);
        initDrawSurface(gl, surfaceWidth, glWindowHeight, true);
    }

    private final Benchmark benchmark = new Benchmark("RENDERER_DRAW_TEST").warmUp(500)
        .sessions(10)
        .measuresPerSession(500)
        .logBySession(false)
        .listener(new Benchmark.OnBenchmarkListener() {
            @Override public void onEnd() {
                //EventBus.getDefault().post(new ShowToastEvent("PRESS BACK BUTTON!!!!"));
            }
        });

    /**
     * {@inheritDoc}
     */
    @Override public void onDrawFrame(GL10 gl) {
        //benchmark.start();

        // get event indices and event names from processing buffer
        final int copiedEventsCount = processingBuffer.copyEvents(eventIndices, eventNames);

        // get samples from processing buffer and check if it's valid
        // FIXME: 24-Sep-18 CURRENTLY THE ONLY WAY TO SAVE DATA WHEN SWITCHING BETWEEN PLAYBACK AND THRESHOLD MODE WHILE PAUSING IS TO HAVE TWO DIFFERENT BUFFERS BUT THIS SHOULD BE IMPLEMENTED BETTER
        if (sampleBuffer == null || sampleBuffer.getSize() != processingBuffer.getBufferSize()) {
            sampleBuffer = new DrawBuffer(processingBuffer.getBufferSize());
            samples = new short[processingBuffer.getBufferSize()];
        }
        if (averagedSamplesBuffer == null
            || averagedSamplesBuffer.getSize() != processingBuffer.getThresholdBufferSize()) {
            averagedSamplesBuffer = new DrawBuffer(processingBuffer.getThresholdBufferSize());
            averagedSamples = new short[processingBuffer.getThresholdBufferSize()];
        }
        int count = processingBuffer.get(samples);
        if (count > 0) sampleBuffer.add(samples, count);
        count = processingBuffer.getAveraged(averagedSamples);
        if (count > 0) averagedSamplesBuffer.add(averagedSamples, count);
        // select buffer for drawing
        DrawBuffer tmpSampleBuffer = signalAveraging ? averagedSamplesBuffer : sampleBuffer;

        // auto-scale before drawing if necessary
        if (autoScale) {
            autoScale(tmpSampleBuffer.getArray());
            autoScale = false;
        }

        final boolean surfaceSizeDirty = this.surfaceSizeDirty;
        final int surfaceWidth = this.surfaceWidth;
        final int surfaceHeight = this.surfaceHeight;
        final boolean glWindowWidthDirty = this.glWindowWidthDirty;
        final boolean glWindowHeightDirty = this.glWindowHeightDirty;
        final int glWindowWidth = this.glWindowWidth;
        final int glWindowHeight = this.glWindowHeight;

        // let's reset dirty flags right away
        this.glWindowWidthDirty = false;
        this.glWindowHeightDirty = false;

        final int sampleCount = tmpSampleBuffer.getArray().length;
        final long lastSampleIndex = processingBuffer.getLastSampleIndex();

        // calculate necessary drawing parameters
        int drawStartIndex = Math.max(sampleCount - glWindowWidth, -glWindowWidth);
        if (drawStartIndex + glWindowWidth > sampleCount) drawStartIndex = sampleCount - glWindowWidth;
        final int drawEndIndex = Math.min(drawStartIndex + glWindowWidth, sampleCount);

        // construct waveform vertices and populate eventIndices buffer
        getWaveformVertices(samplesWithEvents, tmpSampleBuffer.getArray(), eventIndices, eventNames, copiedEventsCount,
            eventsBuffer, drawStartIndex, drawEndIndex, surfaceWidth);
        eventsBuffer.clear();
        getEvents(samplesWithEvents, eventNames, copiedEventsCount, eventsBuffer);
        final int samplesDrawCount = (int) (samplesWithEvents.sampleCount * .5);

        // calculate scale x and scale y
        if (surfaceSizeDirty || glWindowWidthDirty) {
            scaleX = samplesDrawCount > 0 ? (float) glWindowWidth / samplesDrawCount : 1f;
        }
        if (surfaceSizeDirty || glWindowHeightDirty) {
            scaleY = surfaceHeight > 0 ? (float) glWindowHeight / surfaceHeight : 1f;
        }

        // init surface before drawing
        initDrawSurface(gl, samplesDrawCount, glWindowHeight, surfaceSizeDirty || glWindowWidthDirty);

        // draw on surface
        draw(gl, tmpSampleBuffer.getArray(), samplesWithEvents.samples, samplesWithEvents.sampleCount, eventsBuffer,
            surfaceWidth, surfaceHeight, glWindowWidth, glWindowHeight, drawStartIndex, drawEndIndex, scaleX, scaleY,
            lastSampleIndex);

        // invoke callback that the surface has been drawn
        if (onDrawListener != null) onDrawListener.onDraw(glWindowWidth, glWindowHeight);

        //benchmark.end();
    }

    private void initDrawSurface(GL10 gl, int samplesDrawCount, int glWindowHeight, boolean updateProjection) {
        GlUtils.glClear(gl);
        if (updateProjection) {
            float heightHalf = glWindowHeight * .5f;
            gl.glMatrixMode(GL10.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glOrthof(0f, samplesDrawCount - 1, -heightHalf, heightHalf, -1f, 1f);
        }
    }

    protected void getWaveformVertices(@NonNull SamplesWithEvents samplesWithEvents, @NonNull short[] samples,
        @NonNull int[] eventIndices, @NonNull String[] eventNames, int eventCount,
        @NonNull SparseArray<String> eventsBuffer, int fromSample, int toSample, int drawSurfaceWidth) {
        //benchmark.start();
        try {
            JniUtils.prepareForDrawing(samplesWithEvents, samples, eventIndices, eventCount, fromSample, toSample,
                drawSurfaceWidth);
        } catch (Exception e) {
            LOGE(TAG, e.getMessage());
            Crashlytics.logException(e);
        }
        //benchmark.end();
    }

    protected void getEvents(@NonNull SamplesWithEvents samplesWithEvents, @NonNull String[] eventNames, int eventCount,
        @NonNull SparseArray<String> eventsBuffer) {
        int indexBase = eventCount - samplesWithEvents.eventCount;
        for (int i = 0; i < samplesWithEvents.eventCount; i++) {
            eventsBuffer.put(samplesWithEvents.eventIndices[i], eventNames[indexBase + i]);
        }
    }

    abstract protected void draw(GL10 gl, @NonNull short[] samples, @NonNull short[] waveformVertices,
        int waveformVerticesCount, @NonNull SparseArray<String> events, int surfaceWidth, int surfaceHeight,
        int glWindowWidth, int glWindowHeight, int drawStartIndex, int drawEndIndex, float scaleX, float scaleY,
        long lastSampleIndex);

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
        autoScale = true;
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
            if (-maxY > minDetectedPCMValue) setGlWindowHeight((int) (maxY * AUTO_SCALE_FACTOR));
        }
    }
}
