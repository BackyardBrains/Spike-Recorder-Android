package com.backyardbrains.drawing.gl;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.android.texample.GLText;
import com.backyardbrains.utils.JniUtils;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class GlGraphAxes {

    private static final float AXIS_WIDTH = 2f;
    private static final int AXES_VERTICES_COUNT = 6;
    private static final short[] AXES_INDICES = { 0, 1, 0, 2 };
    private static final float AXES_SCALE_SIZE = 10f;
    private static final float AXES_VALUES_MARGIN = 20f;

    private static final NumberFormat DEFAULT_FORMATTER = new DecimalFormat("#0.0#");

    private final FloatBuffer axesVFB;
    private final ShortBuffer axesISB;
    private FloatBuffer vAxisValuesVFB;
    private FloatBuffer hAxisValuesVFB;

    private final GLText glText;

    private final float[] borderVertices = new float[AXES_VERTICES_COUNT];

    private NumberFormat hValueFormatter = DEFAULT_FORMATTER;
    private NumberFormat vValueFormatter = DEFAULT_FORMATTER;
    private float vAxisOffset, hAxisOffset;

    GlGraphAxes(@NonNull Context context, @NonNull GL10 gl, @Nullable NumberFormat hAxisValuesFormatter,
        @Nullable NumberFormat vAxisValuesFormatter) {
        if (hAxisValuesFormatter != null) hValueFormatter = hAxisValuesFormatter;
        if (vAxisValuesFormatter != null) vValueFormatter = vAxisValuesFormatter;

        ByteBuffer axesVBB = ByteBuffer.allocateDirect(AXES_VERTICES_COUNT * 4);
        axesVBB.order(ByteOrder.nativeOrder());
        axesVFB = axesVBB.asFloatBuffer();

        ByteBuffer axesIBB = ByteBuffer.allocateDirect(AXES_INDICES.length * 2);
        axesIBB.order(ByteOrder.nativeOrder());
        axesISB = axesIBB.asShortBuffer();
        axesISB.put(AXES_INDICES);
        axesISB.position(0);

        glText = new GLText(gl, context.getAssets());
        glText.load("dos-437.ttf", 32, 0, 0);
    }

    @SuppressWarnings("WeakerAccess") public float getVAxisOffset() {
        return vAxisOffset;
    }

    @SuppressWarnings("WeakerAccess") public float getHAxisOffset() {
        return hAxisOffset;
    }

    public void draw(@NonNull GL10 gl, float x, float y, float w, float h, @NonNull float[] hAxisValues,
        float hAxisMinValue, float hAxisMaxValue, @NonNull float[] vAxisValues, float vAxisMinValue,
        float vAxisMaxValue) {
        // calculate max vertical scale value
        float vValueMaxW = Float.MIN_VALUE;
        for (float vAxisValue : vAxisValues) {
            final float vValueW = glText.getLength(vValueFormatter.format(vAxisValue));
            if (vValueW > vValueMaxW) vValueMaxW = vValueW;
        }
        vAxisOffset = vValueMaxW + AXES_VALUES_MARGIN + AXES_SCALE_SIZE;
        hAxisOffset = glText.getHeight() + AXES_VALUES_MARGIN + AXES_SCALE_SIZE;

        drawGraphAxes(gl, x + vAxisOffset, y + hAxisOffset, w - vAxisOffset, h - hAxisOffset);
        drawGraphHAxisValues(gl, x + vAxisOffset, y, w - vAxisOffset, hAxisOffset, hAxisMinValue, hAxisMaxValue,
            hAxisValues);
        drawGraphVAxisValues(gl, x, y + hAxisOffset, h - hAxisOffset, vAxisOffset, vAxisMinValue, vAxisMaxValue,
            vAxisValues, vValueMaxW);
    }

    private void drawGraphAxes(@NonNull GL10 gl, float x, float y, float w, float h) {
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glColor4f(1f, 1f, 1f, 1f);
        gl.glLineWidth(AXIS_WIDTH);
        borderVertices[0] = x;
        borderVertices[1] = y;
        borderVertices[2] = x;
        borderVertices[3] = y + h;
        borderVertices[4] = x + w;
        borderVertices[5] = y;
        axesVFB.put(borderVertices);
        axesVFB.position(0);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, axesVFB);
        gl.glDrawElements(GL10.GL_LINES, AXES_INDICES.length, GL10.GL_UNSIGNED_SHORT, axesISB);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }

    private void drawGraphHAxisValues(@NonNull GL10 gl, float x, float y, float w, float hOffset, float minValue,
        float maxValue, float[] values) {
        if (minValue > maxValue) return;

        // calculate x coordinates for all x axis values
        int length = values.length;
        float[] xCoords = new float[length];

        // map values to available width
        JniUtils.map(xCoords, values, length, minValue, maxValue, 0f, w);

        float[] hAxisValuesVertices = new float[length * 4];
        int j = 0;

        // draw scales
        gl.glColor4f(1f, 1f, 1f, 1f);
        gl.glLineWidth(2f);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        for (int i = 0; i < length; i++) {
            float value = x + xCoords[i];
            hAxisValuesVertices[j++] = value;
            hAxisValuesVertices[j++] = y + hOffset - AXES_SCALE_SIZE;
            hAxisValuesVertices[j++] = value;
            hAxisValuesVertices[j++] = y + hOffset;

            xCoords[i] = value;
        }
        if (hAxisValuesVFB == null || hAxisValuesVFB.capacity() != hAxisValuesVertices.length) {
            ByteBuffer vAxisValuesVBB = ByteBuffer.allocateDirect(hAxisValuesVertices.length * 4);
            vAxisValuesVBB.order(ByteOrder.nativeOrder());
            hAxisValuesVFB = vAxisValuesVBB.asFloatBuffer();
        }
        hAxisValuesVFB.put(hAxisValuesVertices);
        hAxisValuesVFB.position(0);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, hAxisValuesVFB);
        gl.glDrawArrays(GL10.GL_LINES, 0, (int) (hAxisValuesVertices.length * .5f));
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);

        // draw scale values
        gl.glEnable(GL10.GL_TEXTURE_2D);
        glText.begin(1f, 1f, 1f, 1f);
        for (int i = 0; i < length; i++) {
            glText.drawCX(hValueFormatter.format(values[i]), xCoords[i], y);
        }
        glText.end();
        gl.glDisable(GL10.GL_TEXTURE_2D);
    }

    private void drawGraphVAxisValues(@NonNull GL10 gl, float x, float y, float h, float vOffset, float minValue,
        float maxValue, float[] values, float valueMaxW) {
        if (minValue > maxValue) return;

        // calculate y coordinates for all y axis values
        int length = values.length;
        float[] yCoords = new float[length];

        // map values to available height
        JniUtils.map(yCoords, values, length, minValue, maxValue, 0f, h);

        float[] vAxisValuesVertices = new float[length * 4];
        int j = 0;

        // draw scales
        gl.glColor4f(1f, 1f, 1f, 1f);
        gl.glLineWidth(2f);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        for (int i = 0; i < length; i++) {
            float value = y + yCoords[i];
            vAxisValuesVertices[j++] = x + vOffset - AXES_SCALE_SIZE;
            vAxisValuesVertices[j++] = value;
            vAxisValuesVertices[j++] = x + vOffset;
            vAxisValuesVertices[j++] = value;

            yCoords[i] = value;
        }
        if (vAxisValuesVFB == null || vAxisValuesVFB.capacity() != vAxisValuesVertices.length) {
            ByteBuffer vAxisValuesVBB = ByteBuffer.allocateDirect(vAxisValuesVertices.length * 4);
            vAxisValuesVBB.order(ByteOrder.nativeOrder());
            vAxisValuesVFB = vAxisValuesVBB.asFloatBuffer();
        }
        vAxisValuesVFB.put(vAxisValuesVertices);
        vAxisValuesVFB.position(0);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, vAxisValuesVFB);
        gl.glDrawArrays(GL10.GL_LINES, 0, (int) (vAxisValuesVertices.length * .5f));
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);

        // draw scale values
        gl.glEnable(GL10.GL_TEXTURE_2D);
        glText.begin(1f, 1f, 1f, 1f);
        for (int i = 0; i < length; i++) {
            String value = vValueFormatter.format(values[i]);
            glText.drawCY(value, x + (valueMaxW - glText.getLength(value)), yCoords[i]);
        }
        glText.end();
        gl.glDisable(GL10.GL_TEXTURE_2D);
    }
}
