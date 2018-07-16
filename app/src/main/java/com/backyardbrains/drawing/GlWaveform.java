package com.backyardbrains.drawing;

import android.support.annotation.Size;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class GlWaveform {

    // Default vertices size
    private static final int MAX_VERTICES = 5000;

    private static final float LINE_WIDTH = 1f;

    private ByteBuffer waveformVBB;
    private ShortBuffer waveformVSB;

    GlWaveform() {
        waveformVBB = ByteBuffer.allocateDirect(MAX_VERTICES * 2);
        waveformVBB.order(ByteOrder.nativeOrder());
        waveformVSB = waveformVBB.asShortBuffer();
    }

    public void draw(GL10 gl, short[] waveformVertices, int waveformVerticesCount, @Size(4) float[] waveformColor) {
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glColor4f(waveformColor[0], waveformColor[1], waveformColor[2], waveformColor[3]);
        gl.glLineWidth(LINE_WIDTH);
        if (waveformVBB.capacity() < waveformVerticesCount * 2) {
            waveformVBB = ByteBuffer.allocateDirect(waveformVerticesCount * 2);
            waveformVBB.order(ByteOrder.nativeOrder());
            waveformVSB = waveformVBB.asShortBuffer();
        }
        waveformVSB.put(waveformVertices, 0, waveformVerticesCount);
        waveformVSB.position(0);
        gl.glVertexPointer(2, GL10.GL_SHORT, 0, waveformVSB);
        gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, (int) (waveformVerticesCount * .5));
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }
}
