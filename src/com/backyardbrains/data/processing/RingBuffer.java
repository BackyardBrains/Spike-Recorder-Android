/*
 * Backyard Brains Android App
 * Copyright (C) 2011 Backyard Brains
 * by Nathan Dotz <nate (at) backyardbrains.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.backyardbrains.data.processing;

import com.crashlytics.android.Crashlytics;
import java.lang.reflect.Array;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class RingBuffer<T> {

    private static final String TAG = makeLogTag(RingBuffer.class);

    private final Class<T> clazz;
    private final int size;

    private T[] buffer;

    public RingBuffer(Class<T> clazz, int size) {
        this.clazz = clazz;
        this.size = size;

        //noinspection unchecked
        buffer = (T[]) Array.newInstance(clazz, size);
    }

    /**
     * Returns size of this ring buffer.
     */
    public int getSize() {
        return size;
    }

    public void add(T[] incoming) {
        try {
            System.arraycopy(buffer, incoming.length, buffer, 0, buffer.length - incoming.length);
            System.arraycopy(incoming, 0, buffer, buffer.length - incoming.length, incoming.length);
        } catch (Exception e) {
            LOGD(TAG, "Can't add incoming to buffer, it's larger then buffer - src.length=" + buffer.length + " srcPos="
                + incoming.length + " dst.length=" + buffer.length + " dstPos=" + 0 + " length=" + (buffer.length
                - incoming.length));
            Crashlytics.logException(e);
        }
    }

    public void add(T incoming) {
        System.arraycopy(buffer, 1, buffer, 0, buffer.length - 1);
        buffer[buffer.length - 1] = incoming;
    }

    /**
     * @return an order-adjusted version of the whole buffer
     */
    public T[] getArray() {
        return buffer;
    }

    /**
     * Clears the buffer as sets all values to zeros
     */
    public void clear() {
        //noinspection unchecked
        buffer = (T[]) Array.newInstance(clazz, size);
    }
}
