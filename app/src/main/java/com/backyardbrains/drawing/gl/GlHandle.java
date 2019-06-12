package com.backyardbrains.drawing.gl;

import android.support.annotation.NonNull;
import android.support.annotation.Size;
import com.backyardbrains.drawing.Colors;
import javax.microedition.khronos.opengles.GL10;

/**
 * Defines a visual representation of a handle that's used to set a threshold or position a waveform on screen.
 *
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class GlHandle {

    private static final int HORIZONTAL = 0;
    private static final int VERTICAL = 1;

    private final GlCircle outerCircle;
    private final GlTriangle triangle;

    private final Rect borders;

    public GlHandle() {
        outerCircle = new GlCircle();
        triangle = new GlTriangle();

        borders = new Rect();
    }

    public void draw(@NonNull GL10 gl, float baseRadius, boolean selected, @NonNull @Size(4) float[] color) {
        borders.set(0f, -baseRadius, baseRadius * 3f, baseRadius * 2f);

        gl.glPushMatrix();
        gl.glTranslatef(baseRadius, 0f, 0f);
        outerCircle.draw(gl, baseRadius, color);
        gl.glPopMatrix();

        gl.glPushMatrix();
        gl.glTranslatef(baseRadius * .35f, 0f, 0f);
        triangle.draw(gl, baseRadius, -baseRadius + .95f, baseRadius * 3f, 0f, baseRadius, baseRadius * 0.95f, color);
        gl.glPopMatrix();

        if (!selected) {
            gl.glPushMatrix();
            gl.glTranslatef(baseRadius, 0f, 0f);
            outerCircle.draw(gl, baseRadius * .8f, Colors.BLACK);
            gl.glPopMatrix();
        }
    }

    public void getBorders(@NonNull Rect rect) {
        rect.set(borders.x, borders.y, borders.width, borders.height);
    }
}
