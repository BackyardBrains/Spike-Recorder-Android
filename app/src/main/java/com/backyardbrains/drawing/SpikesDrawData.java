package com.backyardbrains.drawing;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class SpikesDrawData {

    public float[] vertices;
    public float[] colors;

    public int vertexCount;
    public int colorCount;

    public SpikesDrawData(int maxSpikes) {
        vertices = new float[maxSpikes * 2];
        colors = new float[maxSpikes * 4];
        vertexCount = 0;
        colorCount = 0;
    }
}
