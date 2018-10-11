package com.backyardbrains.drawing.gl;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.android.texample.GLText;
import javax.microedition.khronos.opengles.GL10;

/**
 * Defines a visual representation of  marker
 *
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class GlAveragingTriggerLine {

    private static final float[][] MARKER_COLORS = new float[][] {
        new float[] { .847f, .706f, .906f, 1f }, new float[] { 1f, .314f, 0f, 1f }, new float[] { 1f, .925f, .58f, 1f },
        new float[] { 1f, .682f, .682f, 1f }, new float[] { .69f, .898f, .486f, 1f },
        new float[] { .706f, .847f, .906f, 1f }, new float[] { .757f, .855f, .839f, 1f },
        new float[] { .675f, .82f, .914f, 1f }, new float[] { .682f, 1f, .682f, 1f }, new float[] { 1f, .925f, 1f, 1f }
    };
    private static final float[] BACKGROUND_LINE_COLOR = new float[] { 0f, 0f, 0f, 1f };
    private static final float[] DASHED_LINE_COLOR = new float[] { .58824f, .58824f, .58824f, 1f };
    private static final int LINE_WIDTH = 2;
    private static final float DASH_HEIGHT = 12;

    private final GlVLine line;
    private final GlDashedVLine dashedLine;
    private final GlRectangle rect;
    private final GLText text;

    public GlAveragingTriggerLine(@NonNull Context context, @NonNull GL10 gl) {
        line = new GlVLine();
        dashedLine = new GlDashedVLine();
        rect = new GlRectangle();
        text = new GLText(gl, context.getAssets());
        text.load("dos-437.ttf", 48, 2, 2);
    }

    public void draw(@NonNull GL10 gl, @Nullable String eventName, float x, float y0, float y1, float scaleX,
        float scaleY) {
        // draw black line
        line.draw(gl, x, y0, y1, LINE_WIDTH, BACKGROUND_LINE_COLOR);

        // draw dashed gray line
        dashedLine.draw(gl, x, y0, y1, DASH_HEIGHT * scaleY, LINE_WIDTH, DASHED_LINE_COLOR);

        if (eventName != null) {
            int len = eventName.length();
            int ascii;
            // we just use event up to the first unsupported character
            for (int i = 0; i < len; i++) {
                ascii = (int) eventName.charAt(i);
                if (ascii < GLText.CHAR_START || ascii > GLText.CHAR_END) {
                    eventName = eventName.substring(0, i);
                    break;
                }
            }
            final char ch = eventName.length() > 0 ? eventName.charAt(0) : '1';
            final float[] glColor = MARKER_COLORS[(ch - '0') % MARKER_COLORS.length];
            gl.glColor4f(glColor[0], glColor[1], glColor[2], glColor[3]);

            // draw label background
            text.setScale(scaleX < 1 ? scaleX : 1f, scaleY);
            float textW = text.getLength(eventName);
            float textH = text.getHeight();
            float labelW = textW * 1.3f;
            float labelH = textH * 1.3f;
            float labelX = x + labelW * .5f;
            float labelY = y1 - textH;
            rect.draw(gl, labelX, labelY, labelW, labelH, glColor);

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
}
