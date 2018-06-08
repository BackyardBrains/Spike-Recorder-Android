package com.backyardbrains.drawing;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import com.android.texample.GLText;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class GlBarGraph {

    private static final float GRAPH_NAME_MARGIN = 5f;
    private static final float AXES_WIDTH = 2f;
    private static final int AXES_VERTICES_COUNT = 6;
    private static final short[] AXES_INDICES = { 0, 1, 0, 2 };
    private final static int VERTICES_PER_SPRITE = 3;
    private final static int INDICES_PER_SPRITE = 6;

    private final GLText text;
    private final FloatBuffer axesVFB;
    private final ShortBuffer axesISB;
    private FloatBuffer graphVFB;
    private ShortBuffer graphISB;

    private final float[] borderVertices = new float[AXES_VERTICES_COUNT];

    GlBarGraph(@NonNull Context context, @NonNull GL10 gl) {
        ByteBuffer axesVBB = ByteBuffer.allocateDirect(AXES_VERTICES_COUNT * 4);
        axesVBB.order(ByteOrder.nativeOrder());
        axesVFB = axesVBB.asFloatBuffer();

        ByteBuffer axesIBB = ByteBuffer.allocateDirect(AXES_INDICES.length * 2);
        axesIBB.order(ByteOrder.nativeOrder());
        axesISB = axesIBB.asShortBuffer();
        axesISB.put(AXES_INDICES);
        axesISB.position(0);

        text = new GLText(gl, context.getAssets());
        text.load("dos-437.ttf", 48, 5, 5);
    }

    public void draw(@NonNull GL10 gl, float x, float y, float w, float h, float[] graphVertices,
        @Size(4) float[] color, @Nullable String graphName) {
        // draw graph
        drawGraph(gl, x, y, w, h, graphVertices, color);
        // draw border lines
        drawGraphBorders(gl, x, y, w, h);
        // draw graph name if there is one
        //if (graphName != null) drawGraphName(gl, x, y, w, h, graphName);
    }

    private void drawGraph(@NonNull GL10 gl, float x, float y, float w, float h, float[] graphVertices,
        @Size(4) float[] color) {
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glColor4f(color[0], color[1], color[2], color[3]);
        int len = graphVertices.length;
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
        float barWidth = w / graphVertices.length;
        j = 0;
        for (i = 0; i < len; i++, j += INDICES_PER_SPRITE) {
            vertices[j] = x + barWidth * i;
            vertices[j + 1] = y;
            vertices[j + 2] = x + barWidth * i;
            vertices[j + 3] = y + h * graphVertices[i];
            vertices[j + 4] = x + barWidth * (i + 1);
            vertices[j + 5] = y + h * graphVertices[i];
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

    private void drawGraphBorders(@NonNull GL10 gl, float x, float y, float w, float h) {
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

    private void drawGraphName(@NonNull GL10 gl, float x, float y, float w, float h, @Nullable String graphName) {
        float textW = text.getLength(graphName);
        float textH = text.getHeight();
        gl.glEnable(GL10.GL_TEXTURE_2D);
        text.begin(1f, 1f, 1f, 1f);
        text.draw(graphName, x + w - (textW + GRAPH_NAME_MARGIN), y + h - (textH + GRAPH_NAME_MARGIN));
        text.end();
        gl.glDisable(GL10.GL_TEXTURE_2D);
    }
}
