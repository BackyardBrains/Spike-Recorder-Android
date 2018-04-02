package com.backyardbrains.utils;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class BufferUtils {

    // Maximum number of bytes that can be shifter is 6 seconds of audio data in bytes
    private static final int MAX_BUFFER_SIZE = AudioUtils.SAMPLE_RATE * 6 * 2;
    @SuppressWarnings("MismatchedReadAndWriteOfArray") private static final byte[] EMPTY_BUFFER =
        new byte[MAX_BUFFER_SIZE];

    /**
     * Shifts all bytes to right for {@code offset} places and returns new, shifted array. New array is prepended with
     * zeros.
     */
    public static byte[] shiftRight(byte[] buffer, int offset) {
        System.arraycopy(buffer, 0, buffer, offset, buffer.length - offset);
        System.arraycopy(EMPTY_BUFFER, 0, buffer, 0, offset);
        return buffer;
    }
}
