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

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class SampleBuffer {

    private static final String TAG = makeLogTag(SampleBuffer.class);

    private final int size;

    private short[] buffer;

    SampleBuffer(int size) {
        this.size = size;

        buffer = new short[size];
    }

    /**
     * Adds new {@code incoming} samples to the buffer.
     */
    public void add(short[] incoming) {
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

    /**
     * @return an order-adjusted version of the whole buffer
     */
    public short[] getArray() {
        return buffer;
    }

    /**
     * Clears the buffer as sets all values to zeros
     */
    public void clear() {
        buffer = new short[size];
    }
}
