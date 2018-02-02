package com.backyardbrains.usb;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class HIDBuffer {

    public static final int DEFAULT_READ_BUFFER_SIZE = 64;
    public static final int DEFAULT_WRITE_BUFFER_SIZE = 255 * 2;

    private ByteBuffer readBuffer;
    private SynchronizedBuffer writeBuffer;

    public HIDBuffer() {
        writeBuffer = new SynchronizedBuffer();
        readBuffer = ByteBuffer.allocate(DEFAULT_READ_BUFFER_SIZE);
    }

    public void putReadBuffer(ByteBuffer data) {
        synchronized (this) {
            try {
                readBuffer.put(data);
            } catch (BufferOverflowException e) {
                // TO-DO
            }
        }
    }

    public ByteBuffer getReadBuffer() {
        synchronized (this) {
            return readBuffer;
        }
    }

    public byte[] getDataReceived() {
        synchronized (this) {
            byte[] dst = new byte[readBuffer.position()];
            readBuffer.position(0);
            readBuffer.get(dst, 0, dst.length);
            return dst;
        }
    }

    public void clearReadBuffer() {
        synchronized (this) {
            readBuffer.clear();
        }
    }

    public byte[] getWriteBuffer() {
        return writeBuffer.get();
    }

    public void putWriteBuffer(byte[] data) {
        writeBuffer.put(data);
    }

    public void resetWriteBuffer() {
        writeBuffer.reset();
    }

    private class SynchronizedBuffer {
        private byte[] buffer;
        private int position;

        SynchronizedBuffer() {
            this.buffer = new byte[DEFAULT_WRITE_BUFFER_SIZE];
            position = -1;
        }

        synchronized void put(byte[] src) {
            if (src == null || src.length == 0) return;
            if (position == -1) position = 0;

            //Checking bounds. Source data does not fit in buffer
            if (position + src.length > DEFAULT_WRITE_BUFFER_SIZE - 1) {
                if (position < DEFAULT_WRITE_BUFFER_SIZE) {
                    System.arraycopy(src, 0, buffer, position, DEFAULT_WRITE_BUFFER_SIZE - position);
                }
                position = DEFAULT_WRITE_BUFFER_SIZE;
                notify();
            } else // Source data fits in buffer
            {
                System.arraycopy(src, 0, buffer, position, src.length);
                position += src.length;
                notify();
            }
        }

        public synchronized byte[] get() {
            if (position == -1) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (position <= -1) return new byte[0];
            byte[] dst = Arrays.copyOfRange(buffer, 0, position);
            position = -1;
            return dst;
        }

        public synchronized void reset() {
            position = -1;
        }
    }
}
