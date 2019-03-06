package com.backyardbrains.utils;

import org.greenrobot.essentials.io.CircularByteBuffer;

/**
 * A circular float array buffer (also called ring buffer) allows putting and getting float arrays in a FIFO way.
 * Typical use cases are (usually concurrent/asynchronous) producers and consumers operating on float arrays. This
 * enables building a multi-threaded processing pipeline.
 * <p/>
 * All put&get methods are non-blocking.
 * <p/>
 * This class is thread-safe.
 * <p/>
 * The class is written in reference to greenrobot's {@link CircularByteBuffer} class.
 *
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class CircularFloatArrayBuffer {
    private final float[][] buffer;
    private final int wCapacity;

    private int available;
    private int idxGet;
    private int idxPut;

    public CircularFloatArrayBuffer() {
        this(8192, 8192);
    }

    public CircularFloatArrayBuffer(int wCapacity, int hCapacity) {
        this.wCapacity = wCapacity;
        buffer = new float[wCapacity][hCapacity];
    }

    /**
     * Clears all data from the buffer.
     */
    public synchronized void clear() {
        idxGet = idxPut = available = 0;
    }

    /**
     * Gets as many of the requested float arrays as available from this buffer.
     *
     * @return number of float arrays actually got from this buffer (0 if no float arrays are available)
     */
    public int get(float[][] dst) {
        return get(dst, 0, dst.length);
    }

    /**
     * Gets as many of the requested float  arrays as available from this buffer.
     *
     * @return number of float arrays actually got from this buffer (0 if no float arrays are available)
     */
    public synchronized int get(float[][] dst, int off, int len) {
        if (available == 0) return 0;

        // limit is last index to read + 1
        int limit = idxGet < idxPut ? idxPut : wCapacity;
        int count = Math.min(limit - idxGet, len);
        copy(buffer, idxGet, dst, off, count);
        idxGet += count;

        if (idxGet == wCapacity) {
            // Array end reached, check if we have more
            int count2 = Math.min(len - count, idxPut);
            if (count2 > 0) {
                copy(buffer, 0, dst, off + count, count2);
                idxGet = count2;
                count += count2;
            } else {
                idxGet = 0;
            }
        }
        available -= count;
        return count;
    }

    /**
     * Puts as many of the given float arrays as possible into this buffer.
     *
     * @return number of float arrays actually put into this buffer (0 if the buffer is full)
     */
    public synchronized int put(float[][] src, int off, int wLen) {
        if (available == wCapacity) return 0;

        // limit is last index to put + 1
        int limit = idxPut < idxGet ? idxGet : wCapacity;
        int count = Math.min(limit - idxPut, wLen);
        copy(src, off, buffer, idxPut, count);
        idxPut += count;

        if (idxPut == wCapacity) {
            // Array end reached, check if we have more
            int count2 = Math.min(wLen - count, idxGet);
            if (count2 > 0) {
                copy(src, off + count, buffer, 0, count2);
                idxPut = count2;
                count += count2;
            } else {
                idxPut = 0;
            }
        }
        available += count;
        return count;
    }

    private void copy(float[][] src, int srcPos, float[][] dst, int dstPos, int wLength) {
        int dstCounter = 0;
        for (int i = srcPos; i < srcPos + wLength; i++) {
            System.arraycopy(src[i], 0, dst[dstPos + dstCounter], 0,
                Math.min(src[i].length, dst[dstPos + dstCounter].length));
            dstCounter++;
        }
    }
}