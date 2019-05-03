package com.backyardbrains.drawing.gl;

import android.content.Context;
import android.support.annotation.NonNull;
import com.android.texample.GLText;
import com.backyardbrains.drawing.FftDrawData;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.opengles.GL10;

public class GlFft {

    private static final String TIME_AXIS_NAME = "Time [S]";
    private static final float TIME_AXIS_NAME_X_OFFSET = 30f;
    private static final float TIME_AXIS_NAME_Y_OFFSET = 20f;
    private static final String[] TIME_AXIS_VALUES = new String[] { "0", "1", "2", "3", "4", "5", "6" };
    private static final int TIME_AXIS_NOTCH_COUNT = (TIME_AXIS_VALUES.length - 1) * 10 + 1;
    private static final float TIME_AXIS_OFFSET = 5f;
    private static final float AXES_SIZE_1 = 5f;
    private static final float AXES_SIZE_5 = 10f;
    private static final float AXES_SIZE_10 = 15f;

    private FloatBuffer timeAxisValuesVFB;

    private GlSpectrogram glSpectrogram;
    private GLText glText;

    public GlFft(Context context, GL10 gl) {
        glSpectrogram = new GlSpectrogram();
        glText = new GLText(gl, context.getAssets());
        glText.load("dos-437.ttf", 32, 2, 2);
    }

    /**
     * This function draws our square on screen.
     */
    public void draw(GL10 gl, @NonNull FftDrawData fft, float w, float h, float scaleY) {
        // draw spectrogram
        glSpectrogram.draw(gl, fft);

        // draw time axis
        float scaleX = (float) fft.vertices.length / fft.vertexCount;
        gl.glPushMatrix();
        gl.glTranslatef(0f, h, 0f);
        gl.glScalef(1f, scaleY, 1f);
        drawTimeAxis(gl, 0, 0, w, h, scaleX);
        gl.glPopMatrix();
    }

    private void drawTimeAxis(GL10 gl, float x, float y, float w, float h, float scaleX) {
        float timeAxisValuesStep = w / (TIME_AXIS_NOTCH_COUNT - 1) * scaleX;
        float[] timeAxisValuesVertices = new float[TIME_AXIS_NOTCH_COUNT * 4];
        float[] values = new float[TIME_AXIS_VALUES.length];
        int j = 0, k = 0;
        float end = x + w;

        // draw scale notches
        gl.glColor4f(1f, 1f, 1f, 1f);
        gl.glLineWidth(2f);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        for (int i = 0; i < TIME_AXIS_NOTCH_COUNT; i++) {
            float value = end - timeAxisValuesStep * i;
            timeAxisValuesVertices[j++] = value;
            timeAxisValuesVertices[j++] = 0;
            timeAxisValuesVertices[j++] = value;
            if (i % 10 == 0) {
                timeAxisValuesVertices[j++] = -AXES_SIZE_10;

                values[k++] = value; // save x value so we can draw scale numbers later
            } else if (i % 5 == 0) {
                timeAxisValuesVertices[j++] = -AXES_SIZE_5;
            } else {
                timeAxisValuesVertices[j++] = -AXES_SIZE_1;
            }
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
        gl.glEnable(GL10.GL_TEXTURE_2D);
        gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        glText.begin(1f, 1f, 1f, 1f);
        float textH = glText.getHeight();
        float textY = y - (textH + AXES_SIZE_10 + TIME_AXIS_OFFSET);
        for (int i = 1; i < values.length - 1; i++) {
            glText.drawCX(TIME_AXIS_VALUES[i], values[i], textY);
        }
        float textX = w - (glText.getLength(TIME_AXIS_NAME) + TIME_AXIS_NAME_X_OFFSET);
        textY -= (textH + TIME_AXIS_NAME_Y_OFFSET);
        glText.draw(TIME_AXIS_NAME, textX, textY);
        glText.end();
        gl.glDisable(GL10.GL_BLEND);
        gl.glDisable(GL10.GL_TEXTURE_2D);
    }
}
