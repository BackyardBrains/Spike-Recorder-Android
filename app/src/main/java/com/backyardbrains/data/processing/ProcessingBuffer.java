package com.backyardbrains.data.processing;

import android.support.annotation.NonNull;
import com.backyardbrains.utils.AudioUtils;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class ProcessingBuffer {

    private static final String TAG = makeLogTag(ProcessingBuffer.class);

    //
    public static final int MAX_BUFFER_SIZE = AudioUtils.SAMPLE_RATE * 6; // 6 seconds

    private static ProcessingBuffer INSTANCE;

    private SampleBuffer sampleBuffer;
    private RingBuffer<String> eventBuffer;
    private int bufferSize = MAX_BUFFER_SIZE;
    private long lastSampleIndex;

    // Private constructor through which we create singleton instance
    private ProcessingBuffer() {
        sampleBuffer = new SampleBuffer(bufferSize);
        eventBuffer = new RingBuffer<>(String.class, bufferSize);
        lastSampleIndex = 0;
    }

    /**
     * Returns singleton instance of {@link ProcessingBuffer} with default configuration.
     */
    public static ProcessingBuffer get() {
        if (INSTANCE == null) {
            synchronized (ProcessingBuffer.class) {
                if (INSTANCE == null) INSTANCE = new ProcessingBuffer();
            }
        }
        return INSTANCE;
    }

    //======================================================================
    //  PUBLIC METHODS
    //======================================================================

    /**
     * Sets buffer size of the {@link SampleBuffer}.
     */
    public void setBufferSize(int bufferSize) {
        LOGD(TAG, "setBufferSize(" + bufferSize + ")");

        if (this.bufferSize == bufferSize) return;
        if (bufferSize <= 0) return;

        sampleBuffer.clear();
        sampleBuffer = new SampleBuffer(bufferSize);

        eventBuffer.clear();
        eventBuffer = new RingBuffer<>(String.class, bufferSize);

        lastSampleIndex = 0;

        this.bufferSize = bufferSize;
    }

    /**
     * Returns buffer size.
     */
    public int getBufferSize() {
        return bufferSize;
    }

    /**
     * Returns an array of shorts that are representing the sample data.
     *
     * @return a ordinate-corrected version of the audio buffer
     */
    @NonNull public short[] getData() {
        return sampleBuffer != null ? sampleBuffer.getArray() : new short[0];
    }

    /**
     * Returns an array of Strings that are representing all the events accompanying sames data.
     */
    @NonNull public String[] getEvents() {
        return eventBuffer != null ? eventBuffer.getArray() : new String[0];
    }

    /**
     * Returns index of the last sample in the buffer. By default the value is {@code 0}, and is set only when
     * processing samples during playback.
     */
    public long getLastSampleIndex() {
        return lastSampleIndex;
    }

    /**
     * Adds specified {@code samples} to the ring buffer and returns all the events from this sample batch if any.
     */
    public void addToBuffer(@NonNull DataProcessor.SamplesWithMarkers samplesWithMarkers) {
        // add samples to ring buffer
        if (sampleBuffer != null) sampleBuffer.add(samplesWithMarkers.samples);
        // add events to ring buffer
        if (eventBuffer != null) eventBuffer.add(samplesWithMarkers.events);
        // save last sample index (playhead)
        this.lastSampleIndex = samplesWithMarkers.lastSampleIndex;
    }

    /**
     * Clears the ring buffer and resets last read byte position
     */
    public void clearBuffer() {
        if (sampleBuffer != null) sampleBuffer.clear();
        if (eventBuffer != null) eventBuffer.clear();
        this.lastSampleIndex = 0;
    }
}
