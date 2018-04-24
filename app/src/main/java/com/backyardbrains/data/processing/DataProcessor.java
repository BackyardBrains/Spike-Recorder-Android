package com.backyardbrains.data.processing;

import android.support.annotation.NonNull;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public interface DataProcessor {

    /**
     * Data holder class that hold samples, events and last sample index after processing.
     */
    class SamplesWithMarkers {
        public short[] samples;
        public String[] events;
        public long lastSampleIndex;

        public SamplesWithMarkers() {
            samples = new short[0];
            events = new String[0];
            this.lastSampleIndex = -1;
        }

        public SamplesWithMarkers(@NonNull String[] events) {
            this.samples = new short[0];
            this.events = events;
            this.lastSampleIndex = -1;
        }

        public SamplesWithMarkers(short[] samples, String[] events) {
            this.samples = samples;
            this.events = events;
            this.lastSampleIndex = -1;
        }

        public SamplesWithMarkers(short[] samples, String[] events, long lastSampleIndex) {
            this.samples = samples;
            this.events = events;
            this.lastSampleIndex = lastSampleIndex;
        }
    }

    /**
     * Takes incoming {@code byte[]}, processes it and returns array of samples. Additionally saves all the detected
     * events into the specified {@code events} collection.
     */
    @NonNull SamplesWithMarkers process(@NonNull byte[] data);
}
