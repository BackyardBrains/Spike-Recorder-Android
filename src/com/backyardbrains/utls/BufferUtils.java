package com.backyardbrains.utls;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class BufferUtils {

    /**
     * Shifts all bytes to right for {@code offset} places and returns new, shifted array. New array is prepended with
     * zeros.
     */
    public static byte[] shift(byte[] buffer, int offset) {
        System.arraycopy(buffer, 0, buffer, offset, buffer.length - offset);
        System.arraycopy(new byte[offset], 0, buffer, 0, offset);
        return buffer;
    }
}
