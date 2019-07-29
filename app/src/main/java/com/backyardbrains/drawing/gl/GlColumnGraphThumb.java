package com.backyardbrains.drawing.gl;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import com.backyardbrains.utils.GlUtils;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class GlColumnGraphThumb extends GlGraphThumb {

    private final static int VERTICES_PER_SPRITE = 3;
    private final static int INDICES_PER_SPRITE = 6;

    private FloatBuffer graphVFB;
    private ShortBuffer graphISB;

    public GlColumnGraphThumb(@NonNull Context context, @NonNull GL10 gl) {
        super(context, gl);
    }

    public void draw(@NonNull GL10 gl, float x, float y, float w, float h, @Nullable int[] values,
        @Size(4) float[] color, @Nullable String graphName) {
        if (values == null) return;

        float[] normalizedValues = GlUtils.normalize(values);

        // draw graph
        drawGraph(gl, x, y, w, h, normalizedValues, color);

        // draw borders and name
        super.draw(gl, x, y, w, h, graphName);
    }

    private void drawGraph(@NonNull GL10 gl, float x, float y, float w, float h, float[] values,
        @Size(4) float[] color) {
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glColor4f(color[0], color[1], color[2], color[3]);
        float barWidth = w / values.length;
        int len = values.length;
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
        j = 0;
        for (i = 0; i < len; i++, j += INDICES_PER_SPRITE) {
            vertices[j] = x + barWidth * i;
            vertices[j + 1] = y;
            vertices[j + 2] = x + barWidth * i;
            vertices[j + 3] = y + h * values[i];
            vertices[j + 4] = x + barWidth * (i + 1);
            vertices[j + 5] = y + h * values[i];
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
}
