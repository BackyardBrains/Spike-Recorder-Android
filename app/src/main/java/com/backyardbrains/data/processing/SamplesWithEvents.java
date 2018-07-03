package com.backyardbrains.data.processing;

import android.support.annotation.NonNull;
import com.backyardbrains.utils.EventUtils;

/**
 * Data holder class that hold samples, events and last sample index after processing.
 *
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class SamplesWithEvents {

    private static final int SAMPLE_BUFFER_SIZE = 5000;
    private static final int EVENT_BUFFER_SIZE = EventUtils.MAX_EVENT_COUNT;

    public short[] samples;
    public int sampleCount;
    public int[] eventIndices;
    public String[] eventNames;
    public int eventCount;
    public long lastSampleIndex = -1;

    public SamplesWithEvents(byte fake) {
        this.samples = new short[SAMPLE_BUFFER_SIZE];
        this.eventIndices = new int[EVENT_BUFFER_SIZE];
        this.eventNames = new String[EVENT_BUFFER_SIZE];
    }

    public SamplesWithEvents(int sampleBufferSize) {
        this.samples = new short[sampleBufferSize];
        this.eventIndices = new int[EVENT_BUFFER_SIZE];
        this.eventNames = new String[EVENT_BUFFER_SIZE];
    }

    public void setAll(@NonNull SamplesWithEvents samplesWithEvents) {
        setAll(samplesWithEvents.samples, samplesWithEvents.sampleCount, samplesWithEvents.eventIndices,
            samplesWithEvents.eventNames, samplesWithEvents.eventCount, samplesWithEvents.lastSampleIndex);
    }

    public void setAll(short[] samples, int sampleCount, int[] eventIndices, String[] eventNames, int eventCount,
        long lastSampleIndex) {
        System.arraycopy(samples, 0, this.samples, 0, sampleCount);
        System.arraycopy(eventIndices, 0, this.eventIndices, 0, eventCount);
        System.arraycopy(eventNames, 0, this.eventNames, 0, eventCount);

        this.sampleCount = sampleCount;
        this.eventCount = eventCount;
        this.lastSampleIndex = lastSampleIndex;
    }

    // USED FROM PLAYBACK SAMPLE SOURCE
    public SamplesWithEvents() {
        this.samples = new short[0];
        this.eventIndices = new int[0];
        this.eventNames = new String[0];
    }

    // USED FROM C++
    public SamplesWithEvents(short[] samples, int[] eventIndices, String[] eventNames) {
        this.samples = samples;
        this.eventIndices = eventIndices;
        this.eventNames = eventNames;
    }
}
