package com.backyardbrains.drawing;

import androidx.annotation.NonNull;
import androidx.annotation.Size;
import android.view.MotionEvent;
import com.backyardbrains.drawing.gl.GlHLine;
import com.backyardbrains.drawing.gl.GlHandle;
import com.backyardbrains.drawing.gl.GlHandleDragHelper;
import com.backyardbrains.drawing.gl.GlSpikes;
import com.backyardbrains.drawing.gl.Rect;
import com.backyardbrains.ui.BaseFragment;
import com.backyardbrains.utils.JniUtils;
import com.backyardbrains.utils.ThresholdOrientation;
import com.backyardbrains.utils.ViewUtils;
import com.backyardbrains.vo.SpikeIndexValue;
import com.backyardbrains.vo.Threshold;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.LOGE;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class FindSpikesRenderer extends SeekableWaveformRenderer {

    static final String TAG = makeLogTag(FindSpikesRenderer.class);

    private static final int LINE_WIDTH = 1;
    // Radius of the handle base
    private static final float HANDLE_BASE_RADIUS_DP = 10f;

    private final GlHandleDragHelper thresholdHandleDragHelper;
    private final Rect rect = new Rect();

    private final GlSpikes glSpikes;
    private final GlHLine glThresholdLine;
    private final GlHandle glThresholdHandle;

    private final SpikesDrawData spikesDrawData = new SpikesDrawData(GlSpikes.MAX_SPIKES);

    private final float handleBaseRadius;

    private int[] thresholds = new int[2];

    private float[] currentColor = new float[4];
    private float[] whiteColor = Colors.WHITE;

    private String filePath;
    private long spikeAnalysisId = -1;
    private int selectedSpikeTrain;
    private int prevChannel, prevFromSample, prevToSample;
    private SpikeIndexValue[] valuesAndIndices;

    public FindSpikesRenderer(@NonNull BaseFragment fragment, @NonNull String filePath) {
        super(filePath, fragment);

        this.filePath = filePath;

        setMeasureEnabled(false);

        thresholdHandleDragHelper = new GlHandleDragHelper(new GlHandleDragHelper.OnDragListener() {
            @Override public void onDragStart(int index) {
                // ignore
            }

            @Override public void onDrag(int index, float dy) {
                updateSpikeTrainThreshold(dy, index);
            }

            @Override public void onDragStop(int index) {
                saveSpikeTrainThreshold(index);
            }
        });

        glSpikes = new GlSpikes();
        glThresholdLine = new GlHLine();
        glThresholdHandle = new GlHandle();

        handleBaseRadius = ViewUtils.dpToPx(fragment.getResources(), HANDLE_BASE_RADIUS_DP);

        setCurrentColor(Colors.RED);

        loadSpikeTrains();
    }

    //=================================================
    //  Renderer INTERFACE IMPLEMENTATIONS
    //=================================================

    /**
     * {@inheritDoc}
     */
    @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
        super.onSurfaceChanged(gl, width, height);

        thresholdHandleDragHelper.resetDraggableAreas();
        thresholdHandleDragHelper.setSurfaceHeight(height);
    }

    //=================================================
    //  TouchEnabledRenderer INTERFACE IMPLEMENTATIONS
    //=================================================

    /**
     * {@inheritDoc}
     */
    @Override public boolean onTouchEvent(MotionEvent event) {
        return thresholdHandleDragHelper.onTouch(event);
    }

    //=================================================
    //  PUBLIC AND PROTECTED METHODS
    //=================================================

    /**
     * Sets currently selected spike train
     */
    public void setSelectedSpikeTrain(int selectedSpikeTrain) {
        this.selectedSpikeTrain = selectedSpikeTrain;

        loadSpikeTrains();
    }

    //=================================================
    //  OVERRIDES
    //=================================================

    @Override public void onChannelSelectionChanged(int channelIndex) {
        super.onChannelSelectionChanged(channelIndex);

        loadSpikeTrains();
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
        final int samplesToDraw = (int) (signalDrawData.sampleCounts[0] * .5f);
        final int sampleRate = getSampleRate();

        // draw waveform
        drawWaveform(gl, signalDrawData.samples[selectedChannel], signalDrawData.sampleCounts[selectedChannel],
            waveformScaleFactors[selectedChannel], Colors.GRAY);

        if (getAnalysisManager() != null) {
            // retry getting spike analysis id until we have it
            if (spikeAnalysisId <= 0) {
                spikeAnalysisId = getAnalysisManager().getSpikeAnalysisId(filePath);
                if (spikeAnalysisId <= 0) return;
            }

            // let's save start and end sample positions that are being drawn before triggering the actual draw
            final int toSample = (int) lastFrameIndex;
            final int fromSample = (int) Math.max(0, toSample - glWindowWidth);
            boolean shouldQuerySamples =
                prevChannel != selectedChannel || prevFromSample != fromSample || prevToSample != toSample;
            //benchmark.start();
            if (valuesAndIndices == null || valuesAndIndices.length == 0 || shouldQuerySamples) {
                valuesAndIndices =
                    getAnalysisManager().getSpikesForRange(spikeAnalysisId, selectedChannel, fromSample, toSample);
            }
            //benchmark.end();
            final int min = Math.min(thresholds[ThresholdOrientation.LEFT], thresholds[ThresholdOrientation.RIGHT]);
            final int max = Math.max(thresholds[ThresholdOrientation.LEFT], thresholds[ThresholdOrientation.RIGHT]);
            try {
                JniUtils.prepareForSpikesDrawing(spikesDrawData, valuesAndIndices, currentColor, whiteColor, min, max,
                    fromSample, toSample, drawStartIndex, drawEndIndex, samplesToDraw, surfaceWidth);
            } catch (Exception e) {
                LOGE(TAG, e.getMessage());
                FirebaseCrashlytics.getInstance().recordException(e);
            }
            // draw spikes
            if (spikesDrawData.vertexCount > 0) {
                gl.glPushMatrix();
                gl.glScalef(1f, waveformScaleFactors[selectedChannel], 1f);
                glSpikes.draw(gl, spikesDrawData.vertices, spikesDrawData.colors, spikesDrawData.vertexCount);
                gl.glPopMatrix();
            }

            prevChannel = selectedChannel;
            prevFromSample = fromSample;
            prevToSample = toSample;
        }

        // draw left threshold
        float scaledThreshold = thresholds[ThresholdOrientation.LEFT] * waveformScaleFactors[selectedChannel];
        // draw threshold line
        gl.glPushMatrix();
        gl.glTranslatef(0f, scaledThreshold, 0f);
        //gl.glScalef(1f, waveformScaleFactors[selectedChannel], 1f);
        glThresholdLine.draw(gl, 0f, surfaceWidth, LINE_WIDTH, currentColor);
        gl.glPopMatrix();
        // draw threshold handle
        if (scaledThreshold < -MAX_GL_VERTICAL_HALF_SIZE) scaledThreshold = -MAX_GL_VERTICAL_HALF_SIZE;
        if (scaledThreshold > MAX_GL_VERTICAL_HALF_SIZE) scaledThreshold = MAX_GL_VERTICAL_HALF_SIZE;
        gl.glPushMatrix();
        gl.glTranslatef(0f, scaledThreshold, 0f);
        gl.glScalef(1f, scaleY, 1f);
        glThresholdHandle.draw(gl, handleBaseRadius, true, currentColor);
        gl.glPopMatrix();

        // register left threshold handle as draggable area with drag helper
        glThresholdHandle.getBorders(rect);
        thresholdHandleDragHelper.registerDraggableArea(ThresholdOrientation.LEFT, rect.x,
            rect.y + glYToSurfaceY(scaledThreshold), rect.width, rect.height);

        // draw right threshold
        scaledThreshold = thresholds[ThresholdOrientation.RIGHT] * waveformScaleFactors[selectedChannel];
        // draw threshold line
        gl.glPushMatrix();
        gl.glTranslatef(0f, scaledThreshold, 0f);
        gl.glScalef(1f, waveformScaleFactors[selectedChannel], 1f);
        glThresholdLine.draw(gl, 0f, surfaceWidth, LINE_WIDTH, currentColor);
        gl.glPopMatrix();
        // draw threshold handle
        if (scaledThreshold < -MAX_GL_VERTICAL_HALF_SIZE) scaledThreshold = -MAX_GL_VERTICAL_HALF_SIZE;
        if (scaledThreshold > MAX_GL_VERTICAL_HALF_SIZE) scaledThreshold = MAX_GL_VERTICAL_HALF_SIZE;
        gl.glPushMatrix();
        gl.glTranslatef(surfaceWidth, scaledThreshold, 0f);
        gl.glScalef(-1f, scaleY, 1f);
        glThresholdHandle.draw(gl, handleBaseRadius, true, currentColor);
        gl.glPopMatrix();

        // register right threshold handle as draggable area with drag helper
        glThresholdHandle.getBorders(rect);
        thresholdHandleDragHelper.registerDraggableArea(ThresholdOrientation.RIGHT, surfaceWidth - rect.width,
            rect.y + glYToSurfaceY(scaledThreshold), rect.width, rect.height);

        // draw time label
        drawTimeLabel(gl, sampleRate, surfaceWidth, glWindowWidth, scaleY);
    }

    //=================================================
    //  PRIVATE AND PACKAGE-PRIVATE METHODS
    //=================================================

    private void loadSpikeTrains() {
        if (getAnalysisManager() != null) {
            getAnalysisManager().getSpikeTrainThresholdsByChannel(filePath, getSelectedChanel(), thresholds -> {
                final int thresholdCount = thresholds.size();
                if (thresholdCount > 0 && selectedSpikeTrain < thresholdCount) {
                    final Threshold threshold = thresholds.get(selectedSpikeTrain);
                    setSpikeTrainThreshold(threshold.getThreshold(ThresholdOrientation.LEFT),
                        ThresholdOrientation.LEFT);
                    setSpikeTrainThreshold(threshold.getThreshold(ThresholdOrientation.RIGHT),
                        ThresholdOrientation.RIGHT);

                    setCurrentColor(Colors.SPIKE_TRAIN_COLORS[selectedSpikeTrain]);
                }
            });
        }
    }

    @SuppressWarnings("WeakerAccess") void updateSpikeTrainThreshold(float dy, @ThresholdOrientation int orientation) {
        setSpikeTrainThreshold((int) (thresholds[orientation] - surfaceHeightToGlHeight(dy) / getWaveformScaleFactor()),
            orientation);
    }

    @SuppressWarnings("WeakerAccess") void saveSpikeTrainThreshold(@ThresholdOrientation int orientation) {
        if (getAnalysisManager() != null) {
            getAnalysisManager().setThreshold(filePath, getSelectedChanel(), selectedSpikeTrain, orientation,
                thresholds[orientation]);
        }
    }

    private void setSpikeTrainThreshold(int t, @ThresholdOrientation int orientation) {
        thresholds[orientation] = t;
    }

    private void setCurrentColor(@Size(4) float[] color) {
        System.arraycopy(color, 0, currentColor, 0, currentColor.length);
    }
}
