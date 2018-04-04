package com.backyardbrains.drawing;

import android.content.Context;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.util.SparseArray;
import com.backyardbrains.BaseFragment;
import com.backyardbrains.data.processing.ProcessingBuffer;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.BYBGlUtils;
import com.backyardbrains.utils.BYBUtils;
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
    private static final int SECONDS_TO_RENDER = 12;

    private static int MAX_SAMPLES_COUNT = AudioUtils.SAMPLE_RATE * SECONDS_TO_RENDER; // 12 sec

    private ProcessingBuffer processingBuffer;

    private float[] tempBufferToDraws;
    private short[] samples;
    private String[] markers;

    private int surfaceWidth;
    private int surfaceHeight;
    private boolean surfaceSizeDirty;
    private int glWindowWidth = BYBGlUtils.DEFAULT_GL_WINDOW_HORIZONTAL_SIZE;
    private int glWindowHeight = BYBGlUtils.DEFAULT_GL_WINDOW_VERTICAL_SIZE;
    private boolean glWindowWidthDirty;
    private boolean glWindowHeightDirty;
    private float scaleX;
    private float scaleY;

    private boolean autoScale;

    private int minGlWindowWidth = (int) (AudioUtils.SAMPLE_RATE * .0004); // 0.2 millis
    private float minimumDetectedPCMValue = BYBGlUtils.DEFAULT_MIN_DETECTED_PCM_VALUE;

    private Callback callback;

    public interface Callback {

        void onDraw(int drawSurfaceWidth, int drawSurfaceHeight);

        void onHorizontalDragStart();

        void onHorizontalDrag(float dx);

        void onHorizontalDragEnd();
    }

    public static class CallbackAdapter implements Callback {

        @Override public void onDraw(int drawSurfaceWidth, int drawSurfaceHeight) {
        }

        @Override public void onHorizontalDrag(float dx) {
        }

        @Override public void onHorizontalDragStart() {
        }

        @Override public void onHorizontalDragEnd() {
        }
    }

    //==============================================
    //  CONSTRUCTOR & SETUP
    //==============================================

    public BYBBaseRenderer(@NonNull BaseFragment fragment, @NonNull float[] preparedBuffer) {
        super(fragment);

        processingBuffer = ProcessingBuffer.get();

        this.tempBufferToDraws = preparedBuffer;
    }

    public static float[] initTempBuffer() {
        final float[] buffer = new float[MAX_SAMPLES_COUNT * 2];
        for (int i = 0; i < buffer.length; i += 2) {
            buffer[i] = i / 2;
        }

        return buffer;
    }

    /**
     * Cleans any occupied resources.
     */
    public void close() {
    }

    //==============================================
    //  PUBLIC AND PROTECTED METHODS
    //==============================================

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    /**
     * Sets current sample rate that should be used when calculating rendering parameters.
     */
    public void setSampleRate(int sampleRate) {
        LOGD(TAG, "setSampleRate(" + sampleRate + ")");
        MAX_SAMPLES_COUNT = sampleRate * SECONDS_TO_RENDER;
        minGlWindowWidth = (int) (sampleRate * .0004);
        tempBufferToDraws = initTempBuffer();

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

    int getSurfaceHeight() {
        return surfaceHeight;
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
        minimumDetectedPCMValue = PrefUtils.getMinimumDetectedPcmValue(context, getClass());
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
        PrefUtils.setMinimumDetectedPcmValue(context, getClass(), minimumDetectedPCMValue);
    }

    //==============================================
    //  Renderer INTERFACE IMPLEMENTATIONS
    //==============================================

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

    @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
        LOGD(TAG, "onSurfaceCreated()");
        gl.glViewport(0, 0, width, height);
        initDrawSurface(gl, glWindowWidth, glWindowHeight, true);

        // save new surface width and height
        surfaceWidth = width;
        surfaceHeight = height;
        // set surface size dirty so we can recalculate scale
        surfaceSizeDirty = true;
    }

    @Override public void onDrawFrame(GL10 gl) {
        //long start = System.currentTimeMillis();

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

        // get samples from processing buffer and check if it's valid
        if (samples == null || samples.length != processingBuffer.getData().length) {
            samples = new short[processingBuffer.getData().length];
        }
        System.arraycopy(processingBuffer.getData(), 0, samples, 0, samples.length);
        // check if we have a valid buffer after filling it
        if (samples.length <= 0) return;

        // samples are OK, we can move on

        // get markers from processing buffer and check if it's valid
        if (markers == null || markers.length != processingBuffer.getEvents().length) {
            markers = new String[processingBuffer.getEvents().length];
        }
        System.arraycopy(processingBuffer.getEvents(), 0, markers, 0, markers.length);

        if (surfaceSizeDirty || glWindowWidthDirty) {
            scaleX = surfaceWidth > 0 ? glWindowWidth / (float) surfaceWidth : (float) glWindowWidth;
        }
        if (surfaceSizeDirty || glWindowHeightDirty) {
            scaleY = surfaceHeight > 0 ? glWindowHeight / (float) surfaceHeight : (float) glWindowHeight;
        }

        final int sampleCount = samples.length;

        // calculate necessary drawing parameters
        int drawStartIndex = Math.max(sampleCount - glWindowWidth, -glWindowWidth);
        if (drawStartIndex + glWindowWidth > sampleCount) drawStartIndex = sampleCount - glWindowWidth;
        final int drawEndIndex = Math.min(drawStartIndex + glWindowWidth, sampleCount);

        // construct waveform vertices and populate markers buffer
        final SparseArray<String> markersBuffer = new SparseArray<>();
        final float[] waveformVertices =
            getWaveformVertices(samples, markers, markersBuffer, glWindowWidth, drawStartIndex, drawEndIndex);

        // init surface before drawing
        initDrawSurface(gl, glWindowWidth, glWindowHeight, glWindowWidthDirty || glWindowHeightDirty);

        //autoScaleCheck(samples);

        // draw on surface
        draw(gl, samples, waveformVertices, markersBuffer, surfaceWidth, surfaceHeight, glWindowWidth, glWindowHeight,
            drawStartIndex, drawEndIndex, scaleX, scaleY);

        // invoke callback that the surface has been drawn
        if (callback != null) callback.onDraw(glWindowWidth, glWindowHeight);

        //LOGD(TAG, "" + (System.currentTimeMillis() - start));
        //LOGD(TAG, "================================================");
    }

    private void initDrawSurface(GL10 gl, int glWindowWidth, int glWindowHeight, boolean updateProjection) {
        BYBGlUtils.glClear(gl);
        if (updateProjection) {
            float heightHalf = glWindowHeight * .5f;
            gl.glMatrixMode(GL10.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glOrthof(0f, glWindowWidth - 1, -heightHalf, heightHalf, -1f, 1f);
        }
    }

    abstract protected void draw(GL10 gl, @NonNull short[] samples, @NonNull float[] waveformVertices,
        @NonNull SparseArray<String> markers, int surfaceWidth, int surfaceHeight, int glWindowWidth,
        int glWindowHeight, int drawStartIndex, int drawEndIndex, float scaleX, float scaleY);

    protected void onMeasurementStart(float x) {
    }

    protected void onMeasure(float x) {
    }

    protected void onMeasurementEnd(float x) {
    }

    //==============================================
    //  PRIVATE AND PACKAGE-PRIVATE METHODS
    //==============================================

    void startScroll() {
        if (getIsPlaybackMode() && getIsNotPlaying() && !getIsSeeking()) {
            if (callback != null) callback.onHorizontalDragStart();
        }
    }

    void scroll(float dx) {
        if (getIsPlaybackMode() && getIsNotPlaying()) {
            if (callback != null) callback.onHorizontalDrag(dx * glWindowWidth / surfaceWidth);
        }
    }

    void endScroll() {
        if (getIsPlaybackMode() && getIsNotPlaying()) if (callback != null) callback.onHorizontalDragEnd();
    }

    void startMeasurements(float x) {
        if (getIsPlaybackMode() && getIsNotPlaying()) onMeasurementStart(x);
    }

    void measure(float x) {
        if (getIsPlaybackMode() && getIsNotPlaying()) onMeasure(x);
    }

    void endMeasurements(float x) {
        if (getIsPlaybackMode() && getIsNotPlaying()) onMeasurementEnd(x);
    }

    @NonNull protected float[] getWaveformVertices(@NonNull short[] samples, @NonNull String[] markers,
        @NonNull SparseArray<String> markerBuffer, int glWindowWidth, int drawStartIndex, int drawEndIndex) {
        //long start = System.currentTimeMillis();
        //LOGD(TAG, ".........................................");
        //LOGD(TAG, "START - " + samples.length);

        try {
            //boolean clearFront = getIsSeeking();
            int j = 1;
            for (int i = drawStartIndex; i < drawEndIndex; i++) {
                //if (i < 0) {
                //    if (clearFront) tempBufferToDraws[j] = 0;
                //} else {
                tempBufferToDraws[j] = samples[i];
                //}
                if (markers[i] != null) markerBuffer.put((int) tempBufferToDraws[j - 1], markers[i]);

                j += 2;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            LOGE(TAG, "Array size out of sync while building new waveform buffer");
            Crashlytics.logException(e);
        }

        //LOGD(TAG, "END: " + (System.currentTimeMillis() - start));

        return tempBufferToDraws;
    }

    //==============================================
    //  AUTO-SCALE
    //==============================================

    private void autoScaleCheck(@NonNull short[] samples) {
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
            if (-newyMax > getMinimumDetectedPCMValue()) {
                setGlWindowHeight(newyMax * 2);
            }
        }
        setAutoScale(true);
    }

    private float getMinimumDetectedPCMValue() {
        return minimumDetectedPCMValue;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- UTILS
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------------------------------------------------------
    int glHeightToPixelHeight(float glHeight) {
        if (surfaceHeight <= 0) LOGD(TAG, "Checked height and size was less than or equal to zero");

        return BYBUtils.map(glHeight, -getGlWindowHeight() / 2, getGlWindowHeight() / 2, surfaceHeight, 0);
    }

    // ----------------------------------------------------------------------------------------
    public float pixelHeightToGlHeight(float pxHeight) {
        return BYBUtils.map(pxHeight, surfaceHeight, 0, -getGlWindowHeight() / 2, getGlWindowHeight() / 2);
    }

    private boolean getIsPlaybackMode() {
        return getAudioService() != null && getAudioService().isPlaybackMode();
    }

    private boolean getIsNotPlaying() {
        return getAudioService() == null || !getAudioService().isAudioPlaying();
    }

    private boolean getIsSeeking() {
        return getAudioService() != null && getAudioService().isAudioSeeking();
    }
}
