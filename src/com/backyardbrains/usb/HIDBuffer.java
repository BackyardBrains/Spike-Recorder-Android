package com.backyardbrains.usb;

import java.util.Arrays;

class HIDBuffer {

    static final int DEFAULT_READ_BUFFER_SIZE = 64;
    private static final int DEFAULT_WRITE_BUFFER_SIZE = 255 * 2;

    private SynchronizedBuffer writeBuffer;
    private byte[] readBuffer_compatible; // Read buffer for android < 4.2

    HIDBuffer() {
        writeBuffer = new SynchronizedBuffer();
        readBuffer_compatible = new byte[DEFAULT_READ_BUFFER_SIZE];
    }

    byte[] getBufferCompatible() {
        return readBuffer_compatible;
    }

    byte[] getDataReceivedCompatible(int numberBytes) {
        return Arrays.copyOfRange(readBuffer_compatible, 0, numberBytes);
    }

    byte[] getWriteBuffer() {
        return writeBuffer.get();
    }

    void putWriteBuffer(byte[] data) {
        writeBuffer.put(data);
    }

    void resetWriteBuffer() {
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
