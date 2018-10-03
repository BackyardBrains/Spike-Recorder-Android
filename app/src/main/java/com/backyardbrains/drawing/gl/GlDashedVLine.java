package com.backyardbrains.drawing.gl;

import android.support.annotation.NonNull;
import android.support.annotation.Size;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.opengles.GL10;

/**
 * Defines a visual representation of dashed line
 *
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class GlDashedVLine {

    private static final int LINE_VERTICES_COUNT = 1000;

    private final FloatBuffer lineVFB;

    private final float[] lineVertices = new float[LINE_VERTICES_COUNT];

    GlDashedVLine() {
        ByteBuffer lineVBB = ByteBuffer.allocateDirect(LINE_VERTICES_COUNT * 4);
        lineVBB.order(ByteOrder.nativeOrder());
        lineVFB = lineVBB.asFloatBuffer();
    }

    public void draw(@NonNull GL10 gl, float x, float y0, float y1, float dashSize, int lineWidth,
        @Size(4) float[] color) {
        gl.glColor4f(color[0], color[1], color[2], color[3]);
        gl.glLineWidth(lineWidth);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        int count = getDashedLineVertices(lineVertices, x, y0, y1, dashSize);
        lineVFB.put(lineVertices);
        lineVFB.position(0);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, lineVFB);
        gl.glDrawArrays(GL10.GL_LINES, 0, (int) (count * .5));
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }

    private int getDashedLineVertices(float[] vertices, float x, float y0, float y1, float dashSize) {
        int counter = 0;
        int start = (int) Math.min(y0, y1);
        int end = (int) Math.max(y0, y1);
        if ((end - start) / dashSize > LINE_VERTICES_COUNT) dashSize = (end - start) / dashSize;
        for (int i = start; i < end; i += dashSize * 2) {
            vertices[counter++] = x;
            vertices[counter++] = i;
            vertices[counter++] = x;
            vertices[counter++] = i + dashSize;
        }

        return counter;
    }
}
