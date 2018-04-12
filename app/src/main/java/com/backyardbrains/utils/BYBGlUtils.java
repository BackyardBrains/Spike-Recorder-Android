package com.backyardbrains.utils;

import android.util.Log;
import com.backyardbrains.drawing.BYBColors;
import java.nio.FloatBuffer;
import javax.microedition.khronos.opengles.GL10;

public class BYBGlUtils {

    public static final int DEFAULT_GL_WINDOW_HORIZONTAL_SIZE = 4000;
    public static final int DEFAULT_GL_WINDOW_VERTICAL_SIZE = 10000;
    public static final float DEFAULT_MIN_DETECTED_PCM_VALUE = -5000000f;

    // ----------------------------------------------------------------------------------------
    public static void glClear(GL10 gl) {
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
    }

    public static void drawGlLine(GL10 gl, float x0, float y0, float x1, float y1, int color) {
        float[] line = new float[] { x0, y0, x1, y1 };
        //        FloatBuffer l = BYBUtils.getFloatBufferFromFloatArray(line);
        //        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        //        float [] glColor = BYBColors.getHexAsGlColor(color);
        //        gl.glColor4f(glColor[0],glColor[1],glColor[2],glColor[3]);
        //        gl.glLineWidth(2.0f);
        //        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, l);
        //        gl.glDrawArrays(GL10.GL_LINES, 0, 2);
        //        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);

        BYBGlUtils.drawArray2D(gl, line, color, 2);
    }

    public static void drawArray2D(GL10 gl, float[] array, int color, float lineWidth) {
        drawArray2D(gl, array, color, lineWidth, GL10.GL_LINES);
    }

    public static void drawArray2D(GL10 gl, float[] array, int color, float lineWidth, int mode) {
        FloatBuffer buffer = BYBUtils.getFloatBufferFromFloatArray(array, array.length);
        if (array.length % 2 != 0) {
            Log.e("BYBGlUtils", "drawArray2D incorrect array size. array.length%2 !=0");
        }
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        float[] glColor = BYBColors.getHexAsGlColor(color);
        gl.glColor4f(glColor[0], glColor[1], glColor[2], glColor[3]);
        //        gl.glColor4f(0.0f, 1.0f, 1.0f, 1.0f);
        //        gl.glLineWidth(lineWidth);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, buffer);

        gl.glDrawArrays(mode, 0, (int) Math.floor(array.length / 2.0));//GL10.GL_LINES
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }

    public static void drawRectangle(GL10 gl, int x, int y, int w, int h, int color) {
        float[] array = new float[] { x, y, x + w, y, x, y + h, x + w, y + h };
        drawArray2D(gl, array, color, 1, GL10.GL_TRIANGLE_STRIP);
    }
}
