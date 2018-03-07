package com.backyardbrains.data.processing;

import android.support.annotation.NonNull;
import android.util.SparseArray;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public interface DataProcessor {

    /**
     * Takes incoming {@code byte[]}, processes it and returns array of samples. Additionally saves all the detected
     * events into the specified {@code events} collection.
     */
    @NonNull short[] process(@NonNull byte[] data, @NonNull SparseArray<String> events);
}
