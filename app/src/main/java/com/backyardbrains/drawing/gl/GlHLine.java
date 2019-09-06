package com.backyardbrains.drawing.gl;

import androidx.annotation.NonNull;
import javax.microedition.khronos.opengles.GL10;

/**
 * Defines a visual representation of a horizontal line
 *
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class GlHLine extends GlLine {

    public void draw(@NonNull GL10 gl, float x1, float x2, int lineWidth, @NonNull float[] color) {
        super.draw(gl, x1, 0f, x2, 0f, lineWidth, color);
    }
}
