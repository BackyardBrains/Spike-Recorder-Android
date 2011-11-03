package com.backyardbrains.audio;

import java.nio.ByteBuffer;

public class RingBuffer {
	private byte[] buffer;
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
		buffer = new byte[size];
		beginning = end = -1;
	}

	public void addEnd(byte b) {
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
		buffer[end] = b;
	}

	public void add(ByteBuffer buffer) {
		byte swp;
		while (buffer.hasRemaining()) {
			swp = buffer.get();
			addEnd(buffer.get());
			addEnd(swp);
		}
	}

	/**
	 * return an order-adjusted version of the whole buffer
	 * 
	 * @return
	 */
	public byte[] getArray() {
		if (beginning == 0) {
			byte[] returnArray = new byte[end + 1];
			System.arraycopy(buffer, 0, returnArray, 0, returnArray.length);
			return returnArray;
		}
		byte[] returnArray = new byte[buffer.length];
		System.arraycopy(buffer, beginning, returnArray, 0, buffer.length
				- beginning);
		System.arraycopy(buffer, 0, returnArray, buffer.length - beginning,
				end + 1);
		return returnArray;
	}

	public void zeroFill() {
		for (int i = 0; i < buffer.length; i++) {
			addEnd((byte) 0);
		}

	}
}
