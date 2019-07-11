package com.backyardbrains.drawing.gl;

import android.support.annotation.NonNull;
import android.support.annotation.Size;
import com.backyardbrains.utils.JniUtils;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class GlLineGraph {

    private FloatBuffer lineVFB;

    private float[] vertices;
    private float[] normalizedData;

    public void draw(@NonNull GL10 gl, float x, float y, float w, float h, @NonNull float[] data,
        @Size(4) float[] color) {
        int length = data.length;
        int verticesCount = length * 2;

        // map values to available height
        if (normalizedData == null || normalizedData.length < length) normalizedData = new float[length];
        JniUtils.map(normalizedData, data, length, -1f, 1f, 0f, h);

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glColor4f(color[0], color[1], color[2], color[3]);
        gl.glLineWidth(3f);
        if (vertices == null || vertices.length < verticesCount) vertices = new float[verticesCount];
        float xIncrement = w / (length - 1);
        int j = 0, i;
        for (i = 0; i < length; i++) {
            vertices[j++] = x + xIncrement * i;
            vertices[j++] = y + normalizedData[i];
        }
        if (lineVFB == null || lineVFB.capacity() != vertices.length) {
            ByteBuffer averageLineVBB = ByteBuffer.allocateDirect(vertices.length * 4);
            averageLineVBB.order(ByteOrder.nativeOrder());
            lineVFB = averageLineVBB.asFloatBuffer();
        }
        lineVFB.put(vertices);
        lineVFB.position(0);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, lineVFB);
        gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, length);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }
}