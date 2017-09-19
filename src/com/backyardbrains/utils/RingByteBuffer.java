package com.backyardbrains.utils;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class RingByteBuffer {

    private byte[] buffer;      // queue elements
    private int count = 0;      // number of elements on queue
    private int indexOut = 0;   // index of first element of queue
    private int indexIn = 0;    // index of next available slot

    // cast needed since no generic array creation in Java
    public RingByteBuffer(int capacity) {
        buffer = new byte[capacity];
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public int size() {
        return count;
    }

    public void push(byte item) {
        if (count == buffer.length) {
            throw new RuntimeException("Ring buffer overflow");
        }
        buffer[indexIn] = item;
        indexIn = (indexIn + 1) % buffer.length;     // wrap-around
        count++;
    }

    public byte pop() {
        if (isEmpty()) {
            throw new RuntimeException("Ring buffer underflow");
        }
        byte item = buffer[indexOut];
        buffer[indexOut] = 0;
        count--;
        indexOut = (indexOut + 1) % buffer.length; // wrap-around
        return item;
    }
}
