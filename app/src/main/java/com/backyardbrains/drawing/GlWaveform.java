package com.backyardbrains.drawing;

import android.support.annotation.Size;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author Tihomir Leka <ticapeca at gmail.com.
 */
public class GlWaveform {

    // Default vertices size
    private static final int MAX_VERTICES = 5000;

    private static final float LINE_WIDTH = 1f;

    private ShortBuffer waveformVSB;

    GlWaveform() {
        ByteBuffer waveformVBB = ByteBuffer.allocateDirect(MAX_VERTICES * 2);
        waveformVBB.order(ByteOrder.nativeOrder());
        waveformVSB = waveformVBB.asShortBuffer();
    }

    public void draw(GL10 gl, short[] waveformVertices, @Size(4) float[] waveformColor) {
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glColor4f(waveformColor[0], waveformColor[1], waveformColor[2], waveformColor[3]);
        gl.glLineWidth(LINE_WIDTH);
        if (waveformVSB.capacity() != waveformVertices.length * 2) {
            ByteBuffer waveformVBB = ByteBuffer.allocateDirect(waveformVertices.length * 2);
            waveformVBB.order(ByteOrder.nativeOrder());
            waveformVSB = waveformVBB.asShortBuffer();
        }
        waveformVSB.put(waveformVertices);
        waveformVSB.position(0);
        gl.glVertexPointer(2, GL10.GL_SHORT, 0, waveformVSB);
        gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, (int) (waveformVertices.length * .5));
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }
}
