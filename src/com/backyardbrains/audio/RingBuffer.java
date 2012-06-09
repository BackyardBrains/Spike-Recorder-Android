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
