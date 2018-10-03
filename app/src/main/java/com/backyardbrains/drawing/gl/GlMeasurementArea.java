package com.backyardbrains.drawing.gl;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class GlMeasurementArea {

    private static final float[] LIMIT_LINE_COLOR = new float[] { 0.8f, 0.8f, 0.8f, 1f };
    private static final float[] AREA_COLOR = new float[] { 0.4f, 0.4f, 0.4f, 0.5f };

    private static final int LIMIT_LINES_WIDTH = 2;

    private final GlVLine line;
    private final GlRectangle area;

    public GlMeasurementArea() {
        line = new GlVLine();
        area = new GlRectangle();
    }

    public void draw(GL10 gl, float x0, float x1, float y0, float y1) {
        // draw limit lines
        line.draw(gl, x0, y0, y1, LIMIT_LINES_WIDTH, LIMIT_LINE_COLOR);
        line.draw(gl, x1, y0, y1, LIMIT_LINES_WIDTH, LIMIT_LINE_COLOR);

        // draw measurement area
        area.draw(gl, x0, y0, x1 - x0, y1 - y0, AREA_COLOR);
    }
}
