package com.backyardbrains.data.processing;

import android.support.annotation.NonNull;
import com.backyardbrains.utils.BufferUtils;
import com.backyardbrains.utils.EventUtils;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class ProcessingBuffer {

    private static final String TAG = makeLogTag(ProcessingBuffer.class);

    private static final int MAX_BUFFER_SIZE = BufferUtils.MAX_SAMPLE_BUFFER_SIZE;

    private static final Object eventBufferLock = new Object();

    private static ProcessingBuffer INSTANCE;

    private CircularShortBuffer ringBuffer;
    private final int[] eventIndices;
    private final String[] eventNames;
    private int eventCount;
    private int bufferSize = MAX_BUFFER_SIZE;
    private long lastSampleIndex;

    // Private constructor through which we create singleton instance
    private ProcessingBuffer() {
        ringBuffer = new CircularShortBuffer(bufferSize);
        eventIndices = new int[EventUtils.MAX_EVENT_COUNT];
        eventNames = new String[EventUtils.MAX_EVENT_COUNT];
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
    public void setSize(int bufferSize) {
        LOGD(TAG, "setSize(" + bufferSize + ")");

        if (this.bufferSize == bufferSize) return;
        if (bufferSize <= 0) return;

        ringBuffer.clear();
        ringBuffer = new CircularShortBuffer(bufferSize);

        eventCount = 0;

        lastSampleIndex = 0;

        this.bufferSize = bufferSize;
    }

    /**
     * Returns buffer size.
     */
    public int getSize() {
        return bufferSize;
    }

    /**
     * Gets as many of the requested samples as available from this buffer.
     *
     * @return number of samples actually got from this buffer (0 if no samples are available)
     */
    public int get(@NonNull short[] data) {
        return ringBuffer.get(data);
    }

    /**
     * Copies collections of event indices and event names accompanying sample data currently in the buffer to
     * specified {@code indices} and {@code events} and returns number of copied events.
     */
    public int copyEvents(int[] indices, String[] events) {
        synchronized (eventBufferLock) {
            System.arraycopy(eventIndices, 0, indices, 0, eventCount);
            System.arraycopy(eventNames, 0, events, 0, eventCount);

            return eventCount;
        }
    }

    /**
     * Returns index of the last sample in the buffer. By default the value is {@code 0}, and is set only when
     * processing samples during playback.
     */
    public long getLastSampleIndex() {
        return lastSampleIndex;
    }

    /**
     * Adds specified {@code samplesWithEvents} to the sample ring buffer and events collections.
     */
    public void addToBuffer(@NonNull SamplesWithEvents samplesWithEvents) {
        // add samples to ring buffer
        if (ringBuffer != null) ringBuffer.put(samplesWithEvents.samples, 0, samplesWithEvents.sampleCount);

        // add new events, update indices of existing events and remove events that are no longer visible
        synchronized (eventBufferLock) {
            int removeIndices;
            for (removeIndices = 0; removeIndices < eventCount; removeIndices++) {
                if (eventIndices[removeIndices] - samplesWithEvents.sampleCount < 0) continue;

                break;
            }
            int eventCounter = 0;
            for (int i = removeIndices; i < eventCount; i++) {
                eventIndices[eventCounter] = eventIndices[i] - samplesWithEvents.sampleCount;
                eventNames[eventCounter++] = eventNames[i];
            }
            int baseIndex = bufferSize - samplesWithEvents.sampleCount;
            for (int i = 0; i < samplesWithEvents.eventCount; i++) {
                eventIndices[eventCounter] = baseIndex + samplesWithEvents.eventIndices[i];
                eventNames[eventCounter++] = samplesWithEvents.eventNames[i];
            }
            eventCount = eventCount - removeIndices + samplesWithEvents.eventCount;
        }

        // save last sample index (playhead)
        lastSampleIndex = samplesWithEvents.lastSampleIndex;
    }

    /**
     * Clears the sample data ring buffer, events collections and resets last read byte position
     */
    public void clearBuffer() {
        if (ringBuffer != null) ringBuffer.clear();
        eventCount = 0;
        lastSampleIndex = 0;
    }
}
