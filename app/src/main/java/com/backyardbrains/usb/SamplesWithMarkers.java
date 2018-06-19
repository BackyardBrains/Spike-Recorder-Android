package com.backyardbrains.usb;

import android.support.annotation.NonNull;

/**
 * Data holder class that hold samples, events and last sample index after processing.
 *
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class SamplesWithMarkers {

    public short[] samples;
    public int[] eventIndices;
    public String[] eventLabels;
    //public String[] events;
    public long lastSampleIndex;

    public SamplesWithMarkers() {
        this.samples = new short[0];
        this.eventIndices = new int[0];
        this.eventLabels = new String[0];
        //this.events = new String[0];
        this.lastSampleIndex = -1;
    }

    public SamplesWithMarkers(int[] eventIndices, @NonNull String[] eventLabels) {
        this.samples = new short[0];
        this.eventIndices = eventIndices;
        this.eventLabels = eventLabels;
        //this.events = eventLabels;
        this.lastSampleIndex = -1;
    }

    public SamplesWithMarkers(short[] samples, int[] eventIndices, String[] eventLabels) {
        this.samples = samples;
        this.eventIndices = eventIndices;
        this.eventLabels = eventLabels;
        //this.events = eventLabels;
        this.lastSampleIndex = -1;
    }

    public SamplesWithMarkers(short[] samples, int[] eventIndices, String[] eventLabels, long lastSampleIndex) {
        this.samples = samples;
        this.eventIndices = eventIndices;
        this.eventLabels = eventLabels;
        //this.events = eventLabels;
        this.lastSampleIndex = lastSampleIndex;
    }
}
