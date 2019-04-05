package com.backyardbrains.drawing;

import com.backyardbrains.dsp.SignalProcessor;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class FftDrawData {

    private static final int MAX_SEGMENTS =
        SignalProcessor.DEFAULT_FFT_WINDOW_COUNT * SignalProcessor.DEFAULT_FFT_30HZ_WINDOW_SIZE;
    private static final int MAX_VERTICES = MAX_SEGMENTS * 2;
    private static final int MAX_INDICES = MAX_SEGMENTS * 6;
    private static final int MAX_COLORS = MAX_SEGMENTS * 4;

    public float[] vertices;
    public short[] indices;
    public float[] colors;

    public int vertexCount;
    public int indexCount;
    public int colorCount;

    public FftDrawData() {
        vertices = new float[MAX_VERTICES];
        indices = new short[MAX_INDICES];
        colors = new float[MAX_COLORS];
        vertexCount = 0;
        indexCount = 0;
        colorCount = 0;
    }
}
