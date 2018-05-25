package com.backyardbrains.utils;

import java.util.Arrays;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class BufferUtils {

    // Maximum buffer size for a buffer that holds samples
    private static final int MAX_SAMPLE_BUFFER_SIZE = AudioUtils.SAMPLE_RATE * 6;
    // Maximum buffer size for a buffer that holds data bytes
    private static final int MAX_BYTE_SIZE = MAX_SAMPLE_BUFFER_SIZE * 2;
    private static final byte[] EMPTY_DATA_BUFFER = new byte[MAX_BYTE_SIZE];
    private static final short[] EMPTY_SHORT_BUFFER = new short[MAX_SAMPLE_BUFFER_SIZE];
    private static final int[] EMPTY_INT_BUFFER = new int[MAX_SAMPLE_BUFFER_SIZE];
    private static final String[] EMPTY_STRING_BUFFER = new String[MAX_SAMPLE_BUFFER_SIZE];

    static {
        Arrays.fill(EMPTY_DATA_BUFFER, (byte) 0);
        Arrays.fill(EMPTY_SHORT_BUFFER, (short) 0);
        Arrays.fill(EMPTY_INT_BUFFER, 0);
        Arrays.fill(EMPTY_STRING_BUFFER, null);
    }

    /**
     * Shifts all bytes to right for {@code offset} places and returns new, shifted array. New array is prepended with
     * zeros.
     */
    public static byte[] shiftRight(byte[] buffer, int offset) {
        System.arraycopy(buffer, 0, buffer, offset, buffer.length - offset);
        System.arraycopy(EMPTY_DATA_BUFFER, 0, buffer, 0, offset);
        return buffer;
    }

    /**
     * Empties the specified {@code buffer} of shorts by setting all values to {@code 0}.
     */
    public static void emptyShortBuffer(short[] buffer) {
        System.arraycopy(EMPTY_SHORT_BUFFER, 0, buffer, 0, buffer.length);
    }

    /**
     * Empties the specified {@code buffer} of ints by setting all values to {@code 0}.
     */
    public static void emptyIntBuffer(int[] buffer) {
        System.arraycopy(EMPTY_INT_BUFFER, 0, buffer, 0, buffer.length);
    }

    /**
     * Empties the specified {@code buffer} strings by setting all values to {@code null}.
     */
    public static void emptyStringBuffer(String[] buffer) {
        System.arraycopy(EMPTY_STRING_BUFFER, 0, buffer, 0, buffer.length);
    }
}
