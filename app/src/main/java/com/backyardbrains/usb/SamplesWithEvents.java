package com.backyardbrains.usb;

import android.support.annotation.NonNull;

/**
 * Data holder class that hold samples, events and last sample index after processing.
 *
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class SamplesWithEvents {

    public short[] samples;
    public int[] eventIndices;
    public String[] eventLabels;
    public long lastSampleIndex;

    // CURRENTLY NOT USED CAUSE SAMPLE STREAM IS PROCESSED IN C++
    public SamplesWithEvents() {
        this.samples = new short[0];
        this.eventIndices = new int[0];
        this.eventLabels = new String[0];
        this.lastSampleIndex = -1;
    }

    // TEMPORARILY USED UNTIL SAMPLE SOURCE IS CREATED FOR AUDIO DATA
    public SamplesWithEvents(int[] eventIndices, @NonNull String[] eventLabels) {
        this.samples = new short[0];
        this.eventIndices = eventIndices;
        this.eventLabels = eventLabels;
        this.lastSampleIndex = -1;
    }

    // USED FROM C++
    public SamplesWithEvents(short[] samples, int[] eventIndices, String[] eventLabels) {
        this.samples = samples;
        this.eventIndices = eventIndices;
        this.eventLabels = eventLabels;
        this.lastSampleIndex = -1;
    }

    // USED FROM PLAYBACK SAMPLE SOURCE
    public SamplesWithEvents(short[] samples, int[] eventIndices, String[] eventLabels, long lastSampleIndex) {
        this.samples = samples;
        this.eventIndices = eventIndices;
        this.eventLabels = eventLabels;
        this.lastSampleIndex = lastSampleIndex;
    }
}
