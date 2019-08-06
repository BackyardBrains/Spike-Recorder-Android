package com.backyardbrains.dsp.usb;

import java.util.Arrays;

class HIDBuffer {

    private static final int PACKET_SIZE = 64;
    private static final int BUFFER_MULTIPLIER = 8;
    static final int DEFAULT_READ_BUFFER_SIZE = PACKET_SIZE * BUFFER_MULTIPLIER;
    static final int DEFAULT_READ_PAYLOAD_BUFFER_SIZE = DEFAULT_READ_BUFFER_SIZE - BUFFER_MULTIPLIER * 2;
    private static final int DEFAULT_WRITE_BUFFER_SIZE = 255 * 2;

    private SynchronizedBuffer writeBuffer;
    private byte[] readBuffer;

    HIDBuffer() {
        this(DEFAULT_READ_BUFFER_SIZE, DEFAULT_WRITE_BUFFER_SIZE);
    }

    private HIDBuffer(int readBufferSize, int writeBufferSize) {
        writeBuffer = new SynchronizedBuffer(writeBufferSize);
        readBuffer = new byte[readBufferSize];
    }

    byte[] getReadBuffer() {
        return readBuffer;
    }

    int getDataReceived(byte[] buffer, int length) {
        int counter = 0;
        int copy;
        for (int i = 0; i < length; i += PACKET_SIZE) {
            copy = readBuffer[i + 1];
            // first two bytes are reserved for HID Report ID(vendor specific), and number of transferred bytes
            System.arraycopy(readBuffer, i + 2, buffer, counter, copy);
            counter += copy;
        }

        return counter;
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
        private int bufferSize;

        SynchronizedBuffer(int bufferSize) {
            this.bufferSize = bufferSize;
            this.buffer = new byte[bufferSize];
            position = -1;
        }

        synchronized void put(byte[] src) {
            if (src == null || src.length == 0) return;
            if (position == -1) position = 0;

            //Checking bounds. Source data does not fit in buffer
            if (position + src.length > bufferSize - 1) {
                if (position < bufferSize) System.arraycopy(src, 0, buffer, position, bufferSize - position);
                position = bufferSize;
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
