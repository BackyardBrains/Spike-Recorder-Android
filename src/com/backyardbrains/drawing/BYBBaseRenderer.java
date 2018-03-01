package com.backyardbrains.drawing;

import android.content.Context;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import com.backyardbrains.BaseFragment;
import com.backyardbrains.data.processing.ProcessingBuffer;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.BYBGlUtils;
import com.backyardbrains.utils.BYBUtils;
import com.backyardbrains.utils.PrefUtils;
import com.crashlytics.android.Crashlytics;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
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

    private float focusX;
    private float scaledFocusX;
    private float normalizedFocusX;
    private int focusedSample;

    private boolean bZooming;
    private boolean bPanning;
    private float panningDx;

    private ProcessingBuffer processingBuffer;

    short[] drawingBuffer;
    private float[] tempBufferToDraws;
    String[] markers;

    private int glWindowHorizontalSize = BYBGlUtils.DEFAULT_GL_WINDOW_HORIZONTAL_SIZE;
    private int glWindowVerticalSize = BYBGlUtils.DEFAULT_GL_WINDOW_VERTICAL_SIZE;
    private boolean glWindowSizeDirty;
    private int surfaceWidth;
    private int surfaceHeight;
    private boolean autoScale;
    // Prevents buffer to update while preparing samples for drawing
    private AtomicBoolean processing = new AtomicBoolean(false);

    private static int MAX_SAMPLES_COUNT = AudioUtils.SAMPLE_RATE * SECONDS_TO_RENDER; // 6 sec

    private int minGlWindowHorizontalSize = (int) (AudioUtils.SAMPLE_RATE * .0002); // 0.2 millis
    private float minimumDetectedPCMValue = BYBGlUtils.DEFAULT_MIN_DETECTED_PCM_VALUE;

    private boolean bShowScalingAreaX;
    private int scalingAreaStartX;
    private int scalingAreaEndX;
    private boolean bShowScalingAreaY;
    private int scalingAreaStartY;
    private int scalingAreaEndY;

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
        minGlWindowHorizontalSize = (int) (sampleRate * .0002);
        tempBufferToDraws = initTempBuffer();

        // recalculate GlWindowHorizontalSize
        int newSize = glWindowHorizontalSize;
        if (glWindowHorizontalSize < minGlWindowHorizontalSize) newSize = minGlWindowHorizontalSize;
        if (drawingBuffer != null) {
            final int maxLength = Math.min(drawingBuffer.length, MAX_SAMPLES_COUNT);
            if (maxLength > 0 && newSize > maxLength) newSize = maxLength;
        }
        // save new GL windows width
        glWindowHorizontalSize = newSize;
        // set GL window size dirty so we can recalculate projection
        glWindowSizeDirty = true;
    }

    public void setGlWindowHorizontalSize(int newSize) {
        if (newSize < 0 || newSize == glWindowHorizontalSize) return;

        if (newSize < minGlWindowHorizontalSize) newSize = minGlWindowHorizontalSize;
        if (drawingBuffer != null) {
            final int maxLength = Math.min(drawingBuffer.length, MAX_SAMPLES_COUNT);
            if (maxLength > 0 && newSize > maxLength) newSize = maxLength;
        }
        // save new GL windows width
        glWindowHorizontalSize = newSize;
        // set GL window size dirty so we can recalculate projection
        glWindowSizeDirty = true;
    }

    public int getGlWindowHorizontalSize() {
        return glWindowHorizontalSize;
    }

    public void setGlWindowVerticalSize(int newSize) {
        if (newSize < 0 || newSize == glWindowVerticalSize) return;
        if (newSize < MIN_GL_VERTICAL_SIZE) newSize = MIN_GL_VERTICAL_SIZE;
        if (newSize > PCM_MAXIMUM_VALUE) newSize = PCM_MAXIMUM_VALUE;

        // save new GL windows height
        glWindowVerticalSize = newSize;
        // set GL window size dirty so we can recalculate projection
        glWindowSizeDirty = true;
    }

    public int getGlWindowVerticalSize() {
        return glWindowVerticalSize;
    }

    int getSurfaceWidth() {
        return surfaceWidth;
    }

    int getSurfaceHeight() {
        return surfaceHeight;
    }

    float getScaleX(int glWindowHorizontalSize) {
        return surfaceWidth > 0 ? glWindowHorizontalSize / (float) surfaceWidth : (float) glWindowHorizontalSize;
    }

    float getScaleY(int glWindowVerticalSize) {
        return surfaceHeight > 0 ? glWindowVerticalSize / (float) surfaceHeight : (float) glWindowVerticalSize;
    }

    public void setScaleFocusX(float fx) {
        focusX = fx;
        bZooming = true;
        bPanning = false;
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
        setGlWindowHorizontalSize(PrefUtils.getGlWindowHorizontalSize(context, getClass()));
        setGlWindowVerticalSize(PrefUtils.getGlWindowVerticalSize(context, BYBBaseRenderer.class));
        surfaceWidth = PrefUtils.getViewportWidth(context, getClass());
        surfaceHeight = PrefUtils.getViewportHeight(context, BYBBaseRenderer.class);
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
        PrefUtils.setGlWindowHorizontalSize(context, getClass(), glWindowHorizontalSize);
        PrefUtils.setGlWindowVerticalSize(context, BYBBaseRenderer.class, glWindowVerticalSize);
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
        gl.glDisable(GL10.GL_DITHER);
        gl.glHint(GL10.GL_LINE_SMOOTH_HINT, GL10.GL_NICEST);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
    }

    @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
        LOGD(TAG, "onSurfaceCreated()");
        gl.glViewport(0, 0, width, height);
        prepareForDrawing(gl);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        float heightHalf = glWindowVerticalSize * .5f;
        gl.glOrthof(0, glWindowHorizontalSize, -heightHalf, heightHalf, -1f, 1f);
        gl.glRotatef(0f, 0f, 0f, 1f);

        this.surfaceWidth = width;
        this.surfaceHeight = height;
    }

    @Override public void onDrawFrame(GL10 gl) {
        //long start = System.currentTimeMillis();
        //LOGD(TAG, "START");
        if (!fillBuffer()) {
            LOGD(TAG, "Can't fill audio buffer!");
            return;
        }
        //LOGD(TAG, (System.currentTimeMillis() - start) + " AFTER fillBuffer()");
        if (!BYBUtils.isValidBuffer(drawingBuffer)) {
            LOGD(TAG, "Invalid audio buffer!");
            return;
        }
        //LOGD(TAG, (System.currentTimeMillis() - start) + " AFTER isValidBuffer()");

        if (callback != null) callback.onDraw(glWindowHorizontalSize, glWindowVerticalSize);

        preDrawingHandler();
        //LOGD(TAG, (System.currentTimeMillis() - start) + " AFTER preDrawingHandler()");
        prepareForDrawing(gl);
        //LOGD(TAG, (System.currentTimeMillis() - start) + " AFTER prepareForDrawing()");
        drawingHandler(gl);
        //LOGD(TAG, (System.currentTimeMillis() - start) + " AFTER drawingHandler()");
        postDrawingHandler(gl);
        //LOGD(TAG, (System.currentTimeMillis() - start) + " AFTER postDrawingHandler()");
        //LOGD(TAG, "END");
        //LOGD(TAG, "================================================");
    }

    protected void preDrawingHandler() {
    }

    private void prepareForDrawing(GL10 gl) {
        BYBGlUtils.glClear(gl);
        if (glWindowSizeDirty) {
            float heightHalf = glWindowVerticalSize * .5f;
            gl.glMatrixMode(GL10.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glOrthof(0f, glWindowHorizontalSize, -heightHalf, heightHalf, -1f, 1f);

            glWindowSizeDirty = false;
        }
    }

    abstract protected void drawingHandler(GL10 gl);

    protected void postDrawingHandler(GL10 gl) {
        //if (bShowScalingAreaX || bShowScalingAreaY) {
        //    gl.glEnable(GL10.GL_BLEND);
        //    // Specifies pixel arithmetic
        //    gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        //    if (bShowScalingAreaX) {
        //        BYBGlUtils.drawRectangle(gl, scalingAreaStartX, -getGlWindowVerticalSize(),
        //            scalingAreaEndX - scalingAreaStartX, getGlWindowVerticalSize() * 2, 0xFFFFFF33);
        //    } else {
        //        BYBGlUtils.drawRectangle(gl, 0, scalingAreaStartY, getGlWindowHorizontalSize(), scalingAreaEndY,
        //            0xFFFFFF33);
        //    }
        //    gl.glDisable(GL10.GL_BLEND);
        //}
    }

    //==============================================
    //  PRIVATE AND PACKAGE-PRIVATE METHODS
    //==============================================

    void startAddToGlOffset() {
        if (getIsPlaybackMode() && !getIsPlaying() && !getIsSeeking()) {
            if (callback != null) callback.onHorizontalDragStart();
        }
    }

    void addToGlOffset(float dx, float dy) {
        if (getIsPlaybackMode() && !getIsPlaying()) {
            if (callback != null) callback.onHorizontalDrag(dx * glWindowHorizontalSize / surfaceWidth);

            //bPanning = true;
            //panningDx = dx;
            //bZooming = false;
        }
    }

    void endAddToGlOffset() {
        if (getIsPlaybackMode() && !getIsPlaying()) if (callback != null) callback.onHorizontalDragEnd();
    }

    // Fills buffer with sample data. Returns true if buffer is successfully filled, false otherwise.
    private boolean fillBuffer() {
        if (!processing.get()) {
            drawingBuffer = processingBuffer.getData();
            markers = processingBuffer.getEvents();
        }
        return true;
    }

    @Nullable protected FloatBuffer getWaveformBuffer(short[] sampleBuffer, int glWindowHorizontalSize) {
        return getWaveformBuffer(sampleBuffer, new SparseArray<String>(), glWindowHorizontalSize);
    }

    @Nullable protected FloatBuffer getWaveformBuffer(short[] sampleBuffer, SparseArray<String> markerBuffer,
        int glWindowHorizontalSize) {
        //long start = System.currentTimeMillis();
        //LOGD(TAG, ".........................................");
        //LOGD(TAG, "START - " + shortArrayToDraw.length);

        // start preparing samples for drawing
        processing.set(true);

        boolean clearFront = getIsSeeking();
        //Log.d(TAG, "AFTER setStartEndIndex():" + (System.currentTimeMillis() - start));
        int j = 1;
        int len = sampleBuffer.length;
        int startIndex = Math.max(len - glWindowHorizontalSize, -glWindowHorizontalSize);
        if (startIndex + glWindowHorizontalSize > len) startIndex = len - glWindowHorizontalSize;
        int endIndex = Math.min(startIndex + glWindowHorizontalSize, len);
        String[] markers = Arrays.copyOf(this.markers, this.markers.length);

        try {
            for (int i = startIndex; i < endIndex; i++) {
                if (i < 0) {
                    if (clearFront) tempBufferToDraws[j] = 0;
                } else {
                    tempBufferToDraws[j] = sampleBuffer[i];
                }
                //LOGD(TAG, "currentSample: " + tempBufferToDraws[j] + " - " + (System.currentTimeMillis() - start));

                if (markers[i] != null) markerBuffer.put((int) tempBufferToDraws[j - 1], markers[i]);

                j += 2;
            }
            //LOGD(TAG, "AFTER for loop2:" + (System.currentTimeMillis() - start));
        } catch (ArrayIndexOutOfBoundsException e) {
            LOGE(TAG, "Array size out of sync while building new waveform buffer");
            Crashlytics.logException(e);
        }

        //Log.d(TAG, "AFTER postCycle():" + (System.currentTimeMillis() - start));

        final FloatBuffer fb = BYBUtils.getFloatBufferFromFloatArray(tempBufferToDraws, glWindowHorizontalSize * 2);

        // mark preparing samples for drawing as finished
        processing.set(false);

        //Log.d(TAG, "AFTER getFloatBufferFromFloatArray():" + (System.currentTimeMillis() - start));
        //LOGD(TAG, ".........................................");
        return fb;
    }

    private float getMinimumDetectedPCMValue() {
        return minimumDetectedPCMValue;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- GL
    ////////////////////////////////////////////////////////////////////////////////////////////////

    // ----------------------------------------------------------------------------------------
    private boolean isAutoScale() {
        return autoScale;
    }

    // ----------------------------------------------------------------------------------------
    private void setAutoScale(boolean isScaled) {
        autoScale = isScaled;
    }

    // ----------------------------------------------------------------------------------------
    void autoScaleCheck() {
        if (!isAutoScale() && drawingBuffer != null) {
            if (drawingBuffer.length > 0) {
                autoSetFrame(drawingBuffer);
            }
        }
    }

    // ----------------------------------------------------------------------------------------
    private void autoSetFrame(short[] arrayToScaleTo) {
        //	//Log.d(TAG, "autoSetFrame");
        int theMax = 0;
        int theMin = 0;

        for (int i = 0; i < arrayToScaleTo.length; i++) {
            if (theMax < arrayToScaleTo[i]) theMax = arrayToScaleTo[i];
            if (theMin > arrayToScaleTo[i]) theMin = arrayToScaleTo[i];
        }

        if (theMax != 0 && theMin != 0) {
            final int newyMax;
            if (Math.abs(theMax) >= Math.abs(theMin)) {
                newyMax = Math.abs(theMax) * 2;
            } else {
                newyMax = Math.abs(theMin) * 2;
            }
            if (-newyMax > getMinimumDetectedPCMValue()) {
                setGlWindowVerticalSize(newyMax * 2);
            }
        }
        setAutoScale(true);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- UTILS
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------------------------------------------------------
    int glHeightToPixelHeight(float glHeight) {
        if (surfaceHeight <= 0) LOGD(TAG, "Checked height and size was less than or equal to zero");

        return BYBUtils.map(glHeight, -getGlWindowVerticalSize() / 2, getGlWindowVerticalSize() / 2, surfaceHeight, 0);
    }

    // ----------------------------------------------------------------------------------------
    public float pixelHeightToGlHeight(float pxHeight) {
        return BYBUtils.map(pxHeight, surfaceHeight, 0, -getGlWindowVerticalSize() / 2, getGlWindowVerticalSize() / 2);
    }

    private boolean getIsPlaybackMode() {
        return getAudioService() != null && getAudioService().isPlaybackMode();
    }

    private boolean getIsPlaying() {
        return getAudioService() != null && getAudioService().isAudioPlaying();
    }

    private boolean getIsSeeking() {
        return getAudioService() != null && getAudioService().isAudioSeeking();
    }
}
