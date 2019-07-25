package com.backyardbrains.dsp.usb;

import java.util.Arrays;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

class HIDBuffer {

    private static final String TAG = makeLogTag(HIDBuffer.class);

    // TI Vendor ID
    static final byte TEXAS_INSTRUMENTS_VENDOR_ID = 63;
    // Valid number of bytes to copy in each packet
    static final byte VALID_DATA_PACKET_SIZE = 62;

    private static final int PACKET_SIZE = 64;
    private static final int BUFFER_MULTIPLIER = 8;
    static final int DEFAULT_READ_BUFFER_SIZE = PACKET_SIZE * BUFFER_MULTIPLIER;
    static final int DEFAULT_READ_PAYLOAD_BUFFER_SIZE = DEFAULT_READ_BUFFER_SIZE - BUFFER_MULTIPLIER * 2;
    private static final int DEFAULT_WRITE_BUFFER_SIZE = 255 * 2;

    private SynchronizedBuffer writeBuffer;
    private byte[] readBuffer_compatible;
    //private byte[] tmp;
    //private byte[] tmp2;
    //private byte[] tmp3;
    //boolean printNext;

    HIDBuffer() {
        this(DEFAULT_READ_BUFFER_SIZE, DEFAULT_WRITE_BUFFER_SIZE);
    }

    private HIDBuffer(int readBufferSize, int writeBufferSize) {
        writeBuffer = new SynchronizedBuffer(writeBufferSize);
        readBuffer_compatible = new byte[readBufferSize];
        tmp = new byte[readBufferSize];
        tmp2 = new byte[readBufferSize];
        tmp3 = new byte[readBufferSize];
    }

    byte[] getBufferCompatible() {
        return readBuffer_compatible;
    }

    int getDataReceivedCompatible(byte[] buffer, int length) {
        int counter = 0;
        int copy, vendorId;
        //if (printNext) {
        //    LOGD(TAG, Arrays.toString(tmp3));
        //    LOGD(TAG, Arrays.toString(tmp2));
        //    LOGD(TAG, Arrays.toString(tmp));
        //    LOGD(TAG, Arrays.toString(readBuffer_compatible));
        //    LOGD(TAG, "===================");
        //    printNext = false;
        //}
        for (int i = 0; i < length; i += PACKET_SIZE) {
            vendorId = readBuffer_compatible[i];
            copy = readBuffer_compatible[i + 1];
            if (vendorId != TEXAS_INSTRUMENTS_VENDOR_ID || copy != VALID_DATA_PACKET_SIZE) {
                //LOGD(TAG, "(" + length + ") -> " + copy);
                //printNext = true;
                break;
            }
            System.arraycopy(readBuffer_compatible, i + 2, buffer, counter, copy);
            counter += copy;
        }
        if (length < DEFAULT_READ_BUFFER_SIZE) {
            LOGD(TAG, "(" + length + ") -> " + counter);
        }
        //System.arraycopy(tmp2, 0, tmp3, 0, DEFAULT_READ_BUFFER_SIZE);
        //System.arraycopy(tmp, 0, tmp2, 0, DEFAULT_READ_BUFFER_SIZE);
        //System.arraycopy(readBuffer_compatible, 0, tmp, 0, DEFAULT_READ_BUFFER_SIZE);

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
