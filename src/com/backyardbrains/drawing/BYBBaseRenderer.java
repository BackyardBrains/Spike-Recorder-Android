package com.backyardbrains.drawing;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import com.backyardbrains.BYBConstants;
import com.backyardbrains.BYBGlUtils;
import com.backyardbrains.BYBUtils;
import com.backyardbrains.BaseFragment;
import com.backyardbrains.utls.Formats;
import java.nio.FloatBuffer;
import java.text.DecimalFormat;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utls.LogUtils.LOGD;
import static com.backyardbrains.utls.LogUtils.makeLogTag;

public class BYBBaseRenderer extends BaseRenderer {

    private static final String TAG = makeLogTag(BYBBaseRenderer.class);

    int glWindowHorizontalSize = 4000;
    private int glWindowVerticalSize = 10000;
    private int prevGlWindowHorizontalSize = 4000;
    private int prevGlWindowVerticalSize = 10000;

    private float focusX = 0;
    private float scaledFocusX = 0;
    private float normalizedFocusX = 0;
    private int focusedSample = 0;

    private boolean bZooming = false;
    private boolean bPanning = false;
    private float panningDx = 0;

    short[] mBufferToDraws;
    private float[] mTempBufferToDraws;

    protected int height;
    protected int width;
    private boolean autoScaled = false;
    private static final int PCM_MAXIMUM_VALUE = (Short.MAX_VALUE * 40);
    private static final int MIN_GL_HORIZONTAL_SIZE = 16;
    private static final int MIN_GL_VERTICAL_SIZE = 400;
    private static final int MAX_NUM_SAMPLES = 264600; //6 seconds
    private float minimumDetectedPCMValue = -5000000f;

    private int startIndex = 0;
    private int endIndex = 0;
    private boolean bShowScalingAreaX = false;
    private int scalingAreaStartX;
    private int scalingAreaEndX;
    private boolean bShowScalingAreaY = false;
    private int scalingAreaStartY;
    private int scalingAreaEndY;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- CONSTRUCTOR & SETUP
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public BYBBaseRenderer(@NonNull BaseFragment fragment, @NonNull float[] preparedBuffer) {
        super(fragment);

        this.mTempBufferToDraws = preparedBuffer;
    }

    public static float[] initTempBuffer() {
        final float[] buffer = new float[MAX_NUM_SAMPLES * 2];
        for (int i = 0; i < buffer.length; i += 2) {
            buffer[i] = i / 2;
        }

        return buffer;
    }

