package com.backyardbrains.drawing;

/**
 * Data holder class that hold samples, events and last sample index after processing.
 *
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class SignalDrawData {

    public int channelCount;
    public int maxSamplesPerChannel;

    public short[][] samples;
    public int[] sampleCounts;

    public SignalDrawData(int channelCount, int maxSamplesPerChannel) {
        this.channelCount = channelCount;
        this.maxSamplesPerChannel = maxSamplesPerChannel;

        this.samples = new short[channelCount][];
        for (int i = 0; i < channelCount; i++) {
            this.samples[i] = new short[maxSamplesPerChannel];
        }
        this.sampleCounts = new int[channelCount];
    }
}
