package com.backyardbrains.drawing;

import android.content.Context;
import android.support.annotation.NonNull;
import com.android.texample.GLText;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import javax.microedition.khronos.opengles.GL10;

/**
 * Defines a visual representation of  marker
 *
 * @author Tihomir Leka <ticapeca at gmail.com.
 */
public class EventMarker {

    private static final float[][] MARKER_COLORS = new float[][] {
        new float[] { .847f, .706f, .906f, 1f }, new float[] { 1f, .314f, 0f, 1f }, new float[] { 1f, .925f, .58f, 1f },
        new float[] { 1f, .682f, .682f, 1f }, new float[] { .69f, .898f, .486f, 1f },
        new float[] { .706f, .847f, .906f, 1f }, new float[] { .757f, .855f, .839f, 1f },
        new float[] { .675f, .82f, .914f, 1f }, new float[] { .682f, 1f, .682f, 1f }, new float[] { 1f, .925f, 1f, 1f }
    };
    private static final int LINE_WIDTH = 2;
    private static final float LABEL_TOP = 230f;
    private static final int LINE_VERTICES_COUNT = 4;
    private static final int LABEL_VERTICES_COUNT = 8;
    private static final short[] INDICES = { 0, 1, 2, 0, 2, 3 };

    private final FloatBuffer lineVFB;
    private final FloatBuffer labelVFB;
    private final ShortBuffer indicesBuffer;
    private final GLText text;

    EventMarker(@NonNull Context context, @NonNull GL10 gl) {
        ByteBuffer lineVBB = ByteBuffer.allocateDirect(LINE_VERTICES_COUNT * 4);
        lineVBB.order(ByteOrder.nativeOrder());
        lineVFB = lineVBB.asFloatBuffer();

        ByteBuffer labelVBB = ByteBuffer.allocateDirect(LABEL_VERTICES_COUNT * 4);
        labelVBB.order(ByteOrder.nativeOrder());
        labelVFB = labelVBB.asFloatBuffer();

        ByteBuffer ibb = ByteBuffer.allocateDirect(INDICES.length * 2);
        ibb.order(ByteOrder.nativeOrder());
        indicesBuffer = ibb.asShortBuffer();
        indicesBuffer.put(INDICES);
        indicesBuffer.position(0);

        text = new GLText(gl, context.getAssets());
        text.load("dos-437.ttf", 48, 2, 2);
    }

    public void draw(@NonNull GL10 gl, @NonNull String eventName, float x, float y0, float y1, float scaleX,
        float scaleY) {
        final char ch = eventName.length() > 0 ? eventName.charAt(0) : '1';
        final float[] glColor = MARKER_COLORS[(ch - '0') % MARKER_COLORS.length];
        gl.glColor4f(glColor[0], glColor[1], glColor[2], glColor[3]);
        gl.glLineWidth(LINE_WIDTH);

        // draw line
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        lineVFB.put(new float[] { x, y0, x, y1 });
        lineVFB.position(0);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, lineVFB);
        gl.glDrawArrays(GL10.GL_LINES, 0, 2);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);

        // draw label background
        text.setScale(scaleX, scaleY);
        float textW = text.getLength(eventName);
        float textH = text.getHeight();
        float labelW = textW * 1.3f;
        float labelH = textH * 1.3f;
        float labelX = x - labelW * .5f;
        float labelY = y1 - LABEL_TOP * scaleY;

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        labelVFB.put(new float[] {
            labelX, labelY, labelX, labelY + labelH, labelX + labelW, labelY + labelH, labelX + labelW, labelY
        });
        labelVFB.position(0);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, labelVFB);
        gl.glDrawElements(GL10.GL_TRIANGLES, INDICES.length, GL10.GL_UNSIGNED_SHORT, indicesBuffer);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);

        // draw label text
        gl.glEnable(GL10.GL_TEXTURE_2D);
        gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        text.begin(0f, 0f, 0f, 1f);
        text.draw(eventName, labelX + (labelW - textW) * .5f, labelY + (labelH - textH) * .5f);
        text.end();
        gl.glDisable(GL10.GL_BLEND);
        gl.glDisable(GL10.GL_TEXTURE_2D);
    }
}
