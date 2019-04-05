package com.backyardbrains.dsp;

import android.support.annotation.NonNull;
import com.backyardbrains.utils.EventUtils;

/**
 * Data holder class that hold samples, events and last sample index after processing.
 *
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class SignalData {

    private static final int EVENT_BUFFER_SIZE = EventUtils.MAX_EVENT_COUNT;

    public int channelCount;
    public int maxSamplesPerChannel;

    public short[][] samples;
    public int[] sampleCounts;
    public int[] eventIndices;
    public String[] eventNames;
    public int eventCount;
    public long lastSampleIndex = -1;

    public SignalData(int channelCount, int maxSamplesPerChannel) {
        this.channelCount = channelCount;
        this.maxSamplesPerChannel = maxSamplesPerChannel;

        this.samples = new short[channelCount][];
        for (int i = 0; i < channelCount; i++) {
            this.samples[i] = new short[maxSamplesPerChannel];
        }
        this.sampleCounts = new int[channelCount];
        this.eventIndices = new int[EVENT_BUFFER_SIZE];
        this.eventNames = new String[EVENT_BUFFER_SIZE];
        this.eventCount = 0;
    }

    void copyReconfigured(@NonNull SignalData signalData, @NonNull SignalConfiguration signalConfiguration) {
        // we cannot copy if channel counts are not same
        if (signalConfiguration.getChannelCount() != channelCount) return;

        int counter = 0;
        for (int i = 0; i < channelCount; i++) {
            if (signalConfiguration.isChannelVisible(i)) {
                signalData.samples[counter] = samples[i];
                signalData.sampleCounts[counter++] = sampleCounts[i];
            }
        }
        signalData.eventIndices = eventIndices;
        signalData.eventNames = eventNames;
        signalData.eventCount = eventCount;
        signalData.lastSampleIndex = lastSampleIndex;
    }
}
