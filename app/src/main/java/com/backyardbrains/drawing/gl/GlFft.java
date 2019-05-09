package com.backyardbrains.drawing.gl;

import android.content.Context;
import android.support.annotation.NonNull;
import com.android.texample.GLText;
import com.backyardbrains.drawing.FftDrawData;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.opengles.GL10;

public class GlFft {

    private static final float AXIS_NOTCH_SIZE_1 = 5f;
    private static final float AXIS_NOTCH_SIZE_5 = 10f;
    private static final float AXIS_NOTCH_SIZE_10 = 15f;
    private static final float AXIS_VALUE_X_OFFSET = 20f;
    private static final float AXIS_VALUE_Y_OFFSET = 5f;
    private static final float AXIS_VALUES_X_DRAW_MARGIN = AXIS_NOTCH_SIZE_10 + AXIS_VALUE_X_OFFSET;
    private static final float AXIS_VALUES_Y_DRAW_MARGIN = AXIS_NOTCH_SIZE_10 + AXIS_VALUE_Y_OFFSET;

    private static final String TIME_AXIS_NAME = "Time [S]";
    private static final float TIME_AXIS_NAME_X_OFFSET = 30f;
    private static final float TIME_AXIS_NAME_Y_OFFSET = 20f;
    private static final String[] TIME_AXIS_VALUES = new String[] { "0", "1", "2", "3", "4", "5", "6" };
    private static final int TIME_AXIS_NOTCH_COUNT = (TIME_AXIS_VALUES.length - 1) * 10 + 1; // 61 notch

    private static final float FREQ_AXIS_VALUES_X_OFFSET = AXIS_NOTCH_SIZE_10 + AXIS_VALUE_X_OFFSET;
    private static final String[] FREQ_AXIS_VALUES = new String[] { "0", "10", "20", "30" };
    private static final String[] FREQ_AXIS_VALUES_ZOOMED =
        new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14" };
    private static final int FREQ_AXIS_NOTCH_COUNT = (FREQ_AXIS_VALUES.length - 1) * 10 + 3; // 33 notches
    private static final int FREQ_AXIS_NOTCH_COUNT_ZOOMED = (FREQ_AXIS_VALUES_ZOOMED.length - 1) * 10 + 3; // 143 notch
    private static final float FREQ_AXIS_NOTCH_ZOOM_SWITCH_SCALE =
        FREQ_AXIS_NOTCH_COUNT * 10f / FREQ_AXIS_NOTCH_COUNT_ZOOMED;

    private FloatBuffer timeAxisValuesVFB;
    private FloatBuffer freqAxisValuesVFB;

    private GlSpectrogram glSpectrogram;
    private GLText glText;

    private float timeAxisValueY;
    private float timeAxisNameY;
    private float timeAxisNameW;
    private float drawMarginX, drawMarginY;

    public GlFft(Context context, GL10 gl) {
        glSpectrogram = new GlSpectrogram();
        glText = new GLText(gl, context.getAssets());
        glText.load("dos-437.ttf", 32, 2, 2);

        float textH = glText.getHeight();
        timeAxisValueY = -(textH + AXIS_NOTCH_SIZE_10 + AXIS_VALUE_Y_OFFSET);
        timeAxisNameY = timeAxisValueY - (textH + TIME_AXIS_NAME_Y_OFFSET);
        timeAxisNameW = glText.getLength(TIME_AXIS_NAME);

        drawMarginX = AXIS_VALUES_X_DRAW_MARGIN + glText.getLength(TIME_AXIS_VALUES[0]) * .5f;
        drawMarginY = AXIS_VALUES_Y_DRAW_MARGIN + textH;
    }

    /**
     * This function draws FFT spectrogram with axis on screen.
     */
    public void draw(GL10 gl, @NonNull FftDrawData fft, float w, float h, float scaleY) {
        // draw spectrogram
        glSpectrogram.draw(gl, fft);

        // draw time axis
        gl.glPushMatrix();
        gl.glTranslatef(0f, h, 0f);
        gl.glScalef(1f, scaleY, 1f);
        drawTimeAxis(gl, w, fft.scaleX);
        gl.glPopMatrix();

        // draw frequency axis
        gl.glPushMatrix();
        drawFrequencyAxis(gl, h, scaleY, fft.scaleY);
        gl.glPopMatrix();
    }

