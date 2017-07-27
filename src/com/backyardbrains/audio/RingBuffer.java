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

package com.backyardbrains.audio;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

public class RingBuffer {

    private final int size;

    private short[] buffer;

    public RingBuffer(int size) {
        this.size = size;

        buffer = new short[size];
    }

    public void add(final ByteBuffer incoming) {
        add(incoming.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer());
    }

    public void add(final ShortBuffer incoming) {
        incoming.clear();

        System.arraycopy(buffer, incoming.capacity(), buffer, 0, buffer.length - incoming.capacity());
        incoming.get(buffer, buffer.length - incoming.capacity(), incoming.capacity());
    }

    public void add(final short[] incoming) {
        System.arraycopy(buffer, incoming.length, buffer, 0, buffer.length - incoming.length);
        System.arraycopy(incoming, 0, buffer, buffer.length - incoming.length, incoming.length);
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
