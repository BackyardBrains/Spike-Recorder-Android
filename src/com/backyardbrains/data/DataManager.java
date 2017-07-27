package com.backyardbrains.data;

import android.support.annotation.Nullable;
import com.backyardbrains.audio.RingBuffer;
import com.backyardbrains.utils.AudioUtils;
import java.nio.ByteBuffer;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class DataManager {

    public static final int DEFAULT_BUFFER_SIZE = AudioUtils.SAMPLE_RATE * 6; // 6 seconds

    private static RingBuffer dataBuffer;
    private long lastBytePosition;

    private static DataManager manager;

    // Private constructor through which we create singleton instance
    private DataManager() {
        dataBuffer = new RingBuffer(DEFAULT_BUFFER_SIZE);
    }

    /**
     * Returns singleton instance of {@link DataManager} with default configuration.
     */
    public static DataManager get() {
        if (manager == null) manager = new DataManager();

        return manager;
    }

    /**
     * Returns singleton instance of {@link DataManager} with the buffer set to {@code bufferSize} size.
     */
    public static DataManager get(int bufferSize) {
        if (manager == null) manager = new DataManager();

        dataBuffer.clear();
        dataBuffer = new RingBuffer(bufferSize);

        return manager;
    }

    //=================================================
    //  PUBLIC METHODS
    //=================================================

    /**
     * Returns an array of shorts that are representing the sample data.
     *
     * @return a ordinate-corrected version of the audio buffer
     */
    public short[] getData() {
        return dataBuffer != null ? dataBuffer.getArray() : new short[0];
    }

    /**
     * Adds specified {@code data} to ring buffer and saves position of the last added byte
     */
    public void addToBuffer(@Nullable ByteBuffer data, long lastBytePosition) {
        // add audio data to buffer
        addToBuffer(data);
        // last played byte position
        this.lastBytePosition = lastBytePosition;
    }

    /**
     * Adds specified {@code data} to ring buffer
     */
    public void addToBuffer(@Nullable ByteBuffer data) {
        // just return if data is null
        if (data == null) return;

        // add data to ring buffer
        if (dataBuffer != null) dataBuffer.add(data);
    }

    /**
     * Adds specified {@code data} to ring buffer
     */
    public void addToBuffer(@Nullable short[] data) {
        // just return if data is null
        if (data == null) return;

        // add data to ring buffer
        if (dataBuffer != null) dataBuffer.add(data);
    }

    /**
     * Clears the ring buffer and resets last read byte position
     */
    public void clearBuffer() {
        if (dataBuffer != null) {
            dataBuffer.clear();
            lastBytePosition = 0;
        }
    }

    /**
     * Returns last read byte position.
     */
    public long getLastBytePosition() {
        return lastBytePosition;
    }
}
