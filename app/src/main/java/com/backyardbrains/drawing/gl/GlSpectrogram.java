package com.backyardbrains.drawing.gl;

import android.support.annotation.NonNull;
import com.backyardbrains.drawing.FftDrawData;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class GlSpectrogram {

    private static final int MAX_VERTICES = 200 * 200;
    private static final int MAX_INDICES = 200 * 200;
    private static final int MAX_COLORS = 200 * 200;

    private ByteBuffer vbb;
    private FloatBuffer vfb;
    private ByteBuffer ibb;
    private ShortBuffer isb;
    private ByteBuffer cbb;
    private FloatBuffer cfb;

    //private float[] vertices = new float[MAX_VERTICES];
    //private short[] indices = new short[MAX_INDICES];
    //private float[] colors = new float[MAX_COLORS];

    public GlSpectrogram() {
        vbb = ByteBuffer.allocateDirect(MAX_VERTICES * 4);
        vbb.order(ByteOrder.nativeOrder());
        vfb = vbb.asFloatBuffer();

        ibb = ByteBuffer.allocateDirect(MAX_INDICES * 2);
        ibb.order(ByteOrder.nativeOrder());
        isb = ibb.asShortBuffer();

        cbb = ByteBuffer.allocateDirect(MAX_COLORS * 4);
        cbb.order(ByteOrder.nativeOrder());
        cfb = cbb.asFloatBuffer();
    }

    public void draw(GL10 gl, @NonNull FftDrawData fft) {
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
        //System.arraycopy(fft.vertices, 0, vertices, 0, fft.vertexCount);
        if (vbb.capacity() != fft.vertexCount * 4) {
            vbb = ByteBuffer.allocateDirect(fft.vertexCount * 4);
            vbb.order(ByteOrder.nativeOrder());
            vfb = vbb.asFloatBuffer();
        }
        vfb.put(fft.vertices, 0, fft.vertexCount);
        vfb.position(0);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, vfb);
        //System.arraycopy(fft.colors, 0, colors, 0, fft.colorCount);
        if (cbb.capacity() != fft.colorCount * 4) {
            cbb = ByteBuffer.allocateDirect(fft.colorCount * 4);
            cbb.order(ByteOrder.nativeOrder());
            cfb = cbb.asFloatBuffer();
        }
        cfb.put(fft.colors, 0, fft.colorCount);
        cfb.position(0);
        gl.glColorPointer(4, GL10.GL_FLOAT, 0, cfb);
        //System.arraycopy(fft.indices, 0, indices, 0, fft.indexCount);
        if (ibb.capacity() != fft.indexCount * 2) {
            ibb = ByteBuffer.allocateDirect(fft.indexCount * 2);
            ibb.order(ByteOrder.nativeOrder());
            isb = ibb.asShortBuffer();
        }
        isb.put(fft.indices, 0, fft.indexCount);
        isb.position(0);
        gl.glDrawElements(GL10.GL_TRIANGLES, fft.indexCount, GL10.GL_UNSIGNED_SHORT, isb);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
    }
}
