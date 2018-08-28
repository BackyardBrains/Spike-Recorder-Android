package com.backyardbrains.data.processing;

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

    public SamplesWithEvents() {
        this.samples = new short[SAMPLE_BUFFER_SIZE];
        this.eventIndices = new int[EVENT_BUFFER_SIZE];
        this.eventNames = new String[EVENT_BUFFER_SIZE];
    }

    public SamplesWithEvents(int sampleBufferSize) {
        this.samples = new short[sampleBufferSize];
        this.eventIndices = new int[EVENT_BUFFER_SIZE];
        this.eventNames = new String[EVENT_BUFFER_SIZE];
    }

    // USED FROM C++
    public SamplesWithEvents(short[] samples, int[] eventIndices, String[] eventNames) {
        this.samples = samples;
        this.eventIndices = eventIndices;
        this.eventNames = eventNames;
    }

    // USED FROM SampleStreamProcessor
    public SamplesWithEvents(short[] samples, int sampleCount, int[] eventIndices, String[] eventNames,
        int eventCount) {
        this.samples = samples;
        this.eventIndices = eventIndices;
        this.eventNames = eventNames;

        this.sampleCount = sampleCount;
        this.eventCount = eventCount;
    }
}
