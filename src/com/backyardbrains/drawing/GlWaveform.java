package com.backyardbrains.drawing;

import com.backyardbrains.utils.AudioUtils;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author Tihomir Leka <ticapeca at gmail.com.
 */
public class GlWaveform {

    // We can maximally handle 6 seconds of sample data,
    // we multiply by 2 because we need bytes
    // and again by 2 because we have 2 vertices for every sample
    private static final int MAX_VERTICES = AudioUtils.SAMPLE_RATE * 6 * 2 * 2;

    private static final float LINE_WIDTH = 1f;

    private final FloatBuffer waveformVFB;

    GlWaveform() {
        ByteBuffer waveformVBB = ByteBuffer.allocateDirect(MAX_VERTICES * 4);
        waveformVBB.order(ByteOrder.nativeOrder());
        waveformVFB = waveformVBB.asFloatBuffer();
    }

    public void draw(GL10 gl, float[] waveformVertices) {
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glColor4f(0f, 1f, 0f, 1f);
        gl.glLineWidth(LINE_WIDTH);
        waveformVFB.put(waveformVertices);
        waveformVFB.position(0);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, waveformVFB);
        gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, (int) (waveformVertices.length * .5));
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }
}
