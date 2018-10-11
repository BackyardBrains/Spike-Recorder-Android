package com.backyardbrains.drawing.gl;

import android.support.annotation.NonNull;
import android.support.annotation.Size;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.opengles.GL10;

/**
 * Defines a visual representation of a line
 *
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class GlVLine {

    private static final int LINE_VERTICES_COUNT = 4;

    private final FloatBuffer lineVFB;

    private final float[] lineVertices = new float[LINE_VERTICES_COUNT];

    GlVLine() {
        ByteBuffer lineVBB = ByteBuffer.allocateDirect(LINE_VERTICES_COUNT * 4);
        lineVBB.order(ByteOrder.nativeOrder());
        lineVFB = lineVBB.asFloatBuffer();
    }

    public void draw(@NonNull GL10 gl, float x, float y0, float y1, int lineWidth, @Size(4) float[] color) {
        gl.glColor4f(color[0], color[1], color[2], color[3]);
        gl.glLineWidth(lineWidth);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        lineVertices[0] = x;
        lineVertices[1] = y0;
        lineVertices[2] = x;
        lineVertices[3] = y1;
        lineVFB.put(lineVertices);
        lineVFB.position(0);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, lineVFB);
        gl.glDrawArrays(GL10.GL_LINES, 0, 2);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }
}
