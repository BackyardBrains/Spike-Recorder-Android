package com.backyardbrains.data.processing;

import android.support.annotation.NonNull;
import com.backyardbrains.usb.SamplesWithMarkers;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public interface DataProcessor {
    /**
     * Takes incoming {@code byte[]}, processes it and returns array of samples. Additionally saves all the detected
     * events into the specified {@code events} collection.
     */
    @NonNull SamplesWithMarkers process(@NonNull byte[] data);
}
