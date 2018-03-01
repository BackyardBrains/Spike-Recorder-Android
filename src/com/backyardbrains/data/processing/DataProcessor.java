package com.backyardbrains.data.processing;

import android.support.annotation.NonNull;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public interface DataProcessor {

    /**
     * Takes incoming {@code byte[]}, processes it and returns array of samples.
     */
    @NonNull short[] process(@NonNull byte[] data);
}
