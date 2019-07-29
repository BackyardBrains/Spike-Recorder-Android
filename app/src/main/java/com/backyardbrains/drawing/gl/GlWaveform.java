package com.backyardbrains.drawing.gl;

import androidx.annotation.Size;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class GlWaveform {

    // Default vertices size
    private static final int MAX_VERTICES = 5000;

    private static final float LINE_WIDTH = 1f;

    private ByteBuffer waveformVBB;
    private FloatBuffer waveformVFB;

    public GlWaveform() {
        waveformVBB = ByteBuffer.allocateDirect(MAX_VERTICES * 4);
        waveformVBB.order(ByteOrder.nativeOrder());
        waveformVFB = waveformVBB.asFloatBuffer();
    }

    public void draw(GL10 gl, float[] waveformVertices, int waveformVerticesCount, @Size(4) float[] waveformColor) {
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glColor4f(waveformColor[0], waveformColor[1], waveformColor[2], waveformColor[3]);
        gl.glLineWidth(LINE_WIDTH);
        if (waveformVBB.capacity() < waveformVerticesCount * 4) {
            waveformVBB = ByteBuffer.allocateDirect(waveformVerticesCount * 4);
            waveformVBB.order(ByteOrder.nativeOrder());
            waveformVFB = waveformVBB.asFloatBuffer();
        }
        waveformVFB.put(waveformVertices, 0, waveformVerticesCount);
        waveformVFB.position(0);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, waveformVFB);
        gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, (int) (waveformVerticesCount * .5f));
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }
}
