package com.backyardbrains.drawing.gl;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.android.texample.GLText;
import com.backyardbrains.drawing.Colors;
import javax.microedition.khronos.opengles.GL10;

/**
 * Defines a visual representation of  marker
 *
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class GlAveragingTriggerLine {

    private static final int LINE_WIDTH = 2;
    private static final float DASH_HEIGHT = 12;

    private final GlVLine vLine;
    private final GlDashedVLine dashedVLine;
    private final GlRectangle rect;
    private final GLText text;

    public GlAveragingTriggerLine(@NonNull Context context, @NonNull GL10 gl) {
        vLine = new GlVLine();
        dashedVLine = new GlDashedVLine();
        rect = new GlRectangle();
        text = new GLText(gl, context.getAssets());
        text.load("dos-437.ttf", 48, 2, 2);
    }

    public void draw(@NonNull GL10 gl, @Nullable String eventName, float x, float y1, float y2, float scaleX,
        float scaleY) {
        gl.glPushMatrix();
        gl.glTranslatef(x, 0f, 0f);

        // draw black line
        vLine.draw(gl, y1, y2, LINE_WIDTH, Colors.BLACK);

        // draw dashed gray line
        dashedVLine.draw(gl, y1, y2, DASH_HEIGHT * scaleY, LINE_WIDTH, Colors.GRAY_DARK);

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
            final float[] glColor = Colors.MARKER_COLORS[(ch - '0') % Colors.MARKER_COLORS.length];
            gl.glColor4f(glColor[0], glColor[1], glColor[2], glColor[3]);

            // scale text before drawing background so we have correct measurements
            text.setScale(scaleX < 1 ? scaleX : 1f, scaleY);
            // draw label background
            float textW = text.getLength(eventName);
            float textH = text.getHeight();
            float labelW = textW * 1.3f;
            float labelH = textH * 1.3f;
            float labelX = labelW * .5f;
            float labelY = y2 - textH;
            gl.glPushMatrix();
            gl.glTranslatef(labelX, labelY, 0f);
            rect.draw(gl, labelW, labelH, glColor);
            gl.glPopMatrix();

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

        gl.glPopMatrix();
    }
}