    private void drawTimeAxis(GL10 gl, float w, float scaleX) {
        float valueStep = w / (TIME_AXIS_NOTCH_COUNT - 1) * scaleX;
        float[] valuesVertices = new float[TIME_AXIS_NOTCH_COUNT * 4];
        float[] values = new float[TIME_AXIS_VALUES.length];
        int vertexCounter = 0, valueCounter = 0;

        // draw scale notches
        gl.glColor4f(1f, 1f, 1f, 1f);
        gl.glLineWidth(2f);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        for (int i = 0; i < TIME_AXIS_NOTCH_COUNT; i++) {
            float value = w - valueStep * i;
            if (value >= 0) {
                valuesVertices[vertexCounter++] = value;
                valuesVertices[vertexCounter++] = 0f;
                valuesVertices[vertexCounter++] = value;
                if (i % 10 == 0) {
                    valuesVertices[vertexCounter++] = -AXIS_NOTCH_SIZE_10;

                    values[valueCounter++] = value; // save x value so we can draw scale numbers later
                } else if (i % 5 == 0) {
                    valuesVertices[vertexCounter++] = -AXIS_NOTCH_SIZE_5;
                } else {
                    valuesVertices[vertexCounter++] = -AXIS_NOTCH_SIZE_1;
                }
            }
        }
        if (timeAxisValuesVFB == null || timeAxisValuesVFB.capacity() < valuesVertices.length) {
            ByteBuffer vAxisValuesVBB = ByteBuffer.allocateDirect(valuesVertices.length * 4);
            vAxisValuesVBB.order(ByteOrder.nativeOrder());
            timeAxisValuesVFB = vAxisValuesVBB.asFloatBuffer();
        }
        timeAxisValuesVFB.put(valuesVertices);
        timeAxisValuesVFB.position(0);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, timeAxisValuesVFB);
        gl.glDrawArrays(GL10.GL_LINES, 0, (int) (vertexCounter * .5f));
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);

        // draw scale values and axis name
        gl.glEnable(GL10.GL_TEXTURE_2D);
        gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        glText.begin(1f, 1f, 1f, 1f);
        glText.setScale(1f, 1f);
        for (int i = 0; i < valueCounter; i++) {
            if (values[i] > drawMarginX && w - values[i] > drawMarginX) {
                glText.drawCX(TIME_AXIS_VALUES[i], values[i], timeAxisValueY);
            }
        }
        float textX = w - (timeAxisNameW + TIME_AXIS_NAME_X_OFFSET);
        glText.draw(TIME_AXIS_NAME, textX, timeAxisNameY);
        glText.end();
        gl.glDisable(GL10.GL_BLEND);
        gl.glDisable(GL10.GL_TEXTURE_2D);
    }

    private void drawFrequencyAxis(GL10 gl, float h, float scaleY, float notchScaleY) {
        final int notchCount =
            notchScaleY < FREQ_AXIS_NOTCH_ZOOM_SWITCH_SCALE ? FREQ_AXIS_NOTCH_COUNT : FREQ_AXIS_NOTCH_COUNT_ZOOMED;
        final String[] notchValues =
            notchScaleY < FREQ_AXIS_NOTCH_ZOOM_SWITCH_SCALE ? FREQ_AXIS_VALUES : FREQ_AXIS_VALUES_ZOOMED;
        if (notchScaleY >= FREQ_AXIS_NOTCH_ZOOM_SWITCH_SCALE) notchScaleY /= FREQ_AXIS_NOTCH_ZOOM_SWITCH_SCALE;
        final float valueStep = h / (notchCount - 1) * notchScaleY;
        final float[] valuesVertices = new float[notchCount * 4];
        final float[] values = new float[notchValues.length];
        final float drawMarginYScaled = drawMarginY * scaleY;
        int vertexCounter = 0, valueCounter = 0;

        // draw scale notches
        gl.glColor4f(1f, 1f, 1f, 1f);
        gl.glLineWidth(2f);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        for (int i = 0; i < notchCount; i++) {
            float value = valueStep * i;
            if (value < h) {
                valuesVertices[vertexCounter++] = 0f;
                valuesVertices[vertexCounter++] = value;
                if (i % 10 == 0) {
                    valuesVertices[vertexCounter++] = AXIS_NOTCH_SIZE_10;

                    values[valueCounter++] = value; // save y value so we can draw scale numbers later
                } else if (i % 5 == 0) {
                    valuesVertices[vertexCounter++] = AXIS_NOTCH_SIZE_5;
                } else {
                    valuesVertices[vertexCounter++] = AXIS_NOTCH_SIZE_1;
                }
                valuesVertices[vertexCounter++] = value;
            }
        }
        if (freqAxisValuesVFB == null || freqAxisValuesVFB.capacity() < valuesVertices.length) {
            ByteBuffer vAxisValuesVBB = ByteBuffer.allocateDirect(valuesVertices.length * 4);
            vAxisValuesVBB.order(ByteOrder.nativeOrder());
            freqAxisValuesVFB = vAxisValuesVBB.asFloatBuffer();
        }
        freqAxisValuesVFB.put(valuesVertices);
        freqAxisValuesVFB.position(0);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, freqAxisValuesVFB);
        gl.glDrawArrays(GL10.GL_LINES, 0, (int) (vertexCounter * .5f));
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);

        // draw scale values
        gl.glEnable(GL10.GL_TEXTURE_2D);
        gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        glText.setScale(1f, scaleY);
        glText.begin(1f, 1f, 1f, 1f);
        for (int i = 0; i < valueCounter; i++) {
            if (values[i] > drawMarginYScaled && h - values[i] > drawMarginYScaled) {
                glText.drawCY(notchValues[i], FREQ_AXIS_VALUES_X_OFFSET, values[i]);
            }
        }
        glText.end();
        gl.glDisable(GL10.GL_BLEND);
        gl.glDisable(GL10.GL_TEXTURE_2D);
    }
}
