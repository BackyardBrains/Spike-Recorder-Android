package com.backyardbrains.data;

import android.support.annotation.NonNull;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public interface DataProcessor {

    /**
     * Takes incoming {@code byte[]}, processes it and returns array of samples.
     */
    short[] process(@NonNull byte[] data);
}
