package com.backyardbrains.drawing;

import android.support.annotation.NonNull;
import android.support.annotation.Size;
import android.util.SparseArray;
import android.view.MotionEvent;
import com.backyardbrains.drawing.gl.GlHLine;
import com.backyardbrains.drawing.gl.GlHandle;
import com.backyardbrains.drawing.gl.GlHandleDragHelper;
import com.backyardbrains.drawing.gl.GlSpikes;
import com.backyardbrains.drawing.gl.GlWaveform;
import com.backyardbrains.ui.BaseFragment;
import com.backyardbrains.utils.ThresholdOrientation;
import com.backyardbrains.vo.SpikeIndexValue;
import com.backyardbrains.vo.Threshold;
import com.crashlytics.android.Crashlytics;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.LOGE;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class FindSpikesRenderer extends SeekableWaveformRenderer {

    static final String TAG = makeLogTag(FindSpikesRenderer.class);

    private static final int LINE_WIDTH = 1;

    private final GlHandleDragHelper thresholdHandleDragHelper;
    private final GlHandleDragHelper.Rect rect = new GlHandleDragHelper.Rect();

    private final GlWaveform glWaveform;
    private final GlSpikes glSpikes;
    private final GlHLine glThresholdLine;
    private final GlHandle glThresholdHandle;
    private final float[] spikesVertices = new float[GlSpikes.MAX_POINT_VERTICES];
    private final float[] spikesColors = new float[GlSpikes.MAX_COLOR_VERTICES];

    private int[] thresholds = new int[2];

    private float[] currentColor = BYBColors.getColorAsGlById(BYBColors.red);
    private float[] whiteColor = BYBColors.getColorAsGlById(BYBColors.white);

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

        glWaveform = new GlWaveform();
        glSpikes = new GlSpikes();
        glThresholdLine = new GlHLine();
        glThresholdHandle = new GlHandle();

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

    /**
     * {@inheritDoc}
     */
    @Override public void setSelectedChannel(int selectedChannel) {
        super.setSelectedChannel(selectedChannel);

        loadSpikeTrains();
    }

    /**
     * {@inheritDoc}
     */
    @Override protected boolean drawSpikes() {
        return false;
    }

    //private final Benchmark benchmark =
    //    new Benchmark("SPIKES_RETRIEVAL").warmUp(200).sessions(10).measuresPerSession(200).logBySession(false);

    /**
     * {@inheritDoc}
     */
    @Override protected void draw(GL10 gl, @NonNull short[][] samples, int selectedChannel,
        @NonNull short[][] waveformVertices, int[] waveformVerticesCount, @NonNull SparseArray<String> events,
        @NonNull FftDrawData fftDrawData, int surfaceWidth, int surfaceHeight, float glWindowWidth, float[] waveformScaleFactors,
        float[] waveformPositions, int drawStartIndex, int drawEndIndex, float scaleX, float scaleY, long lastFrameIndex) {
        final float samplesToDraw = waveformVerticesCount[0] * .5f;
        final float drawScale = surfaceWidth > 0 ? samplesToDraw / surfaceWidth : 1f;
        final float scaleX1 = samplesToDraw / glWindowWidth;

        gl.glPushMatrix();
        gl.glScalef(1f, waveformScaleFactors[selectedChannel], 1f);
        // draw waveform
        glWaveform.draw(gl, waveformVertices[selectedChannel], waveformVerticesCount[selectedChannel], Colors.GRAY);
        gl.glPopMatrix();

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
            int verticesCount =
                fillSpikesAndColorsBuffers(valuesAndIndices, spikesVertices, spikesColors, glWindowWidth, fromSample,
                    toSample);
            // draw spikes
            if (verticesCount > 0) {
                gl.glPushMatrix();
                gl.glScalef(scaleX1, waveformScaleFactors[selectedChannel], 1f);
                glSpikes.draw(gl, spikesVertices, spikesColors, verticesCount);
                gl.glPopMatrix();
            }

            prevChannel = selectedChannel;
            prevFromSample = fromSample;
            prevToSample = toSample;
        }

        // draw left threshold
        float scaledThreshold = thresholds[ThresholdOrientation.LEFT] * waveformScaleFactors[selectedChannel];
        if (scaledThreshold < -MAX_GL_VERTICAL_HALF_SIZE) scaledThreshold = -MAX_GL_VERTICAL_HALF_SIZE;
        if (scaledThreshold > MAX_GL_VERTICAL_HALF_SIZE) scaledThreshold = MAX_GL_VERTICAL_HALF_SIZE;
        // draw threshold line
        gl.glPushMatrix();
        gl.glScalef(scaleX, waveformScaleFactors[selectedChannel], 1f);
        gl.glTranslatef(0f, thresholds[ThresholdOrientation.LEFT], 0f);
        glThresholdLine.draw(gl, 0f, samplesToDraw - 1, LINE_WIDTH, currentColor);
        gl.glPopMatrix();
        // draw threshold handle
        gl.glPushMatrix();
        gl.glTranslatef(0f, scaledThreshold, 0f);
        gl.glScalef(drawScale, scaleY, 1f);
        glThresholdHandle.draw(gl, currentColor, true);
        gl.glPopMatrix();

        // register left threshold handle as draggable area with drag helper
        glThresholdHandle.getBorders(rect);
        thresholdHandleDragHelper.registerDraggableArea(ThresholdOrientation.LEFT, rect.x,
            rect.y + glYToSurfaceY(scaledThreshold), rect.width, rect.height);

        // draw right threshold
        scaledThreshold = thresholds[ThresholdOrientation.RIGHT] * waveformScaleFactors[selectedChannel];
        if (scaledThreshold < -MAX_GL_VERTICAL_HALF_SIZE) scaledThreshold = -MAX_GL_VERTICAL_HALF_SIZE;
        if (scaledThreshold > MAX_GL_VERTICAL_HALF_SIZE) scaledThreshold = MAX_GL_VERTICAL_HALF_SIZE;
        // draw threshold line
        gl.glPushMatrix();
        gl.glScalef(scaleX, waveformScaleFactors[selectedChannel], 1f);
        gl.glTranslatef(0f, thresholds[ThresholdOrientation.RIGHT], 0f);
        glThresholdLine.draw(gl, 0f, samplesToDraw - 1, LINE_WIDTH, currentColor);
        gl.glPopMatrix();
        // draw threshold handle
        gl.glPushMatrix();
        gl.glTranslatef(samplesToDraw - 1, scaledThreshold, 0f);
        gl.glScalef(-drawScale, scaleY, 1f);
        glThresholdHandle.draw(gl, currentColor, true);
        gl.glPopMatrix();

        // register right threshold handle as draggable area with drag helper
        glThresholdHandle.getBorders(rect);
        thresholdHandleDragHelper.registerDraggableArea(ThresholdOrientation.RIGHT, surfaceWidth - rect.width,
            rect.y + glYToSurfaceY(scaledThreshold), rect.width, rect.height);
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

    // Fills spike and color buffers preparing them for drawing. Number of vertices is returned.
    private int fillSpikesAndColorsBuffers(@NonNull SpikeIndexValue[] valueAndIndices, @NonNull float[] spikesVertices,
        @NonNull float[] spikesColors, float glWindowWidth, long fromSample, long toSample) {
        int verticesCounter = 0;
        try {
            if (valueAndIndices.length > 0) {
                int colorsCounter = 0;
                long index;

                final int min = Math.min(thresholds[ThresholdOrientation.LEFT], thresholds[ThresholdOrientation.RIGHT]);
                final int max = Math.max(thresholds[ThresholdOrientation.LEFT], thresholds[ThresholdOrientation.RIGHT]);

                for (SpikeIndexValue valueAndIndex : valueAndIndices) {
                    if (fromSample <= valueAndIndex.getIndex() && valueAndIndex.getIndex() < toSample) {
                        if (toSample - fromSample < glWindowWidth) { // buffer contains 0 samples in front
                            index = (long) (valueAndIndex.getIndex() + glWindowWidth - toSample);
                        } else { // buffer only contains sample data (no 0 samples in front)
                            index = valueAndIndex.getIndex() - fromSample;
                        }
                        spikesVertices[verticesCounter++] = index;
                        float spikeValue = valueAndIndex.getValue();
                        spikesVertices[verticesCounter++] = spikeValue;
                        float[] colorToSet;
                        if (spikeValue >= min && spikeValue < max) {
                            colorToSet = currentColor;
                        } else {
                            colorToSet = whiteColor;
                        }
                        System.arraycopy(colorToSet, 0, spikesColors, colorsCounter, colorToSet.length);
                        colorsCounter += 4;
                    }
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            LOGE(TAG, e.getMessage());
            Crashlytics.logException(e);
        }

        return verticesCounter;
    }
}
