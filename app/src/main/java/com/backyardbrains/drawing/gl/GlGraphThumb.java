package com.backyardbrains.drawing.gl;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.android.texample.GLText;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class GlGraphThumb {

    private static final float GRAPH_NAME_MARGIN = 5f;
    private static final float BORDER_LINES_WIDTH = 2f;
    private static final int BORDER_VERTICES_COUNT = 8;
    private static final short[] BORDER_INDICES = { 0, 1, 1, 2, 2, 3, 3, 0 };

    private final GLText text;

    private final FloatBuffer borderVFB;
    private final ShortBuffer borderISB;

    private final float[] borderVertices = new float[BORDER_VERTICES_COUNT];

    public GlGraphThumb(@NonNull Context context, @NonNull GL10 gl) {
        ByteBuffer borderLinesVBB = ByteBuffer.allocateDirect(BORDER_VERTICES_COUNT * 4);
        borderLinesVBB.order(ByteOrder.nativeOrder());
        borderVFB = borderLinesVBB.asFloatBuffer();

        ByteBuffer ibb = ByteBuffer.allocateDirect(BORDER_INDICES.length * 2);
        ibb.order(ByteOrder.nativeOrder());
        borderISB = ibb.asShortBuffer();
        borderISB.put(BORDER_INDICES);
        borderISB.position(0);

        text = new GLText(gl, context.getAssets());
        text.load("dos-437.ttf", 48, 5, 5);
    }

    public void draw(@NonNull GL10 gl, float x, float y, float w, float h, @Nullable String graphName) {
        // draw border lines
        drawGraphBorders(gl, x, y, w, h);
        // draw graph name if there is one
        if (graphName != null) drawGraphName(gl, x, y, w, h, graphName);
    }

    private void drawGraphBorders(@NonNull GL10 gl, float x, float y, float w, float h) {
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glColor4f(0.3f, 0.3f, 0.3f, 1f);
        gl.glLineWidth(BORDER_LINES_WIDTH);
        borderVertices[0] = x;
        borderVertices[1] = y;
        borderVertices[2] = x + w;
        borderVertices[3] = y;
        borderVertices[4] = x + w;
        borderVertices[5] = y + h;
        borderVertices[6] = x;
        borderVertices[7] = y + h;
        borderVFB.put(borderVertices);
        borderVFB.position(0);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, borderVFB);
        gl.glDrawElements(GL10.GL_LINES, BORDER_INDICES.length, GL10.GL_UNSIGNED_SHORT, borderISB);
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