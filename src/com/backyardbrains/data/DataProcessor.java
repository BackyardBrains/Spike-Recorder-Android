package com.backyardbrains.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.nio.ByteBuffer;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public interface DataProcessor {

    /**
     * Takes incoming {@code data}, processes it and returns array of samples.
     */
    @Nullable short[] processData(@NonNull ByteBuffer data);
}
