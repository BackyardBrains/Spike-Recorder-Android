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

    private static final float OUTER_CIRCLE_RADIUS = 30f;
    private static final float INNER_CIRCLE_RADIUS = OUTER_CIRCLE_RADIUS * .8f;
    private static final float OFFSET_X = .35f * OUTER_CIRCLE_RADIUS;
    private static final float OFFSET_Y = .05f * OUTER_CIRCLE_RADIUS;

    private final GlCircle outerCircle;
    private final GlTriangle triangle;

    private final Rect borders;

    public GlHandle() {
        outerCircle = new GlCircle();
        triangle = new GlTriangle();

        borders = new Rect();
    }

    public void draw(@NonNull GL10 gl, @NonNull @Size(4) float[] color, boolean selected) {
        borders.set(0f, -OUTER_CIRCLE_RADIUS, OUTER_CIRCLE_RADIUS * 3f, OUTER_CIRCLE_RADIUS * 2f);

        gl.glPushMatrix();
        gl.glTranslatef(OUTER_CIRCLE_RADIUS, 0f, 0f);
        outerCircle.draw(gl, OUTER_CIRCLE_RADIUS, color);
        gl.glPopMatrix();

        gl.glPushMatrix();
        gl.glTranslatef(OFFSET_X, 0f, 0f);
        triangle.draw(gl, OUTER_CIRCLE_RADIUS, -OUTER_CIRCLE_RADIUS + OFFSET_Y, OUTER_CIRCLE_RADIUS * 3f, 0f,
            OUTER_CIRCLE_RADIUS, OUTER_CIRCLE_RADIUS - OFFSET_Y, color);
        gl.glPopMatrix();

        if (!selected) {
            gl.glPushMatrix();
            gl.glTranslatef(OUTER_CIRCLE_RADIUS, 0f, 0f);
            outerCircle.draw(gl, INNER_CIRCLE_RADIUS, Colors.BLACK);
            gl.glPopMatrix();
        }
    }

    public void getBorders(@NonNull Rect rect) {
        rect.set(borders.x, borders.y, borders.width, borders.height);
    }
}
