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
import com.backyardbrains.utils.BYBGlUtils;
import com.backyardbrains.utils.BYBUtils;
import com.backyardbrains.utils.Benchmark;
import com.backyardbrains.utils.EventUtils;
import com.backyardbrains.utils.JniUtils;
import com.backyardbrains.utils.PrefUtils;
import com.crashlytics.android.Crashlytics;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.LOGE;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public abstract class BYBBaseRenderer extends BaseRenderer {

    private static final String TAG = makeLogTag(BYBBaseRenderer.class);

    private static final int PCM_MAXIMUM_VALUE = Short.MAX_VALUE * 40;
    private static final int MIN_GL_VERTICAL_SIZE = 400;

    private final ProcessingBuffer processingBuffer;
    private final SparseArray<String> eventsBuffer;

    private short[] samples;
    private int[] eventIndices = new int[EventUtils.MAX_EVENT_COUNT];
    private String[] eventNames = new String[EventUtils.MAX_EVENT_COUNT];
    @SuppressWarnings("WeakerAccess") SamplesWithEvents samplesWithEvents;

    private int surfaceWidth;
    private int surfaceHeight;
    private boolean surfaceSizeDirty;
    private int glWindowWidth = BYBGlUtils.DEFAULT_GL_WINDOW_HORIZONTAL_SIZE;
    private int glWindowHeight = BYBGlUtils.DEFAULT_GL_WINDOW_VERTICAL_SIZE;
    private boolean glWindowWidthDirty;
    private boolean glWindowHeightDirty;
    private float scaleX;
    private float scaleY;

    private boolean scrollEnabled;
    private boolean measureEnabled;

    private boolean autoScale;

    private int minGlWindowWidth = (int) (AudioUtils.SAMPLE_RATE * .0004); // 0.2 millis
    private float minDetectedPCMValue = BYBGlUtils.DEFAULT_MIN_DETECTED_PCM_VALUE;

    private OnDrawListener onDrawListener;
    private OnScrollListener onScrollListener;
    private OnMeasureListener onMeasureListener;

    private final Benchmark benchmark;

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

    public BYBBaseRenderer(@NonNull BaseFragment fragment) {
        super(fragment);

        processingBuffer = ProcessingBuffer.get();
        eventsBuffer = new SparseArray<>(EventUtils.MAX_EVENT_COUNT);

        benchmark = new Benchmark("RENDERER_DRAW_TEST").warmUp(1000)
            .sessions(10)
            .measuresPerSession(1000)
            .logBySession(false)
            .logToFile(true)
            .listener(new Benchmark.OnBenchmarkListener() {
                @Override public void onEnd() {
                    //EventBus.getDefault().post(new ShowToastEvent("PRESS BACK BUTTON!!!!"));
                }
            });
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
        LOGD(TAG, "setSampleRate(" + sampleRate + ")");
        minGlWindowWidth = (int) (sampleRate * .0004);

        // recalculate width of the GL window
        int newSize = glWindowWidth;
        if (newSize < minGlWindowWidth) newSize = minGlWindowWidth;
        if (newSize > processingBuffer.getBufferSize()) newSize = processingBuffer.getBufferSize();
        // save new GL window
        glWindowWidth = newSize;
        // set GL window size dirty so we can recalculate projection
        glWindowWidthDirty = true;
    }

    public void setGlWindowWidth(int newSize) {
        if (newSize < 0 || newSize == glWindowWidth) return;

        if (newSize < minGlWindowWidth) newSize = minGlWindowWidth;
        if (newSize > processingBuffer.getBufferSize()) newSize = processingBuffer.getBufferSize();
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

    int getSurfaceWidth() {
        return surfaceWidth;
    }

    @SuppressWarnings("unused") int getSurfaceHeight() {
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
        setGlWindowWidth(PrefUtils.getGlWindowHorizontalSize(context, getClass()));
        setGlWindowHeight(PrefUtils.getGlWindowVerticalSize(context, BYBBaseRenderer.class));
        surfaceWidth = PrefUtils.getViewportWidth(context, getClass());
        surfaceHeight = PrefUtils.getViewportHeight(context, BYBBaseRenderer.class);
        surfaceSizeDirty = true;
        setAutoScale(PrefUtils.getAutoScale(context, getClass()));
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
        PrefUtils.setGlWindowHorizontalSize(context, getClass(), glWindowWidth);
        PrefUtils.setGlWindowVerticalSize(context, BYBBaseRenderer.class, glWindowHeight);
        PrefUtils.setViewportWidth(context, getClass(), surfaceWidth);
        PrefUtils.setViewportHeight(context, BYBBaseRenderer.class, surfaceHeight);
        PrefUtils.setAutoScale(context, getClass(), autoScale);
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
        //if (envelopedSamples == null || envelopedSamples.length < max) envelopedSamples = new short[max * 5];

        gl.glViewport(0, 0, width, height);
        initDrawSurface(gl, surfaceWidth, glWindowHeight, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override public void onDrawFrame(GL10 gl) {
        //long start = System.currentTimeMillis();

        //benchmark.start();

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

        // get event indices and event names from processing buffer
        final int copiedEventsCount = processingBuffer.copyEvents(eventIndices, eventNames);

        // get samples from processing buffer and check if it's valid
        if (samples == null || samples.length != processingBuffer.getData().length) {
            samples = new short[processingBuffer.getData().length];
        }
        System.arraycopy(processingBuffer.getData(), 0, samples, 0, samples.length);
        // check if we have a valid buffer after filling it
        if (samples.length <= 0) return;

        // samples are OK, we can move on

        final int sampleCount = samples.length;
        final long lastSampleIndex = processingBuffer.getLastSampleIndex();

        // calculate necessary drawing parameters
        int drawStartIndex = Math.max(sampleCount - glWindowWidth, -glWindowWidth);
        if (drawStartIndex + glWindowWidth > sampleCount) drawStartIndex = sampleCount - glWindowWidth;
        final int drawEndIndex = Math.min(drawStartIndex + glWindowWidth, sampleCount);

        // construct waveform vertices and populate eventIndices buffer
        eventsBuffer.clear();
        getWaveformVertices(samplesWithEvents, samples, eventIndices, eventNames, copiedEventsCount, drawStartIndex,
            drawEndIndex, surfaceWidth);
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

        //autoScaleCheck(samples);

        // draw on surface
        draw(gl, samples, samplesWithEvents.samples, samplesWithEvents.sampleCount, eventsBuffer, surfaceWidth,
            surfaceHeight, glWindowWidth, glWindowHeight, drawStartIndex, drawEndIndex, scaleX, scaleY,
            lastSampleIndex);

        // invoke callback that the surface has been drawn
        if (onDrawListener != null) onDrawListener.onDraw(glWindowWidth, glWindowHeight);

        //benchmark.end();

        //LOGD(TAG, "" + (System.currentTimeMillis() - start));
        //LOGD(TAG, "================================================");
    }

    private void initDrawSurface(GL10 gl, int samplesDrawCount, int glWindowHeight, boolean updateProjection) {
        BYBGlUtils.glClear(gl);
        if (updateProjection) {
            float heightHalf = glWindowHeight * .5f;
            gl.glMatrixMode(GL10.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glOrthof(0f, samplesDrawCount - 1, -heightHalf, heightHalf, -1f, 1f);
        }
    }

    protected void getWaveformVertices(@NonNull SamplesWithEvents samplesWithEvents, @NonNull short[] samples,
        @NonNull int[] eventIndices, @NonNull String[] eventNames, int eventCount, int fromSample, int toSample,
        int drawSurfaceWidth) {
        //long start = System.currentTimeMillis();
        //LOGD(TAG, ".........................................");

        try {
            JniUtils.prepareForDrawing(samplesWithEvents, samples, eventIndices, eventCount, fromSample, toSample,
                drawSurfaceWidth);
            int indexBase = eventCount - samplesWithEvents.eventCount;
            for (int i = 0; i < samplesWithEvents.eventCount; i++) {
                eventsBuffer.put(samplesWithEvents.eventIndices[i], eventNames[indexBase + i]);
            }
        } catch (Exception e) {
            LOGE(TAG, e.getMessage());
            Crashlytics.logException(e);
        }

        //LOGD(TAG, "END: " + (System.currentTimeMillis() - start));
    }

    abstract protected void draw(GL10 gl, @NonNull short[] samples, @NonNull short[] waveformVertices,
        int waveformVerticesCount, @NonNull SparseArray<String> markers, int surfaceWidth, int surfaceHeight,
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
     * Whether scrolling of the surface view is enabled.
     */
    public boolean isScrollEnabled() {
        return this.scrollEnabled;
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
     * Whether measurement of the signal is enabled.
     */
    public boolean isMeasureEnabled() {
        return measureEnabled;
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
     * Sets whether measurement of the signal will be enabled.
     */
    void setMeasureEnabled(boolean enabled) {
        this.measureEnabled = enabled;
    }

    //==============================================
    //  AUTO-SCALE
    //==============================================

    @SuppressWarnings("unused") private void autoScaleCheck(@NonNull short[] samples) {
        if (!isAutoScale()) if (samples.length > 0) autoSetFrame(samples);
    }

    private boolean isAutoScale() {
        return autoScale;
    }

    private void setAutoScale(boolean isScaled) {
        autoScale = isScaled;
    }

    private void autoSetFrame(short[] arrayToScaleTo) {
        int theMax = 0;
        int theMin = 0;

        for (short anArrayToScaleTo : arrayToScaleTo) {
            if (theMax < anArrayToScaleTo) theMax = anArrayToScaleTo;
            if (theMin > anArrayToScaleTo) theMin = anArrayToScaleTo;
        }

        if (theMax != 0 && theMin != 0) {
            final int newyMax;
            if (Math.abs(theMax) >= Math.abs(theMin)) {
                newyMax = Math.abs(theMax) * 2;
            } else {
                newyMax = Math.abs(theMin) * 2;
            }
            if (-newyMax > getMinDetectedPCMValue()) {
                setGlWindowHeight(newyMax * 2);
            }
        }
        setAutoScale(true);
    }

    private float getMinDetectedPCMValue() {
        return minDetectedPCMValue;
    }
}
