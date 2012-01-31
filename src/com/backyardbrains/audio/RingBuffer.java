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

	public void addEnd(short r) {
		if (end >= 0) {
			// not empty!
			++end;
			if (end >= buffer.length)
				end = 0;
			if (end == beginning)
				beginning++;
			if (beginning >= buffer.length)
				beginning = 0;
		} else {
			beginning = end = 0;
		}
		buffer[end] = r;
	}

	public void add(ByteBuffer incoming) {
		/*
		byte swp;
		while (buffer.hasRemaining()) {
			swp = buffer.get();
			addEnd(buffer.get());
			addEnd(swp);
		}
		*/
		incoming.clear();
		ShortBuffer sb = incoming.asShortBuffer();
		while (sb.hasRemaining()) {
			addEnd(sb.get());
		}
	}

	/**
	 * return an order-adjusted version of the whole buffer
	 * 
	 * @return
	 */
	public short[] getArray() {
		if (beginning == 0) {
			short[] returnArray = new short[end + 1];
			System.arraycopy(buffer, 0, returnArray, 0, returnArray.length);
			return returnArray;
		}
		short[] returnArray = new short[buffer.length];
		System.arraycopy(buffer, beginning, returnArray, 0, buffer.length
				- beginning);
		System.arraycopy(buffer, 0, returnArray, buffer.length - beginning,
				end + 1);
		return returnArray;
	}

	public void zeroFill() {
		for (int i = 0; i < buffer.length; i++) {
			addEnd((short) 0);
		}

	}
}
