package com.backyardbrains.drawing.gl;

import androidx.annotation.NonNull;
import androidx.annotation.Size;
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

    private static final int VERTICES_COUNT = 8;
    private static final short[] BACKGROUND_INDICES = { 0, 1, 2, 0, 2, 3 };
    private static final short[] BORDER_INDICES = { 0, 1, 2, 3 };
    private static final float BORDER_LINE_WIDTH = 1f;

    private final FloatBuffer verticesVFB;
    private final ShortBuffer backgroundIndicesVSB;
    private final ShortBuffer borderIndicesVSB;

    private final float[] vertices = new float[VERTICES_COUNT];

    GlRectangle() {
        ByteBuffer labelVBB = ByteBuffer.allocateDirect(VERTICES_COUNT * 4);
        labelVBB.order(ByteOrder.nativeOrder());
        verticesVFB = labelVBB.asFloatBuffer();

        ByteBuffer backgroundIBB = ByteBuffer.allocateDirect(BACKGROUND_INDICES.length * 2);
        backgroundIBB.order(ByteOrder.nativeOrder());
        backgroundIndicesVSB = backgroundIBB.asShortBuffer();
        backgroundIndicesVSB.put(BACKGROUND_INDICES);
        backgroundIndicesVSB.position(0);

        ByteBuffer borderIBB = ByteBuffer.allocateDirect(BORDER_INDICES.length * 2);
        borderIBB.order(ByteOrder.nativeOrder());
        borderIndicesVSB = borderIBB.asShortBuffer();
        borderIndicesVSB.put(BORDER_INDICES);
        borderIndicesVSB.position(0);
    }

    public void draw(@NonNull GL10 gl, float w, float h, @NonNull @Size(4) float[] backgroundColor) {
        gl.glColor4f(backgroundColor[0], backgroundColor[1], backgroundColor[2], backgroundColor[3]);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        vertices[0] = 0f;
        vertices[1] = 0f;
        vertices[2] = 0f;
        vertices[3] = h;
        vertices[4] = w;
        vertices[5] = h;
        vertices[6] = w;
        vertices[7] = 0f;
        verticesVFB.put(vertices);
        verticesVFB.position(0);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, verticesVFB);
        gl.glDrawElements(GL10.GL_TRIANGLES, BACKGROUND_INDICES.length, GL10.GL_UNSIGNED_SHORT, backgroundIndicesVSB);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }

    public void draw(@NonNull GL10 gl, float w, float h, @NonNull @Size(4) float[] backgroundColor,
        @NonNull @Size(4) float[] borderColor) {
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);

        vertices[0] = 0f;
        vertices[1] = 0f;
        vertices[2] = 0f;
        vertices[3] = h;
        vertices[4] = w;
        vertices[5] = h;
        vertices[6] = w;
        vertices[7] = 0f;
        verticesVFB.put(vertices);
        verticesVFB.position(0);

        // draw background
        gl.glColor4f(backgroundColor[0], backgroundColor[1], backgroundColor[2], backgroundColor[3]);
        gl.glLineWidth(BORDER_LINE_WIDTH);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, verticesVFB);
        gl.glDrawElements(GL10.GL_TRIANGLES, BACKGROUND_INDICES.length, GL10.GL_UNSIGNED_SHORT, backgroundIndicesVSB);

        // draw border
        gl.glColor4f(borderColor[0], borderColor[1], borderColor[2], borderColor[3]);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, verticesVFB);
        gl.glDrawElements(GL10.GL_LINE_LOOP, BORDER_INDICES.length, GL10.GL_UNSIGNED_SHORT, borderIndicesVSB);

        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }
}
