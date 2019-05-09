package com.backyardbrains.drawing;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class FftDrawData {

    public float[] vertices;
    public short[] indices;
    public float[] colors;

    public int vertexCount;
    public int indexCount;
    public int colorCount;

    public float scaleX;
    public float scaleY;

    public FftDrawData(int maxSegments) {
        vertices = new float[maxSegments * 2];
        indices = new short[maxSegments * 6];
        colors = new float[maxSegments * 4];
        vertexCount = 0;
        indexCount = 0;
        colorCount = 0;
    }
}
