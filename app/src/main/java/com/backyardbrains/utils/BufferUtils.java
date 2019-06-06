package com.backyardbrains.utils;

import com.backyardbrains.dsp.SignalProcessor;
import java.util.Arrays;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class BufferUtils {

    // Maximum buffer size for a buffer that holds samples
    private static final int MAX_SAMPLE_BUFFER_SIZE = SignalProcessor.DEFAULT_PLAYBACK_MAX_PROCESSED_SAMPLES_COUNT;
    // Maximum buffer size for a buffer that holds data bytes
    private static final int MAX_BYTE_SIZE = MAX_SAMPLE_BUFFER_SIZE * 2;
    private static final byte[] EMPTY_DATA_BUFFER = new byte[MAX_BYTE_SIZE];

    static {
        Arrays.fill(EMPTY_DATA_BUFFER, (byte) 0);
    }

    /**
     * Shifts all bytes to right for {@code offset} places and returns new, shifted array. New array is prepended with
     * zeros.
     */
    public static void shiftRight(byte[] buffer, int offset) {
        System.arraycopy(buffer, 0, buffer, offset, buffer.length - offset);
        System.arraycopy(EMPTY_DATA_BUFFER, 0, buffer, 0, offset);
    }

    /**
     * Empties the specified {@code buffer} of bytes by setting all values to {@code 0}.
     */
    public static void emptyBuffer(byte[] buffer) {
        System.arraycopy(EMPTY_DATA_BUFFER, 0, buffer, 0, buffer.length);
    }
}
