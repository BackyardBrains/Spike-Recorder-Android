package com.backyardbrains.drawing.gl;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Size;
import com.android.texample.GLText;
import com.backyardbrains.vo.AverageSpike;
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
public class GlAverageSpikeGraph {

    private static final float V_AXIS_MARGIN = 20f;
    private static final float V_AXIS_SCALE_WIDTH = 10f;
    private static final int V_AXIS_VERTICES_COUNT = 8;

    private static final int INDICES_PER_SPRITE = 6;
    private static final int VERTICES_PER_SPRITE = 4;

    private static final NumberFormat FORMATTER = new DecimalFormat("0.00");

    private final GLText glText;
    private final FloatBuffer vAxisVFB;
    private FloatBuffer graphVFB;
    private ShortBuffer graphISB;
    private FloatBuffer averageLineVFB;

    private final float[] vAxisVertices = new float[V_AXIS_VERTICES_COUNT];

    public GlAverageSpikeGraph(@NonNull Context context, @NonNull GL10 gl) {
        ByteBuffer vAxisVBB = ByteBuffer.allocateDirect(V_AXIS_VERTICES_COUNT * 4);
        vAxisVBB.order(ByteOrder.nativeOrder());
        vAxisVFB = vAxisVBB.asFloatBuffer();

        glText = new GLText(gl, context.getAssets());
        glText.load("dos-437.ttf", 32, 0, 0);
    }

    public float getHOffset() {
        return V_AXIS_SCALE_WIDTH + V_AXIS_MARGIN;
    }

    public void draw(@NonNull GL10 gl, float x, float y, float w, float h, @NonNull AverageSpike data,
        @Size(4) float[] color) {
        // draw average spike graph
        drawAverageSpikeGraph(gl, x + getHOffset(), y, w - getHOffset(), h, data, color);
        // draw average line
        drawAverageLine(gl, x + getHOffset(), y, w - getHOffset(), h, data);
        // draw vertical axes
        drawVerticalAxes(gl, x, y, h, data);
    }

    private void drawAverageSpikeGraph(@NonNull GL10 gl, float x, float y, float w, float h, @NonNull AverageSpike data,
        @Size(4) float[] color) {
        int len = data.getAverageSpike().length;

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glColor4f(color[0], color[1], color[2], color[3]);
        // calculate indices
        short[] indices = new short[(len - 1) * INDICES_PER_SPRITE];
        int j = 0, i;
        for (i = 0; i < indices.length; i += INDICES_PER_SPRITE, j += 2) {
            indices[i] = (short) j;
            indices[i + 1] = (short) (j + 1);
            indices[i + 2] = (short) (j + 2);
            indices[i + 3] = (short) (j + 1);
            indices[i + 4] = (short) (j + 2);
            indices[i + 5] = (short) (j + 3);
        }
        if (graphISB == null || graphISB.capacity() != indices.length) {
            ByteBuffer ibb = ByteBuffer.allocateDirect(indices.length * 2);
            ibb.order(ByteOrder.nativeOrder());
            graphISB = ibb.asShortBuffer();
        }
        graphISB.put(indices);
        graphISB.position(0);
        // calculate vertices
        float[] vertices = new float[len * VERTICES_PER_SPRITE];
        float xIncrement = w / (len - 1);
        j = 0;
        for (i = 0; i < len; i++, j += VERTICES_PER_SPRITE) {
            float tmpX = x + xIncrement * i;
            vertices[j] = tmpX;
            vertices[j + 1] = y + data.getNormBottomSTDLine()[i] * h;
            vertices[j + 2] = tmpX;
            vertices[j + 3] = y + data.getNormTopSTDLine()[i] * h;
        }
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

    private void drawAverageLine(@NonNull GL10 gl, float x, float y, float w, float h, @NonNull AverageSpike data) {
        int len = data.getAverageSpike().length;

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glColor4f(0f, 0f, 0f, 1f);
        gl.glLineWidth(3f);
        float[] vertices = new float[len * 2];
        float xIncrement = w / (len - 1);
        int j = 0, i;
        for (i = 0; i < len; i++) {
            vertices[j++] = x + xIncrement * i;
            vertices[j++] = y + data.getNormAverageSpike()[i] * h;
        }
        if (averageLineVFB == null || averageLineVFB.capacity() != vertices.length) {
            ByteBuffer averageLineVBB = ByteBuffer.allocateDirect(vertices.length * 4);
            averageLineVBB.order(ByteOrder.nativeOrder());
            averageLineVFB = averageLineVBB.asFloatBuffer();
        }
        averageLineVFB.put(vertices);
        averageLineVFB.position(0);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, averageLineVFB);
        gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, (int) (vertices.length * .5f));
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }

    private void drawVerticalAxes(@NonNull GL10 gl, float x, float y, float h, @NonNull AverageSpike data) {
        float[] averageMinMax = GlUtils.getMinMax(data.getNormAverageSpike());
        float max = averageMinMax[GlUtils.MAX_VALUE];
        float min = averageMinMax[GlUtils.MIN_VALUE];

        // draw vertical axis
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glColor4f(1f, 1f, 1f, 1f);
        gl.glLineWidth(2f);
        float x1 = x + V_AXIS_SCALE_WIDTH;
        float y0 = y + max * h;
        float y1 = y + min * h;
        vAxisVertices[0] = x1;
        vAxisVertices[1] = y0;
        vAxisVertices[2] = x;
        vAxisVertices[3] = y0;
        vAxisVertices[4] = x;
        vAxisVertices[5] = y1;
        vAxisVertices[6] = x1;
        vAxisVertices[7] = y1;
        vAxisVFB.put(vAxisVertices);
        vAxisVFB.position(0);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, vAxisVFB);
        gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, (int) (vAxisVertices.length * .5f));
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);

        // draw average value
        float textH = glText.getHeight();
        gl.glEnable(GL10.GL_TEXTURE_2D);
        glText.begin(1f, 1f, 1f, 1f);
        glText.draw(constructAverageValueText(min, max), x + V_AXIS_MARGIN, y0 - textH - V_AXIS_MARGIN);
        glText.end();
        gl.glDisable(GL10.GL_TEXTURE_2D);
    }

    private String constructAverageValueText(float min, float max) {
        float yScale = max - min;
        String unit;
        if (yScale >= 0.2f) {
            unit = "V";
        } else {
            unit = "mV";
            yScale = yScale * 1000f;
        }

        return FORMATTER.format(yScale) + unit;
    }
}
