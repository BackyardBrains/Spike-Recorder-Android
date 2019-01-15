package com.backyardbrains.drawing.gl;

/**
 * Defines a visual representation of dashed line
 *
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class GlDashedVLine extends GlDashedLine {

    @Override protected void getDashedLineVertices(float[] vertices, float c1, float c2, float dashSize) {
        int counter = 0;
        int start = (int) Math.min(c1, c2);
        int end = (int) Math.max(c1, c2);
        if ((end - start) / dashSize > vertices.length) dashSize = (end - start) / dashSize;
        for (int i = start; i < end; i += dashSize * 2) {
            vertices[counter++] = 0f;
            vertices[counter++] = i;
            vertices[counter++] = 0f;
            vertices[counter++] = i + dashSize;
        }
    }
}
