package com.backyardbrains.drawing.gl;

import android.content.Context;
import androidx.annotation.NonNull;
import com.android.texample.GLText;
import com.backyardbrains.drawing.Colors;
import javax.microedition.khronos.opengles.GL10;

/**
 * Defines a visual representation of  marker
 *
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class GlEventMarker {

    private static final int LINE_WIDTH = 2;

    private final GlVLine vLine;
    private final GlRectangle rect;
    private final GLText text;

    private final Rect borders;

    public GlEventMarker(@NonNull Context context, @NonNull GL10 gl) {
        vLine = new GlVLine();
        rect = new GlRectangle();
        text = new GLText(gl, context.getAssets());
        text.load("dos-437.ttf", 48, 2, 2);

        borders = new Rect(0f, 0f, 0f, text.getHeight());
    }

    public void draw(@NonNull GL10 gl, String eventName, float labelOffset, float height, float scaleX, float scaleY) {
        if (eventName == null) return;

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
        gl.glLineWidth(LINE_WIDTH);

        // draw line
        vLine.draw(gl, 0f, height, LINE_WIDTH, glColor);

        // draw label background
        text.setScale(scaleX, scaleY);
        float textW = text.getLength(eventName);
        float textH = text.getHeight();
        float labelW = textW * 1.3f;
        float labelH = textH * 1.3f;
        float labelX = -labelW * .5f;
        float labelY = height - labelOffset * scaleY;

        borders.width = labelW / text.getScaleX();
        borders.x = -borders.width * .5f;

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

    public void getBorders(@NonNull Rect rect) {
        rect.set(borders.x, borders.y, borders.width, borders.height);
    }
}
