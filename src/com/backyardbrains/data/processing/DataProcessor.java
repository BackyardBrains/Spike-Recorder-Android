package com.backyardbrains.data.processing;

import android.support.annotation.NonNull;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public interface DataProcessor {

    /**
     * Data holder class that hold samples and events after processing.
     */
    class Data {
        public short[] samples;
        public String[] events;

        public Data() {
            samples = new short[0];
            events = new String[0];
        }

        public Data(@NonNull short[] samples) {
            this.samples = samples;
            this.events = new String[0];
        }

        public Data(short[] samples, String[] events) {
            this.samples = samples;
            this.events = events;
        }
    }

    /**
     * Takes incoming {@code byte[]}, processes it and returns array of samples. Additionally saves all the detected
     * events into the specified {@code events} collection.
     */
    @NonNull Data process(@NonNull byte[] data);
}
