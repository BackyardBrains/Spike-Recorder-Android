package com.backyardbrains.drawing.gl;

import androidx.annotation.NonNull;
import androidx.annotation.Size;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.opengles.GL10;

/**
 * Defines a visual representation of a triangle.
 *
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class GlTriangle {

    private static final int COORDS_PER_VERTEX = 3;
    private static final int TRIANGLE_VERTICES_COUNT = 3;

    private final FloatBuffer triangleVFB;

    private final float[] vertices = new float[TRIANGLE_VERTICES_COUNT * COORDS_PER_VERTEX];

    GlTriangle() {
        ByteBuffer triangleVBB = ByteBuffer.allocateDirect(vertices.length * 4);
        triangleVBB.order(ByteOrder.nativeOrder());
        triangleVFB = triangleVBB.asFloatBuffer();
    }

    public void draw(@NonNull GL10 gl, float x1, float y1, float x2, float y2, float x3, float y3,
        @NonNull @Size(4) float[] color) {
        gl.glColor4f(color[0], color[1], color[2], color[3]);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        vertices[0] = x1;
        vertices[1] = y1;
        vertices[2] = 0f;
        vertices[3] = x2;
        vertices[4] = y2;
        vertices[5] = 0f;
        vertices[6] = x3;
        vertices[7] = y3;
        vertices[8] = 0f;
        triangleVFB.put(vertices);
        triangleVFB.position(0);
        gl.glVertexPointer(COORDS_PER_VERTEX, GL10.GL_FLOAT, 0, triangleVFB);
        gl.glDrawArrays(GL10.GL_TRIANGLES, 0, TRIANGLE_VERTICES_COUNT);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }
}
