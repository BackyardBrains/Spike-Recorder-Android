package com.backyardbrains.drawing;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import com.android.texample.GLText;
import com.backyardbrains.utils.GlUtils;
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
public class GlBarGraph {

    private static final int MAX_AXIS_VALUES = 5;
    private static final float AXES_VALUES_MARGIN = 20f;
    private static final float AXES_SCALE_SIZE = 10f;

    private static final float GRAPH_NAME_MARGIN = 5f;
    private static final float AXES_WIDTH = 2f;
    private static final int AXES_VERTICES_COUNT = 6;
    private static final short[] AXES_INDICES = { 0, 1, 0, 2 };
    private static final int VERTICES_PER_SPRITE = 3;
    private static final int INDICES_PER_SPRITE = 6;

    private static final NumberFormat DEFAULT_FORMATTER = new DecimalFormat("#0.0#");

    private final GLText glText;
    private final FloatBuffer axesVFB;
    private final ShortBuffer axesISB;
    private FloatBuffer vAxisValuesVFB;
    private FloatBuffer hAxisValuesVFB;
    private FloatBuffer graphVFB;
    private ShortBuffer graphISB;

    private NumberFormat formatter;

    private final float[] borderVertices = new float[AXES_VERTICES_COUNT];

    GlBarGraph(@NonNull Context context, @NonNull GL10 gl) {
        this(context, gl, DEFAULT_FORMATTER);
    }

    GlBarGraph(@NonNull Context context, @NonNull GL10 gl, @NonNull NumberFormat hAxisValuesFormatter) {
        this.formatter = hAxisValuesFormatter;

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

    public void draw(@NonNull GL10 gl, float x, float y, float w, float h, @Nullable int[] vAxisValues,
        float[] hAxisValues, @Size(4) float[] color, @Nullable String graphName) {
        if (vAxisValues == null) return;

        int[] vAxisMinMax = GlUtils.getMinMax(vAxisValues);
        float[] vAxisNormalizedValues = GlUtils.normalize(vAxisValues);
        float vGraphOffset =
            glText.getLength(String.valueOf(vAxisMinMax[GlUtils.MAX_VALUE])) + AXES_VALUES_MARGIN + AXES_SCALE_SIZE;
        float hGraphOffset = glText.getHeight() + AXES_VALUES_MARGIN + AXES_SCALE_SIZE;

        // draw graph
        drawGraph(gl, x + vGraphOffset, y + hGraphOffset, w - vGraphOffset, h - hGraphOffset, vAxisNormalizedValues,
            color);
        // draw graph axes
        drawGraphAxes(gl, x + vGraphOffset, y + hGraphOffset, w - vGraphOffset, h - hGraphOffset);
        // draw vertical axis values
        drawGraphVAxisValues(gl, x, y + hGraphOffset, h - hGraphOffset, vGraphOffset, vAxisMinMax[GlUtils.MAX_VALUE]);
        // draw horizontal axis values
        drawGraphHAxesValues(gl, x + vGraphOffset, y, w - vGraphOffset, hGraphOffset, hAxisValues);
        // draw graph name if there is one
        //if (graphName != null) drawGraphName(gl, x, y, w, h, graphName);
    }

    private void drawGraph(@NonNull GL10 gl, float x, float y, float w, float h, float[] values,
        @Size(4) float[] color) {
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glColor4f(color[0], color[1], color[2], color[3]);
        int len = values.length;
        int i;
        short j = 0;
        // calculate indices
        short[] indices = new short[len * INDICES_PER_SPRITE];
        for (i = 0; i < indices.length; i += INDICES_PER_SPRITE, j += VERTICES_PER_SPRITE) {
            indices[i] = j;
            indices[i + 1] = (short) (j + 1);
            indices[i + 2] = (short) (j + 2);
            indices[i + 3] = (short) (j + 2);
            indices[i + 4] = (short) (j + 3);
            indices[i + 5] = j;
        }
        if (graphISB == null || graphISB.capacity() != indices.length) {
            ByteBuffer ibb = ByteBuffer.allocateDirect(indices.length * 2);
            ibb.order(ByteOrder.nativeOrder());
            graphISB = ibb.asShortBuffer();
        }
        graphISB.put(indices);
        graphISB.position(0);
        // calculate vertices
        float[] vertices = new float[len * INDICES_PER_SPRITE + 2];
        float barWidth = w / values.length;
        j = 0;
        for (i = 0; i < len; i++, j += INDICES_PER_SPRITE) {
            vertices[j] = x + barWidth * i;
            vertices[j + 1] = y;
            vertices[j + 2] = x + barWidth * i;
            vertices[j + 3] = y + h * values[i];
            vertices[j + 4] = x + barWidth * (i + 1);
            vertices[j + 5] = y + h * values[i];
        }
        vertices[j] = x + barWidth * i;
        vertices[j + 1] = y;
        if (graphVFB == null || graphVFB.capacity() != vertices.length) {
            ByteBuffer graphVBB = ByteBuffer.allocateDirect(vertices.length * 4);
            graphVBB.order(ByteOrder.nativeOrder());
            graphVFB = graphVBB.asFloatBuffer();
        }
        graphVFB.put(vertices);
        graphVFB.position(0);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, graphVFB);
        gl.glDrawElements(GL10.GL_TRIANGLES, indices.length, GL10.GL_UNSIGNED_SHORT, graphISB);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }

