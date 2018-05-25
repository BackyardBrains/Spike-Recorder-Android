package com.backyardbrains.drawing;

import com.backyardbrains.utils.AudioUtils;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class GlSpikes {

    // We can maximally handle 6 seconds of sample data,
    // we multiply by 2 because we need bytes
    // and again by 2 because we have 2 vertices for every sample
    private static final int MAX_VERTICES = AudioUtils.SAMPLE_RATE * 6 * 2 * 2;

    private static final float POINT_SIZE = 10f;

    private final FloatBuffer spikesVFB;
    private final FloatBuffer spikesColorVFB;

    private float[] spikesVertices = new float[MAX_VERTICES];
    private float[] spikesColors = new float[MAX_VERTICES];

    GlSpikes() {
        ByteBuffer spikesVBB = ByteBuffer.allocateDirect(MAX_VERTICES * 4);
        spikesVBB.order(ByteOrder.nativeOrder());
        spikesVFB = spikesVBB.asFloatBuffer();

        ByteBuffer spikeColorsVBB = ByteBuffer.allocateDirect(MAX_VERTICES * 4);
        spikeColorsVBB.order(ByteOrder.nativeOrder());
        spikesColorVFB = spikeColorsVBB.asFloatBuffer();
    }

    public void draw(GL10 gl, float[] spikesVertices, float[] spikesColors, int verticesCount) {
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
        gl.glPointSize(POINT_SIZE);
        System.arraycopy(spikesVertices, 0, this.spikesVertices, 0, verticesCount);
        spikesVFB.put(this.spikesVertices);
        spikesVFB.position(0);
        System.arraycopy(spikesColors, 0, this.spikesColors, 0, verticesCount * 2);
        spikesColorVFB.put(this.spikesColors);
        spikesColorVFB.position(0);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, spikesVFB);
        gl.glColorPointer(4, GL10.GL_FLOAT, 0, spikesColorVFB);
        gl.glDrawArrays(GL10.GL_POINTS, 0, (int) (verticesCount * .5));
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
    }
}
