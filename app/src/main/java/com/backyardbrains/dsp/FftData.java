package com.backyardbrains.dsp;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class FftData {

    public int maxWindowCount;
    public int maxThirtyHzDataSize;

    public float[][] fft;
    public int windowCount;
    public int thirtyHzDataSize;

    public FftData(int maxWindowCount, int maxThirtyHzDataSize) {
        this.maxWindowCount = maxWindowCount;
        this.maxThirtyHzDataSize = maxThirtyHzDataSize;

        fft = new float[maxWindowCount][];
        for (int i = 0; i < maxWindowCount; i++) {
            fft[i] = new float[maxThirtyHzDataSize];
        }
        windowCount = 0;
        thirtyHzDataSize = 0;
    }
}
