package com.backyardbrains.drawing.gl;

import android.support.annotation.NonNull;
import android.support.annotation.Size;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import javax.microedition.khronos.opengles.GL10;

/**
 * Defines a visual representation of rectangle
 *
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class GlRectangle {

    private static final int RECT_VERTICES_COUNT = 8;
    private static final short[] INDICES = { 0, 1, 2, 0, 2, 3 };

    private final FloatBuffer labelVFB;
    private final ShortBuffer indicesVSB;

    private final float[] rectVertices = new float[RECT_VERTICES_COUNT];

    GlRectangle() {
        ByteBuffer labelVBB = ByteBuffer.allocateDirect(RECT_VERTICES_COUNT * 4);
        labelVBB.order(ByteOrder.nativeOrder());
        labelVFB = labelVBB.asFloatBuffer();

        ByteBuffer ibb = ByteBuffer.allocateDirect(INDICES.length * 2);
        ibb.order(ByteOrder.nativeOrder());
        indicesVSB = ibb.asShortBuffer();
        indicesVSB.put(INDICES);
        indicesVSB.position(0);
    }

    public void draw(@NonNull GL10 gl, float w, float h, @Size(4) float[] color) {
        gl.glColor4f(color[0], color[1], color[2], color[3]);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        rectVertices[0] = 0f;
        rectVertices[1] = 0f;
        rectVertices[2] = 0f;
        rectVertices[3] = h;
        rectVertices[4] = w;
        rectVertices[5] = h;
        rectVertices[6] = w;
        rectVertices[7] = 0f;
        labelVFB.put(rectVertices);
        labelVFB.position(0);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, labelVFB);
        gl.glDrawElements(GL10.GL_TRIANGLES, INDICES.length, GL10.GL_UNSIGNED_SHORT, indicesVSB);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }
}
