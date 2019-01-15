package com.backyardbrains.drawing.gl;

import android.support.annotation.NonNull;
import android.support.annotation.Size;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.opengles.GL10;

/**
 * Defines a visual representation of a circle.
 *
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class GlCircle {

    private static final int SEGMENT_COUNT = 360;
    private static final int CIRCLE_VERTICES_COUNT = SEGMENT_COUNT * 2 + 2;

    private final FloatBuffer circleVFB;

    private final float[] vertices = new float[CIRCLE_VERTICES_COUNT];

    private final float cos;
    private final float sin;

    GlCircle() {
        ByteBuffer circleVBB = ByteBuffer.allocateDirect(vertices.length * 4);
        circleVBB.order(ByteOrder.nativeOrder());
        circleVFB = circleVBB.asFloatBuffer();

        float theta = (float) (Math.PI * 2 / SEGMENT_COUNT);
        cos = (float) Math.cos(theta);
        sin = (float) Math.sin(theta);
    }

    public void draw(@NonNull GL10 gl, float radius, @NonNull @Size(4) float[] color) {
        gl.glFrontFace(GL10.GL_CCW);
        gl.glColor4f(color[0], color[1], color[2], color[3]);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        float x = radius, y = 0f, temp;
        int counter = 0;
        for (int ii = 0; ii < SEGMENT_COUNT; ii++) {
            vertices[counter++] = x;
            vertices[counter++] = y;

            temp = x;
            x = cos * x - sin * y;
            y = sin * temp + cos * y;
        }
        circleVFB.put(vertices);
        circleVFB.position(0);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, circleVFB);
        gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, (int) (vertices.length * .5f));
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }
}
