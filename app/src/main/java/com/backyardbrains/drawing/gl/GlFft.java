package com.backyardbrains.drawing.gl;

import com.backyardbrains.drawing.FftDrawData;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import javax.microedition.khronos.opengles.GL10;

public class GlFft {

    private static final int MAX_VERTICES = 200 * 200;
    private static final int MAX_INDICES = 200 * 200;
    private static final int MAX_COLORS = 200 * 200;

    private static final String[] TIME_AXIS_VALUES = new String[] { "1", "2", "3", "4", "5", "6" };
    private static final float AXES_SCALE_SIZE = 10000f;

    private ByteBuffer vbb;
    private FloatBuffer vfb;
    private ByteBuffer ibb;
    private ShortBuffer isb;
    private ByteBuffer cbb;
    private FloatBuffer cfb;

    private FloatBuffer timeAxisValuesVFB;

    //private float[] vertices = new float[MAX_VERTICES];
    //private short[] indices = new short[MAX_INDICES];
    //private float[] colors = new float[MAX_COLORS];

    public GlFft() {
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

    /**
     * This function draws our square on screen.
     */
    public void draw(GL10 gl, FftDrawData fft, float w, float h) {
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

        // draw graph axes
        drawTimeAxis(gl, 0, 0, w, h);
    }

    private void drawTimeAxis(GL10 gl, float x, float y, float w, float h) {
        int len = TIME_AXIS_VALUES.length * 10;
        float timeAxisValuesStep = w / (len - 1);
        float[] timeAxisValuesVertices = new float[len * 4];
        float[] values = new float[len];
        int j = 0;

        // draw scales
        gl.glColor4f(1f, 1f, 1f, 1f);
        gl.glLineWidth(2f);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        for (int i = 0; i < len; i++) {
            float value = x + timeAxisValuesStep * i;
            timeAxisValuesVertices[j++] = value;
            timeAxisValuesVertices[j++] = 0;
            timeAxisValuesVertices[j++] = value;
            timeAxisValuesVertices[j++] = AXES_SCALE_SIZE;

            values[i] = value;
        }
        if (timeAxisValuesVFB == null || timeAxisValuesVFB.capacity() != timeAxisValuesVertices.length) {
            ByteBuffer vAxisValuesVBB = ByteBuffer.allocateDirect(timeAxisValuesVertices.length * 4);
            vAxisValuesVBB.order(ByteOrder.nativeOrder());
            timeAxisValuesVFB = vAxisValuesVBB.asFloatBuffer();
        }
        timeAxisValuesVFB.put(timeAxisValuesVertices);
        timeAxisValuesVFB.position(0);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, timeAxisValuesVFB);
        gl.glDrawArrays(GL10.GL_LINES, 0, (int) (timeAxisValuesVertices.length * .5f));
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);

        // draw scale values
        //gl.glEnable(GL10.GL_TEXTURE_2D);
        //glText.begin(1f, 1f, 1f, 1f);
        //for (int i = 0; i < len; i++) {
        //    glText.drawCX(formatter.format(hAxisValues[i]), values[i], y);
        //}
        //glText.end();
        //gl.glDisable(GL10.GL_TEXTURE_2D);
    }
}
