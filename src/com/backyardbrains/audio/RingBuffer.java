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
import java.nio.ShortBuffer;

public class RingBuffer {
	private short[] buffer;
	private int beginning;

	/**
	 * @return the beginning
	 */
	public int getBeginning() {
		return beginning;
	}

	/**
	 * @return the end
	 */
	public int getEnd() {
		return end;
	}

	private int end;

	public RingBuffer(int size) {
		buffer = new short[size];
		beginning = end = -1;
	}

	public void add(final ByteBuffer incoming) {
		incoming.clear();
		final ShortBuffer sb = incoming.asShortBuffer();
		System.arraycopy(buffer, sb.capacity(), buffer, 0, buffer.length-sb.capacity());
		sb.get(buffer, buffer.length-sb.capacity(), sb.capacity());
		//System.arraycopy(s, 0, buffer, buffer.length-s.length, s.length);
	}
	public void add(final ShortBuffer incoming) {
		incoming.clear();
		System.arraycopy(buffer, incoming.capacity(), buffer, 0, buffer.length-incoming.capacity());
		incoming.get(buffer, buffer.length-incoming.capacity(), incoming.capacity());
		//System.arraycopy(s, 0, buffer, buffer.length-s.length, s.length);
	}
	/**
	 * return an order-adjusted version of the whole buffer
	 * 
	 * @return
	 */
	public short[] getArray() {
		return buffer;
	}

	public void zeroFill() {
	}
}
