package com.backyardbrains.dsp;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class FftData {

    public int maxWindowCount;
    public int maxWindowSize;

    public float[][] fft;
    public int windowCount;
    public int windowSize;

    public FftData(int maxWindowCount, int maxWindowSize) {
        this.maxWindowCount = maxWindowCount;
        this.maxWindowSize = maxWindowSize;

        fft = new float[maxWindowCount][];
        for (int i = 0; i < maxWindowCount; i++) {
            fft[i] = new float[maxWindowSize];
        }
        windowCount = 0;
        windowSize = 0;
    }
}
