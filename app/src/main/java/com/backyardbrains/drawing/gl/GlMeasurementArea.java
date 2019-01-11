package com.backyardbrains.drawing.gl;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class GlMeasurementArea {

    private static final int LIMIT_LINES_WIDTH = 2;

    private final GlVLine line;
    private final GlRectangle area;

    public GlMeasurementArea() {
        line = new GlVLine();
        area = new GlRectangle();
    }

    public void draw(GL10 gl, float width, float height, float[] limitLineColor, float[] areaColor) {
        // draw left limit line
        line.draw(gl, 0f, height, LIMIT_LINES_WIDTH, limitLineColor);
        // draw right limit line
        gl.glPushMatrix();
        gl.glTranslatef(width, 0f, 0f);
        line.draw(gl, 0f, height, LIMIT_LINES_WIDTH, limitLineColor);
        gl.glPopMatrix();

        // draw measurement area
        area.draw(gl, 0f, 0f, width, height, areaColor);
    }
}
