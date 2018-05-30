package com.backyardbrains.data.processing;

import android.support.annotation.NonNull;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public interface DataProcessor {

    /**
     * Data holder class that hold samples, events and last sample index after processing.
     */
    class SamplesWithMarkers {
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

    /**
     * Takes incoming {@code byte[]}, processes it and returns array of samples. Additionally saves all the detected
     * events into the specified {@code events} collection.
     */
    @NonNull SamplesWithMarkers process(@NonNull byte[] data);
}
