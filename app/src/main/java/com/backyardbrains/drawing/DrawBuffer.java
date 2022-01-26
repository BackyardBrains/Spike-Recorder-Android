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

package com.backyardbrains.drawing;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class DrawBuffer {

    private static final String TAG = makeLogTag(DrawBuffer.class);

    private final int size;

    private byte[] buffer;

    public DrawBuffer(int size) {
        this.size = size;

        buffer = new byte[size];
    }

    public int getSize() {
        return size;
    }

    /**
     * Adds new {@code incoming} samples to the buffer.
     */
    public void put(byte[] src, int len) {
        try {
            System.arraycopy(buffer, len, buffer, 0, buffer.length - len);
            System.arraycopy(src, 0, buffer, buffer.length - len, len);
        } catch (Exception e) {
            LOGD(TAG,
                "Can't add incoming to buffer, it's larger then buffer - src.length=" + buffer.length + " srcPos=" + len
                    + " dst.length=" + buffer.length + " dstPos=" + 0 + " length=" + (buffer.length - len));
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    public int get(byte[] dst, int off, int len) {
        try {
            System.arraycopy(buffer, off, dst, 0, len);
            return len;
        } catch (Exception e) {
            LOGD(TAG, "Can't copy from buffer to destination - src.length=" + buffer.length + " srcPos=" + off
                + " dst.length=" + dst.length + " dstPos=" + 0 + " length=" + len);
            FirebaseCrashlytics.getInstance().recordException(e);
        }

        return 0;
    }

    /**
     * @return an order-adjusted version of the whole buffer
     */
    public byte[] getArray() {
        return buffer;
    }

    /**
     * Clears the buffer as sets all values to zeros
     */
    public void clear() {
        buffer = new byte[size];
    }
}
