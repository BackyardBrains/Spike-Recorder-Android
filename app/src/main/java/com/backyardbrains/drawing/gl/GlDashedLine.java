package com.backyardbrains.drawing.gl;

import androidx.annotation.NonNull;
import androidx.annotation.Size;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.opengles.GL10;

/**
 * Defines a visual representation of dashed line
 *
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public abstract class GlDashedLine {

    private static final int LINE_VERTICES_COUNT = 3000;

    private final FloatBuffer lineVFB;

    private final float[] lineVertices = new float[LINE_VERTICES_COUNT];

    GlDashedLine() {
        ByteBuffer lineVBB = ByteBuffer.allocateDirect(LINE_VERTICES_COUNT * 4);
        lineVBB.order(ByteOrder.nativeOrder());
        lineVFB = lineVBB.asFloatBuffer();
    }

    public final void draw(@NonNull GL10 gl, float c1, float c2, float dashSize, int lineWidth,
        @Size(4) float[] color) {
        gl.glColor4f(color[0], color[1], color[2], color[3]);
        gl.glLineWidth(lineWidth);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        getDashedLineVertices(lineVertices, c1, c2, dashSize);
        lineVFB.put(lineVertices);
        lineVFB.position(0);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, lineVFB);
        gl.glDrawArrays(GL10.GL_LINES, 0, (int) (LINE_VERTICES_COUNT * .5f));
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }

    protected abstract void getDashedLineVertices(float[] vertices, float c1, float c2, float dashSize);
}
