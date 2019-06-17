/*
 * Backyard Brains Android App
 * Copyright (C) 2011 Backyard Brains
 * by Nathan Dotz <nate (at) backyardbrains.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.backyardbrains.drawing;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.view.MotionEvent;
import com.backyardbrains.drawing.gl.GlAveragingTriggerLine;
import com.backyardbrains.drawing.gl.GlDashedHLine;
import com.backyardbrains.drawing.gl.GlEventMarker;
import com.backyardbrains.drawing.gl.GlFft;
import com.backyardbrains.drawing.gl.GlHLine;
import com.backyardbrains.drawing.gl.GlHandle;
import com.backyardbrains.drawing.gl.GlHandleDragHelper;
import com.backyardbrains.drawing.gl.GlLabel;
import com.backyardbrains.drawing.gl.GlWaveform;
import com.backyardbrains.drawing.gl.Rect;
import com.backyardbrains.ui.BaseFragment;
import com.backyardbrains.utils.ArrayUtils;
import com.backyardbrains.utils.Formats;
import com.backyardbrains.utils.JniUtils;
import com.backyardbrains.utils.PrefUtils;
import com.backyardbrains.utils.ViewUtils;
import com.crashlytics.android.Crashlytics;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.LOGE;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class WaveformRenderer extends BaseWaveformRenderer {

    private static final String TAG = makeLogTag(WaveformRenderer.class);

    private static final float MAX_GL_VERTICAL_SIXTH_SIZE = MAX_GL_VERTICAL_SIZE / 6f;
    // Size of the bottom half of the screen (below zero) when drawing waveform alongside FFT
    private static final float MAX_GL_FFT_VERTICAL_HALF_SIZE = MAX_GL_VERTICAL_HALF_SIZE * 4f;
    // Height of the draw surface occupied by the FFT should be 60%
    private static final float FFT_HEIGHT_PERCENT = .6f; // 60%
    private static final float MIN_FFT_SCALE_FACTOR = 1f;
    private static final float MAX_FFT_SCALE_FACTOR = 30f;

    private static final float DASH_SIZE = 30f;
    private static final int LINE_WIDTH = 1;
    private static final float MARKER_LABEL_TOP = 230f;
    private static final float MARKER_LABEL_TOP_OFFSET = 20f;
    // Width of the time label in DPs
    private final static float TIME_LABEL_WIDTH_DP = 80f;
    // Height of the time label in DPs
    private final static float TIME_LABEL_HEIGHT_DP = 48f;
    // Radius of the handle base
    private static final float HANDLE_BASE_RADIUS_DP = 10f;

    private final Rect rect = new Rect();
    private final GlHandleDragHelper waveformHandleDragHelper;
    private final GlHandleDragHelper thresholdHandleDragHelper;

    private Context context;

    private final GlWaveform glWaveform;
    private final GlHandle glHandle;
    private final GlDashedHLine glThresholdLine;
    private GlFft glFft;
    private GlEventMarker glEventMarker;
    private GlAveragingTriggerLine glAveragingTrigger;
    private GlHLine glTimeLabelSeparator;
    private GlLabel glTimeLabel;

    private final float timeLabelWidth;
    private final float timeLabelHeight;
    private final int timeLabelSeparatorWidth;
    private final float handleBaseRadius;

    private float threshold;
    private String lastTriggerEventName;
    private float[][] channelColors = new float[][] { Colors.CHANNEL_0.clone() };
    private float fftSurfaceHeight;
    private float fftScaleFactor = MIN_FFT_SCALE_FACTOR;

    /**
     * Interface definition for a callback to be invoked when one of the drawn waveforms is selected by clicking he
     * waveform handle.
     */
    public interface OnWaveformSelectionListener {
        /**
         * Listener that is invoked when waveform is selected.
         *
         * @param index Index of the selected waveform.
         */
        void onWaveformSelected(int index);
    }

    private OnWaveformSelectionListener listener;

    public WaveformRenderer(@NonNull BaseFragment fragment) {
        super(fragment);

        context = fragment.getContext();

        waveformHandleDragHelper = new GlHandleDragHelper(new GlHandleDragHelper.OnDragListener() {
            @Override public void onDragStart(int index) {
                selectWaveform(index);
            }

            @Override public void onDrag(int index, float dy) {
                moveGlWindowForSelectedChannel(dy);
            }

            @Override public void onDragStop(int index) {
                // ignore
            }
        });
        thresholdHandleDragHelper = new GlHandleDragHelper(new GlHandleDragHelper.OnDragListener() {
            @Override public void onDragStart(int index) {
                // ignore
            }

            @Override public void onDrag(int index, float dy) {
                updateThreshold(dy);
            }

            @Override public void onDragStop(int index) {
                // ignore
            }
        });

        glWaveform = new GlWaveform();
        glHandle = new GlHandle();
        glThresholdLine = new GlDashedHLine();
        glTimeLabelSeparator = new GlHLine();

        timeLabelWidth = ViewUtils.dpToPx(context.getResources(), TIME_LABEL_WIDTH_DP);
        timeLabelHeight = ViewUtils.dpToPx(context.getResources(), TIME_LABEL_HEIGHT_DP);
        timeLabelSeparatorWidth = ViewUtils.dpToPxInt(context.getResources(), LINE_WIDTH);
        handleBaseRadius = ViewUtils.dpToPx(context.getResources(), HANDLE_BASE_RADIUS_DP);
    }

    //=================================================
    //  PUBLIC AND PROTECTED METHODS
    //=================================================

    /**
     * Registers a callback to be invoked when one of waveforms is selected.
     *
     * @param listener The callback that will be run. This value may be {@code null}.
     */
    public void setOnWaveformSelectionListener(@Nullable OnWaveformSelectionListener listener) {
        this.listener = listener;
    }

    /**
     * Returns color of all the channels.
     */
    public float[][] getChannelColors() {
        return ArrayUtils.copy(channelColors);
    }

    /**
     * Sets specified {@code color} for the channel at specified {@code channelIndex}.
     */
    public void setChannelColor(int channelIndex, @Size(4) float[] color) {
        System.arraycopy(color, 0, channelColors[channelIndex], 0, color.length);
    }

    /**
     * Sets signal threshold value so threshold line and handle can be drawn.
     */
    protected void setThreshold(float threshold) {
        if (threshold == 0) return;

        this.threshold = threshold;

        // pass new threshold to the c++ code
        JniUtils.setThreshold(threshold);
    }

    //=================================================
    //  Renderer INTERFACE IMPLEMENTATIONS
    //=================================================

    /**
     * {@inheritDoc}
     */
    @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        super.onSurfaceCreated(gl, config);

        glEventMarker = new GlEventMarker(context, gl);
        glFft = new GlFft(context, gl);
        glAveragingTrigger = new GlAveragingTriggerLine(context, gl);
        glTimeLabel = new GlLabel(context, gl);
    }

    /**
     * {@inheritDoc}
     */
    @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
        super.onSurfaceChanged(gl, width, height);

        waveformHandleDragHelper.resetDraggableAreas();
        waveformHandleDragHelper.setSurfaceHeight(height);

        thresholdHandleDragHelper.resetDraggableAreas();
        thresholdHandleDragHelper.setSurfaceHeight(height);

        fftSurfaceHeight = height * FFT_HEIGHT_PERCENT;
    }

    //=================================================
    //  TouchEnabledRenderer INTERFACE IMPLEMENTATIONS
    //=================================================

    /**
     * {@inheritDoc}
     */
    @Override public boolean onTouchEvent(MotionEvent event) {
        return waveformHandleDragHelper.onTouch(event) || thresholdHandleDragHelper.onTouch(event);
    }

    //=================================================
    //  ZoomEnabledRenderer INTERFACE IMPLEMENTATIONS
    //=================================================

    /**
     * {@inheritDoc}
     */
    @Override public void onVerticalZoom(float zoomFactor, float zoomFocusX, float zoomFocusY) {
        // depending on the zoom focus point we zoom either waveform or fft spectrogram
        if (isFftProcessing() && getSurfaceHeight() - zoomFocusY < fftSurfaceHeight) {
            setFftScaleFactor(zoomFactor);
        } else {
            super.onVerticalZoom(zoomFactor, zoomFocusX, zoomFocusY);
        }
    }

    //=================================================
    //  BaseWaveformRenderer OVERRIDES
    //=================================================

    /**
     * {@inheritDoc}
     *
     * @param x X coordinate of the tap point.
     * @param y Y coordinate of the tap point.
     */
    @Override void autoScale(float x, float y) {
        // auto scale only if user tapped within waveform area
        if (!isFftProcessing() || getSurfaceHeight() - y > fftSurfaceHeight) {
            super.autoScale(x, y);
        }
        // TODO: 11-Jun-19 TBD WHETHER WE WANT RESET OF FFT NORMALIZATION PARAMETERS ON SPECTROGRAM DOUBLE-CLICK
        /* else {
            JniUtils.resetFftNormalization();
        }*/
    }

    /**
     * {@inheritDoc}
     *
     * @param channelCount The new number of channels.
     */
    @Override public void onChannelCountChanged(int channelCount) {
        super.onChannelCountChanged(channelCount);

        fftScaleFactor = MIN_FFT_SCALE_FACTOR;

        channelColors = new float[channelCount][];
        for (int i = 0; i < channelCount; i++) {
            channelColors[i] = new float[4];
            setChannelColor(i, Colors.CHANNEL_COLORS[i % Colors.CHANNEL_COLORS.length]);
        }

        // we should reset draggable areas for both waveforms and thresholds
        waveformHandleDragHelper.resetDraggableAreas();
        thresholdHandleDragHelper.resetDraggableAreas();
    }

    @Override public void onChannelConfigChanged(boolean[] channelConfig) {
        super.onChannelConfigChanged(channelConfig);

        fftScaleFactor = MIN_FFT_SCALE_FACTOR;

        for (int i = 0; i < channelConfig.length; i++) {
            if (!channelConfig[i]) setChannelColor(i, Colors.BLACK);
        }
    }

    @Override public void onChannelSelectionChanged(int channelIndex) {
        super.onChannelSelectionChanged(channelIndex);

        thresholdHandleDragHelper.resetDraggableAreas();
    }

    @Override public void onSignalAveragingChanged(boolean signalAveraging) {
        super.onSignalAveragingChanged(signalAveraging);

        lastTriggerEventName = null;
    }

    @Override public void onSignalAveragingTriggerTypeChanged(int triggerType) {
        super.onSignalAveragingTriggerTypeChanged(triggerType);

        lastTriggerEventName = null;
    }

    @Override public void onFftProcessingChanged(boolean fftProcessing) {
        super.onFftProcessingChanged(fftProcessing);
    }

    /**
     * {@inheritDoc}
     */
    @Override public void onLoadSettings(@NonNull Context context, String from) {
        super.onLoadSettings(context, from);

        setThreshold(PrefUtils.getThreshold(context, getClass()));
    }

    /**
     * {@inheritDoc}
     */
    @Override public void onSaveSettings(@NonNull Context context, String from) {
        super.onSaveSettings(context, from);

        PrefUtils.setThreshold(context, getClass(), threshold);
    }

    /**
     * {@inheritDoc}
     */
    @Override protected void prepareSignalForDrawing(@NonNull SignalDrawData signalDrawData,
        @NonNull EventsDrawData eventsDrawData, @NonNull short[][] inSamples, int inFrameCount,
        @NonNull int[] inEventIndices, @NonNull String[] inEventNames, int inEventCount, int drawStartIndex,
        int drawEndIndex, int drawSurfaceWidth, long lastFrameIndex) {
        //benchmark.start();
        try {
            // process signal
            if (isSignalAveraging()) {
                JniUtils.prepareForThresholdDrawing(signalDrawData, eventsDrawData, inSamples, inFrameCount,
                    inEventIndices, inEventCount, drawStartIndex, drawEndIndex, drawSurfaceWidth);
            } else {
                JniUtils.prepareForSignalDrawing(signalDrawData, eventsDrawData, inSamples, inFrameCount,
                    inEventIndices, inEventCount, drawStartIndex, drawEndIndex, drawSurfaceWidth);
            }
        } catch (Exception e) {
            LOGE(TAG, e.getMessage());
            Crashlytics.logException(e);
        }

        // only process events if threshold is off
        if (!isSignalAveraging()) {
            int indexBase = inEventCount - eventsDrawData.eventCount;
            if (indexBase >= 0) {
                if (eventsDrawData.eventCount >= 0) {
                    System.arraycopy(inEventNames, indexBase, eventsDrawData.eventNames, 0, eventsDrawData.eventCount);
                }
            }
        }

        saveAveragingTriggerEventName(inEventNames, inEventCount);
        //benchmark.end();
    }

    /**
     * {@inheritDoc}
     */
    @Override protected void prepareFftForDrawing(@NonNull FftDrawData fftDrawData, @NonNull float[][] fft,
        int drawStartIndex, int drawEndIndex, float drawWidthMax, int drawSurfaceWidth) {
        //benchmark.start();
        try {
            JniUtils.prepareForFftDrawing(fftDrawData, fft, drawStartIndex, drawEndIndex, drawWidthMax,
                drawSurfaceWidth, (int) fftSurfaceHeight, fftScaleFactor);
        } catch (Exception e) {
            LOGE(TAG, e.getMessage());
            Crashlytics.logException(e);
        }
        //benchmark.end();
    }

    /**
     * {@inheritDoc}
     */
    @Override protected void draw(GL10 gl, @NonNull short[][] samples, @NonNull SignalDrawData signalDrawData,
        @NonNull EventsDrawData eventsDrawData, @NonNull FftDrawData fftDrawData, int selectedChannel, int surfaceWidth,
        int surfaceHeight, float glWindowWidth, float[] waveformScaleFactors, float[] waveformPositions,
        int drawStartIndex, int drawEndIndex, float scaleX, float scaleY, long lastFrameIndex) {
        final boolean showWaveformHandle = signalDrawData.channelCount > 1;
        final boolean isSignalAveraging = isSignalAveraging();
        final boolean isThresholdSignalAveraging = isThresholdAveragingTriggerType();
        final boolean isFftProcessing = isFftProcessing();
        final int sampleRate = getSampleRate();
        boolean selected, showThresholdHandle;
        float[] waveformColor;

        if (isFftProcessing && !isSignalAveraging) {
            // update orthographic projection and draw waveform
            updateOrthoProjection(gl, 0f, -MAX_GL_FFT_VERTICAL_HALF_SIZE, surfaceWidth, MAX_GL_VERTICAL_HALF_SIZE);
            drawWaveform(gl, signalDrawData.samples[selectedChannel], signalDrawData.sampleCounts[selectedChannel],
                waveformScaleFactors[selectedChannel], Colors.WHITE);

            // update orthographic projection and draw FFT spectrogram
            updateOrthoProjection(gl, 0f, 0f, surfaceWidth, surfaceHeight);
            drawFft(gl, fftDrawData, surfaceWidth, fftSurfaceHeight);
        } else {
            for (int i = 0; i < signalDrawData.channelCount; i++) {
                selected = selectedChannel == i;
                showThresholdHandle = selected && isSignalAveraging && isThresholdSignalAveraging;
                waveformColor = getWaveformColor(i);

                gl.glPushMatrix();
                gl.glTranslatef(0f, waveformPositions[i], 0f);

                // draw waveform
                drawWaveform(gl, signalDrawData.samples[i], signalDrawData.sampleCounts[i], waveformScaleFactors[i],
                    waveformColor);

                // draw waveform handle if necessary
                if (showWaveformHandle) {
                    drawWaveformHandle(gl, selected, scaleY, waveformColor);

                    // register waveform handle as draggable area with drag helper
                    glHandle.getBorders(rect);
                    waveformHandleDragHelper.registerDraggableArea(i, rect.x,
                        rect.y + glYToSurfaceY(waveformPositions[i]), rect.width, rect.height);
                }

                // draw threshold if necessary
                if (showThresholdHandle) {
                    float scaledThreshold = threshold * waveformScaleFactors[i];
                    drawThreshold(gl, scaledThreshold, waveformPositions[i], surfaceWidth, scaleY);

                    // register threshold handle as draggable area with drag helper
                    glHandle.getBorders(rect);
                    thresholdHandleDragHelper.registerDraggableArea(i, surfaceWidth - rect.width,
                        rect.y + glYToSurfaceY(waveformPositions[i] + scaledThreshold), rect.width, rect.height);
                }

                gl.glPopMatrix();
            }

            // draw time label
            drawTimeLabel(gl, sampleRate, surfaceWidth, glWindowWidth, scaleY);
        }

        if (!isSignalAveraging) {
            // draw markers
            float h =
                isFftProcessing ? MAX_GL_FFT_VERTICAL_HALF_SIZE + MAX_GL_VERTICAL_HALF_SIZE : MAX_GL_VERTICAL_SIZE;
            float y = isFftProcessing ? -MAX_GL_FFT_VERTICAL_HALF_SIZE : -MAX_GL_VERTICAL_HALF_SIZE;
            float sy = surfaceHeight > 0 ? h / surfaceHeight : 1f;
            float prevX = 0f, prevLabelYOffset = MARKER_LABEL_TOP;
            for (int i = 0; i < eventsDrawData.eventCount; i++) {
                float x = eventsDrawData.eventIndices[i];
                float labelYOffset = MARKER_LABEL_TOP;
                if (i != 0 && (x - prevX) < rect.width) {
                    labelYOffset = prevLabelYOffset + rect.height + MARKER_LABEL_TOP_OFFSET;
                }

                gl.glPushMatrix();
                gl.glTranslatef(x, y, 0f);
                glEventMarker.draw(gl, eventsDrawData.eventNames[i], labelYOffset, h, 1f, sy);
                gl.glPopMatrix();

                if (i != 0) glEventMarker.getBorders(rect);

                prevX = x;
                prevLabelYOffset = labelYOffset;
            }
        } else {
            if (!isThresholdSignalAveraging) {
                // draw average triggering line
                gl.glPushMatrix();
                gl.glTranslatef(surfaceWidth * .5f, -MAX_GL_VERTICAL_HALF_SIZE + MAX_GL_VERTICAL_SIXTH_SIZE, 0f);
                glAveragingTrigger.draw(gl, lastTriggerEventName, MAX_GL_VERTICAL_SIXTH_SIZE * 4, scaleY);
                gl.glPopMatrix();
            }
        }
    }

    //=================================================
    // PRIVATE AND PACKAGE-PRIVATE METHODS
    //=================================================

    void drawWaveform(@NonNull GL10 gl, @NonNull float[] samples, int sampleCount, float scaleFactor,
        @NonNull @Size(4) float[] color) {
        gl.glPushMatrix();
        gl.glScalef(1f, scaleFactor, 1f);
        glWaveform.draw(gl, samples, sampleCount, color);
        gl.glPopMatrix();
    }

    void drawTimeLabel(@NonNull GL10 gl, int sampleRate, float surfaceWidth, float glWindowWidth, float scaleY) {
        gl.glPushMatrix();
        gl.glTranslatef(0f, -MAX_GL_VERTICAL_HALF_SIZE, 0f);
        gl.glScalef(1f, scaleY, 1f);

        // draw time label separator
        gl.glPushMatrix();
        gl.glTranslatef(0f, timeLabelHeight, 0f);
        glTimeLabelSeparator.draw(gl, surfaceWidth * .25f, surfaceWidth * .75f, timeLabelSeparatorWidth,
            Colors.GRAY_DARK);
        gl.glPopMatrix();

        // draw time label
        gl.glPushMatrix();
        gl.glTranslatef((surfaceWidth - timeLabelWidth) * .5f, 0f, 0f);
        glTimeLabel.draw(gl, timeLabelWidth, timeLabelHeight,
            Formats.formatTime_s_msec(glWindowWidth / (float) sampleRate * 1000f * .5f), Colors.WHITE, null);
        gl.glPopMatrix();

        gl.glPopMatrix();
    }

    private void drawWaveformHandle(@NonNull GL10 gl, boolean selected, float scaleY, @NonNull @Size(4) float[] color) {
        gl.glPushMatrix();
        gl.glScalef(1f, scaleY, 1f);
        glHandle.draw(gl, handleBaseRadius, selected, color);
        gl.glPopMatrix();
    }

    private void drawThreshold(@NonNull GL10 gl, float threshold, float waveformPosition, float width, float scaleY) {
        // draw threshold line
        gl.glPushMatrix();
        gl.glTranslatef(0f, threshold, 0f);
        glThresholdLine.draw(gl, 0f, width, DASH_SIZE, LINE_WIDTH, Colors.RED);
        gl.glPopMatrix();
        // draw threshold handle
        if (threshold + waveformPosition < -MAX_GL_VERTICAL_HALF_SIZE) {
            threshold = -MAX_GL_VERTICAL_HALF_SIZE - waveformPosition;
        }
        if (threshold + waveformPosition > MAX_GL_VERTICAL_HALF_SIZE) {
            threshold = MAX_GL_VERTICAL_HALF_SIZE - waveformPosition;
        }
        gl.glPushMatrix();
        gl.glTranslatef(width, threshold, 0f);
        gl.glScalef(-1f, scaleY, 1f);
        glHandle.draw(gl, handleBaseRadius, true, Colors.RED);
        gl.glPopMatrix();
    }

    private void drawFft(@NonNull GL10 gl, @NonNull FftDrawData fftDrawData, int width, float height) {
        gl.glPushMatrix();
        glFft.draw(gl, fftDrawData, width, height);
        gl.glPopMatrix();
    }

    @SuppressWarnings("WeakerAccess") void selectWaveform(int index) {
        if (listener != null) listener.onWaveformSelected(index);
    }

    @SuppressWarnings("WeakerAccess") void updateThreshold(float dy) {
        setThreshold(threshold - surfaceHeightToGlHeight(dy) / getWaveformScaleFactor());
    }

    // Returns the color of the waveform for the specified channel in rgba format. If color is not defined green is returned.
    private @Size(4) float[] getWaveformColor(int channel) {
        int counter = 0;
        int channelCount = getChannelCount();
        for (int i = 0; i < channelCount; i++) {
            if (isChannelVisible(i)) {
                if (counter == channel) {
                    channel = i % channelCount;
                    return channelColors[channel];
                }

                counter++;
            }
        }

        return channelColors[channel];
    }

    // Saves name of the event that was last to trigger signal averaging
    private void saveAveragingTriggerEventName(String[] eventNames, int eventsCount) {
        if (isAllEventsAveragingTriggerType() && eventsCount > 0) {
            lastTriggerEventName = eventNames[eventsCount - 1];
        }
    }

    // Sets the scale factor for the FFT zoom
    private void setFftScaleFactor(float scaleFactor) {
        if (scaleFactor < 0 || scaleFactor == fftScaleFactor) return;
        scaleFactor *= fftScaleFactor;
        if (scaleFactor < MIN_FFT_SCALE_FACTOR) scaleFactor = MIN_FFT_SCALE_FACTOR;
        if (scaleFactor > MAX_FFT_SCALE_FACTOR) scaleFactor = MAX_FFT_SCALE_FACTOR;

        fftScaleFactor = scaleFactor;
    }
}