package com.backyardbrains.dsp;

import com.backyardbrains.utils.EventUtils;

/**
 * Data holder class that hold samples, events and last sample index after processing.
 *
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class SamplesWithEvents {

    private static final int EVENT_BUFFER_SIZE = EventUtils.MAX_EVENT_COUNT;

    public int channelCount;
    public int maxSamplesPerChannel;

    public short[] samples;
    public int sampleCount;
    public short[][] samplesM;
    public int[] sampleCountM;
    public int[] eventIndices;
    public String[] eventNames;
    public int eventCount;
    public long lastSampleIndex = -1;

    public SamplesWithEvents(int channelCount, int maxSamplesPerChannel) {
        this.channelCount = channelCount;
        this.maxSamplesPerChannel = maxSamplesPerChannel;

        this.samples = new short[channelCount * maxSamplesPerChannel];
        this.sampleCount = 0;
        this.samplesM = new short[channelCount][];
        for (int i = 0; i < channelCount; i++) {
            this.samplesM[i] = new short[maxSamplesPerChannel];
        }
        this.sampleCountM = new int[channelCount];
        this.eventIndices = new int[EVENT_BUFFER_SIZE];
        this.eventNames = new String[EVENT_BUFFER_SIZE];
        this.eventCount = 0;
    }
}