    // ----------------------------------------------------------------------------------------
    public void close() {
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- SETTERS/GETTERS
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------------------------------------------------------
    public void setGlWindowHorizontalSize(int newX) {
        if (newX < 0) {
            return;
        }
        prevGlWindowHorizontalSize = glWindowHorizontalSize;
        int maxLength;
        if (mBufferToDraws != null) {
            maxLength = Math.min(mBufferToDraws.length, MAX_NUM_SAMPLES);
            if (newX < MIN_GL_HORIZONTAL_SIZE) {
                newX = MIN_GL_HORIZONTAL_SIZE;
            }
            if (maxLength > 0 && newX > maxLength) {
                newX = maxLength;
            }
            this.glWindowHorizontalSize = newX;
        }
        //LOGD(TAG, "SetGLHorizontalSize " + glWindowHorizontalSize);
    }

    // ----------------------------------------------------------------------------------------
    public void setGlWindowVerticalSize(int newY) {
        //Log.d(TAG, "SetGLVerticalSize newY: "+newY);
        if (newY < 0) {
            return;
        }
        prevGlWindowVerticalSize = glWindowVerticalSize;
        String text = "hsize: " + newY;
        if (newY < MIN_GL_VERTICAL_SIZE) {
            newY = MIN_GL_VERTICAL_SIZE;
        }
        if (newY > PCM_MAXIMUM_VALUE) {
            newY = PCM_MAXIMUM_VALUE;
        }
        glWindowVerticalSize = newY;
    }

    // ----------------------------------------------------------------------------------------
    public int getGlWindowVerticalSize() {
        return glWindowVerticalSize;
    }

    // ----------------------------------------------------------------------------------------
    public int getGlWindowHorizontalSize() {
        return glWindowHorizontalSize;
    }

    // ----------------------------------------------------------------------------------------
    public int getSurfaceWidth() {
        return width;
    }

    // ----------------------------------------------------------------------------------------
    public int getSurfaceHeight() {
        return height;
    }

    // ----------------------------------------------------------------------------------------
    public void addToGlOffset(float dx, float dy) {
        if (getIsPlaybackMode() && !getIsPlaying()) {
            bPanning = true;
            panningDx = dx;
            bZooming = false;
        }
    }

    private void setStartIndex(int si) {
        startIndex = si;
        if (startIndex < 0) startIndex = 0;
        endIndex = startIndex + glWindowHorizontalSize;
    }

    public void setScaleFocusX(float fx) {
        focusX = fx;
        bZooming = true;
        bPanning = false;
    }

    public void showScalingAreaX(float start, float end) {
        bShowScalingAreaX = true;
        scalingAreaStartX = screenToSampleScale(start);
        scalingAreaEndX = screenToSampleScale(end);
    }

    public void showScalingAreaY(float start, float end) {
        bShowScalingAreaY = true;
        scalingAreaStartY = (int) pixelHeightToGlHeight(start);
        scalingAreaEndY = (int) pixelHeightToGlHeight(end);
    }

    public void hideScalingArea() {
        bShowScalingAreaX = false;
        bShowScalingAreaY = false;
    }

    private void setStartEndIndex(int arrayLength) {
        if (getAudioService() != null && getAudioService().isPlaybackMode()) {
            if (getAudioService().isAudioPlayerPlaying()) {
                long playbackProgress = getAudioService().getPlaybackProgress();
                setStartIndex((int) playbackProgress - glWindowHorizontalSize);
            } else {
                if (bZooming) {
                    bZooming = false;
                    normalizedFocusX = focusX / (float) width;
                    scaledFocusX = (float) prevGlWindowHorizontalSize * normalizedFocusX;//(float)width)*focusX;
                    focusedSample = startIndex + (int) Math.floor(scaledFocusX);
                    setStartIndex(
                        startIndex + (int) ((prevGlWindowHorizontalSize - glWindowHorizontalSize) * normalizedFocusX));
                } else if (bPanning) {
                    bPanning = false;
                    setStartIndex(
                        startIndex - (int) Math.floor(((panningDx * glWindowHorizontalSize) / (float) width)));
                }
            }
        } else {
            setStartIndex(arrayLength - glWindowHorizontalSize);
        }
        if (startIndex < -glWindowHorizontalSize) {
            setStartIndex(-glWindowHorizontalSize);
        }
        if (startIndex + getGlWindowHorizontalSize() > arrayLength) {
            setStartIndex(arrayLength - getGlWindowHorizontalSize());
        }
    }

    // ----------------------------------------------------------------------------------------
    @Nullable protected FloatBuffer getWaveformBuffer(short[] shortArrayToDraw) {
        if (getContext() != null) {
            //long start = System.currentTimeMillis();
            //Log.d(TAG, "START - " + shortArrayToDraw.length);
            //float[] arr;
            if (getAudioService() != null) {
                setStartEndIndex(shortArrayToDraw.length);
                //Log.d(TAG, "AFTER setStartEndIndex():" + (System.currentTimeMillis() - start));
                //arr = new float[(glWindowHorizontalSize) * 2];
                int j = 1;
                int shorter = shortArrayToDraw.length;
                if (shorter > endIndex) shorter = endIndex;
                final int s = startIndex < 0 ? 0 : startIndex;
                try {
                    for (int i = s; i < shorter; i++) {
                        mTempBufferToDraws[j] = shortArrayToDraw[i];
                        j += 2;
                    }
                    //Log.d(TAG, "AFTER for loop2:" + (System.currentTimeMillis() - start));
                    //for (int i = startIndex; i < shortArrayToDraw.length && i < endIndex; i++) {
                    //    arr[j++] = i - startIndex;
                    //    if (i < 0) {
                    //        arr[j++] = 0;
                    //    } else {
                    //        arr[j++] = shortArrayToDraw[i];
                    //    }
                    //                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    Log.e(TAG, "Array size out of sync while building new waveform buffer");
                }
            } else {
                mTempBufferToDraws = new float[0];
            }
            final FloatBuffer fb =
                BYBUtils.getFloatBufferFromFloatArray(mTempBufferToDraws, glWindowHorizontalSize * 2);
            //Log.d(TAG, "AFTER getFloatBufferFromFloatArray():" + (System.currentTimeMillis() - start));
            return fb;
        }
        return null;
    }

    // ----------------------------------------------------------------------------------------
    private float getMinimumDetectedPCMValue() {
        return minimumDetectedPCMValue;
    }

    // ----------------------------------------------------------------------------------------
    private boolean getCurrentAudio() {
        if (getAudioService() != null) {
            mBufferToDraws = getAudioService().getAudioBuffer();
            return true;
        }
        return false;
    }

    // ----------------------------------------------------------------------------------------
    private boolean isAutoScaled() {
        return autoScaled;
    }

    // ----------------------------------------------------------------------------------------
    private void setAutoScaled(boolean isScaled) {
        autoScaled = isScaled;
    }

    // ----------------------------------------------------------------------------------------
    // ----------------------------------------- LABELS
    private void setLabels(int samplesToShow) {
        setmVText();
        final float millisecondsInThisWindow = samplesToShow / 44100.0f * 1000 / 2;
        setMsText(millisecondsInThisWindow);
    }

    // ----------------------------------------------------------------------------------------
    protected void setmVText() {
        float yPerDiv = (float) getGlWindowVerticalSize() / 4.0f / 24.5f / 1000 * BYBConstants.millivoltScale;
        setmVText(yPerDiv);
    }

    // ----------------------------------------------------------------------------------------
    private void setMsText(float ms) {
        //String msString = new DecimalFormat("#.#").format(ms);
        broadcastTextUpdate("BYBUpdateMillisecondsReciever", "millisecondsDisplayedString", Formats.formatTime(ms));
    }

    // ----------------------------------------------------------------------------------------
    void setmVText(Float ms) {
        String msString = new DecimalFormat("#.##").format(ms);
        broadcastTextUpdate("BYBUpdateMillivoltReciever", "millivoltsDisplayedString", msString + " mV");
    }

    // ----------------------------------------------------------------------------------------
    private void broadcastTextUpdate(String action, String name, String data) {
        if (getContext() != null) {
            Intent i = new Intent();
            i.setAction(action);
            i.putExtra(name, data);
            getContext().sendBroadcast(i);
        }
    }

    // ----------------------------------------------------------------------------------------
    private void setMillivoltLabelPosition(int height) {
        if (getContext() != null) {
            Intent i = new Intent();
            i.setAction("BYBMillivoltsViewSize");
            i.putExtra("millivoltsViewNewSize", height / 2);
            getContext().sendBroadcast(i);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- DRAWING
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override public void onDrawFrame(GL10 gl) {
        if (!getCurrentAudio()) {
            LOGD(TAG, "Can't get current audio buffer!");
            return;
        }
        if (!BYBUtils.isValidAudioBuffer(mBufferToDraws)) {
            LOGD(TAG, "Invalid audio buffer!");
            return;
        }

        preDrawingHandler();
        BYBGlUtils.glClear(gl);
        drawingHandler(gl);
        postDrawingHandler(gl);
    }

    // ----------------------------------------------------------------------------------------
    void preDrawingHandler() {
        setLabels(glWindowHorizontalSize);
    }

    // ----------------------------------------------------------------------------------------
    void postDrawingHandler(GL10 gl) {
        if (getIsPlaybackMode() && !getIsPlaying()) {
            float playheadDraw = getAudioService().getPlaybackProgress() - startIndex;

            BYBGlUtils.drawGlLine(gl, playheadDraw, -getGlWindowVerticalSize(), playheadDraw, getGlWindowVerticalSize(),
                0x00FFFFFF);
        }
        if (bShowScalingAreaX || bShowScalingAreaY) {
            gl.glEnable(GL10.GL_BLEND);
            // Specifies pixel arithmetic
            gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
            if (bShowScalingAreaX) {
                BYBGlUtils.drawRectangle(gl, scalingAreaStartX, -getGlWindowVerticalSize(),
                    scalingAreaEndX - scalingAreaStartX, getGlWindowVerticalSize() * 2, 0xFFFFFF33);
            } else {
                BYBGlUtils.drawRectangle(gl, 0, scalingAreaStartY, getGlWindowHorizontalSize(), scalingAreaEndY,
                    0xFFFFFF33);
            }
            gl.glDisable(GL10.GL_BLEND);
        }
    }

    // ----------------------------------------------------------------------------------------
    protected void drawingHandler(GL10 gl) {
    }

    // ----------------------------------------------------------------------------------------
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- SURFACE LISTENERS
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
        this.width = width;
        this.height = height;
        setMillivoltLabelPosition(height);
    }

    // ----------------------------------------------------------------------------------------
    @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(TAG, "onSurfaceCreated");
        gl.glDisable(GL10.GL_DITHER);
        gl.glHint(GL10.GL_LINE_SMOOTH_HINT, GL10.GL_NICEST);
        gl.glEnable(GL10.GL_DEPTH_TEST);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- GL
    ////////////////////////////////////////////////////////////////////////////////////////////////
    void initGL(GL10 gl, float xBegin, float xEnd, float scaledYBegin, float scaledYEnd) {

        gl.glViewport(0, 0, width, height);

        BYBGlUtils.glClear(gl);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrthof(xBegin, xEnd, scaledYBegin, scaledYEnd, -1f, 1f);
        gl.glRotatef(0f, 0f, 0f, 1f);

        gl.glClearColor(0f, 0f, 0f, 1.0f);
        gl.glClearDepthf(1.0f);
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glDepthFunc(GL10.GL_LEQUAL);
        gl.glEnable(GL10.GL_LINE_SMOOTH);
        gl.glHint(GL10.GL_LINE_SMOOTH_HINT, GL10.GL_NICEST);
        // Enable Blending
        //		gl.glEnable(GL10.GL_BLEND);
        //		// Specifies pixel arithmetic
        //		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
    }

    // ----------------------------------------------------------------------------------------
    void autoScaleCheck() {
        if (!isAutoScaled() && mBufferToDraws != null) {
            if (mBufferToDraws.length > 0) {
                autoSetFrame(mBufferToDraws);
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

        int newyMax;
        if (theMax != 0 && theMin != 0) {

            if (Math.abs(theMax) >= Math.abs(theMin)) {
                newyMax = Math.abs(theMax) * 2;
            } else {
                newyMax = Math.abs(theMin) * 2;
            }
            if (-newyMax > getMinimumDetectedPCMValue()) {
                setGlWindowVerticalSize(newyMax * 2);
            }
        }
        setAutoScaled(true);
    }

    // ----------------------------------------------------------------------------------------
    protected void setGlWindow(GL10 gl, final int samplesToShow, final int lengthOfSampleSet) {
        initGL(gl, 0, samplesToShow, -getGlWindowVerticalSize() / 2, getGlWindowVerticalSize() / 2);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- UTILS
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------------------------------------------------------
    int glHeightToPixelHeight(float glHeight) {
        if (height <= 0) {
            //Log.d(TAG, "Checked height and size was less than or equal to zero");
        }
        int ret = BYBUtils.map(glHeight, -getGlWindowVerticalSize() / 2, getGlWindowVerticalSize() / 2, height, 0);

        return ret;
    }

    // ----------------------------------------------------------------------------------------
    public float pixelHeightToGlHeight(float pxHeight) {
        return BYBUtils.map(pxHeight, height, 0, -getGlWindowVerticalSize() / 2, getGlWindowVerticalSize() / 2);
    }

    public int screenToSampleScale(float screenPos) {
        float normalizedScreenPos = screenPos / (float) width;
        return (int) (normalizedScreenPos * getGlWindowHorizontalSize());
    }

    public int screenToSamplePos(float screenPos) {
        return startIndex + screenToSampleScale(screenPos);
    }

    public float samplePosToScreen(int samplePos) {
        float normalizedScreenPos = (float) (samplePos - startIndex) / (float) getGlWindowHorizontalSize();
        return normalizedScreenPos * width;
    }

    // ----------------------------------------------------------------------------------------
    protected long msToSamples(long timeSince) {
        return Math.round(44.1 * timeSince);
    }

    private boolean getIsRecording() {
        return getAudioService() != null && getAudioService().isRecording();
    }

    private boolean getIsPlaybackMode() {
        return getAudioService() != null && getAudioService().isPlaybackMode();
    }

    private boolean getIsPlaying() {
        return getAudioService() != null && getAudioService().isAudioPlayerPlaying();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- SETTINGS
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void readSettings(SharedPreferences settings, String TAG) {
        if (settings != null) {
            setGlWindowHorizontalSize(settings.getInt(TAG + "_glWindowHorizontalSize", glWindowHorizontalSize));
            setGlWindowVerticalSize(settings.getInt(TAG + "_glWindowVerticalSize", glWindowVerticalSize));
            height = settings.getInt(TAG + "_height", height);
            width = settings.getInt(TAG + "_width", width);
            setAutoScaled(settings.getBoolean(TAG + "_autoScaled", autoScaled));
            minimumDetectedPCMValue = settings.getFloat(TAG + "_minimumDetectedPCMValue", minimumDetectedPCMValue);
        }
    }

    // ----------------------------------------------------------------------------------------
    public void saveSettings(SharedPreferences settings, String TAG) {
        if (settings != null) {
            settings.edit()
                .putInt(TAG + "_glWindowHorizontalSize", glWindowHorizontalSize)
                .putInt(TAG + "_glWindowVerticalSize", glWindowVerticalSize)
                .putInt(TAG + "_height", height)
                .putInt(TAG + "_width", width)
                .putBoolean(TAG + "_autoScaled", autoScaled)
                .putFloat(TAG + "_minimumDetectedPCMValue", minimumDetectedPCMValue)
                .apply();
        }
    }
}
