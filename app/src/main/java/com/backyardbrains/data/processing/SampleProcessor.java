package com.backyardbrains.data.processing;

import android.support.annotation.NonNull;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public interface SampleProcessor {

    /**
     * Takes incoming {@code samples} and returns them back processed.
     */
    @NonNull short[] process(@NonNull short[] samples, int length);
}
