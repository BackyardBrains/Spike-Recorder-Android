package com.backyardbrains.drawing.gl;

import androidx.annotation.NonNull;
import javax.microedition.khronos.opengles.GL10;

/**
 * Defines a visual representation of a vertical line
 *
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class GlVLine extends GlLine {

    public void draw(@NonNull GL10 gl, float y1, float y2, int lineWidth, @NonNull float[] color) {
        super.draw(gl, 0f, y1, 0f, y2, lineWidth, color);
    }
}