    private void drawGraphAxes(@NonNull GL10 gl, float x, float y, float w, float h) {
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glColor4f(1f, 1f, 1f, 1f);
        gl.glLineWidth(AXES_WIDTH);
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

    private void drawGraphVAxisValues(@NonNull GL10 gl, float x, float y, float h, float vOffset, int graphMaxValue) {
        float vAxesValueW = glText.getLength(String.valueOf(graphMaxValue));
        int[] res = GlUtils.calculateVAxisCountAndStep(graphMaxValue, MAX_AXIS_VALUES);
        int vAxisValuesCount = res[GlUtils.V_AXIS_VALUES_COUNT] + 1; // +1 for zero
        int vAxisValuesStep = res[GlUtils.V_AXIS_VALUES_STEP];
        float vAxisValuesNormalizedStep = (float) vAxisValuesStep / graphMaxValue;
        float[] vAxisValuesVertices = new float[vAxisValuesCount * 4];
        float[] values = new float[vAxisValuesCount];
        int j = 0;

        // draw scales
        gl.glColor4f(1f, 1f, 1f, 1f);
        gl.glLineWidth(2f);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        for (int i = 0; i < vAxisValuesCount; i++) {
            float value = y + h * vAxisValuesNormalizedStep * i;
            vAxisValuesVertices[j++] = x + vOffset - AXES_SCALE_SIZE;
            vAxisValuesVertices[j++] = value;
            vAxisValuesVertices[j++] = x + vOffset;
            vAxisValuesVertices[j++] = value;

            values[i] = value;
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
        for (int i = 0; i < vAxisValuesCount; i++) {
            String value = String.valueOf(vAxisValuesStep * i);
            glText.drawCY(value, x + (vAxesValueW - glText.getLength(value)), values[i]);
        }
        glText.end();
        gl.glDisable(GL10.GL_TEXTURE_2D);
    }

    private void drawGraphHAxesValues(@NonNull GL10 gl, float x, float y, float w, float hOffset, float[] hAxisValues) {
        int len = hAxisValues.length;
        float hAxisValuesStep = w / (len - 1);
        float[] hAxisValuesVertices = new float[len * 4];
        float[] values = new float[len];
        int j = 0;

        // draw scales
        gl.glColor4f(1f, 1f, 1f, 1f);
        gl.glLineWidth(2f);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        for (int i = 0; i < len; i++) {
            float value = x + hAxisValuesStep * i;
            hAxisValuesVertices[j++] = value;
            hAxisValuesVertices[j++] = y + hOffset - AXES_SCALE_SIZE;
            hAxisValuesVertices[j++] = value;
            hAxisValuesVertices[j++] = y + hOffset;

            values[i] = value;
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
        for (int i = 0; i < len; i++) {
            glText.drawCX(formatter.format(hAxisValues[i]), values[i], y);
        }
        glText.end();
        gl.glDisable(GL10.GL_TEXTURE_2D);
    }

    private void drawGraphName(@NonNull GL10 gl, float x, float y, float w, float h, @Nullable String graphName) {
        float textW = glText.getLength(graphName);
        float textH = glText.getHeight();
        gl.glEnable(GL10.GL_TEXTURE_2D);
        glText.begin(1f, 1f, 1f, 1f);
        glText.draw(graphName, x + w - (textW + GRAPH_NAME_MARGIN), y + h - (textH + GRAPH_NAME_MARGIN));
        glText.end();
        gl.glDisable(GL10.GL_TEXTURE_2D);
    }
}
