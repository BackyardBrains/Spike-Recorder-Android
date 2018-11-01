package com.backyardbrains.dsp;

import android.support.annotation.NonNull;
import com.backyardbrains.utils.CircularShortBuffer;
import com.backyardbrains.utils.EventUtils;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class ProcessingBuffer {

    private static final Object eventBufferLock = new Object();

    private static ProcessingBuffer INSTANCE;

    // Circular buffer that holds incoming samples
    private CircularShortBuffer sampleBuffer;
    // Circular buffer that holds averaged incoming samples
    private CircularShortBuffer averagedSamplesBuffer;

    private final int[] eventIndices;
    private final String[] eventNames;
    private int eventCount;
    private long lastSampleIndex;

    // Private constructor through which we create singleton instance
    private ProcessingBuffer() {
        sampleBuffer = new CircularShortBuffer();
        averagedSamplesBuffer = new CircularShortBuffer();
        eventIndices = new int[EventUtils.MAX_EVENT_COUNT];
        eventNames = new String[EventUtils.MAX_EVENT_COUNT];
        eventCount = 0;
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
    //  PUBLIC and PACKAGE-PRIVATE METHODS
    //======================================================================

    /**
     * Returns size of the sample buffer.
     */
    public int getSampleBufferSize() {
        return sampleBuffer.capacity();
    }

    /**
     * Sets size of the sample buffer.
     */
    void setSampleBufferSize(int bufferSize) {
        if (sampleBuffer != null) sampleBuffer.clear();
        sampleBuffer = new CircularShortBuffer(bufferSize);
        eventCount = 0;
        lastSampleIndex = 0;
    }

    /**
     * Returns size of the averaged samples buffer.
     */
    public int getAveragedSamplesBufferSize() {
        return averagedSamplesBuffer.capacity();
    }

    /**
     * Sets size of the averages samples buffer.
     */
    void setAveragedSamplesBufferSize(int bufferSize) {
        if (averagedSamplesBuffer != null) averagedSamplesBuffer.clear();
        averagedSamplesBuffer = new CircularShortBuffer(bufferSize);
        eventCount = 0;
        lastSampleIndex = 0;
    }

    /**
     * Gets as many of the requested samples as available from the buffer.
     *
     * @return number of samples actually got from this buffer (0 if no samples are available)
     */
    public int get(@NonNull short[] data) {
        return sampleBuffer.get(data);
    }

    /**
     * Gets as many of the requested averaged samples as available from the buffer.
     *
     * @return number of averaged samples actually got from this buffer (0 if no samples are available)
     */
    public int getAveraged(@NonNull short[] data) {
        return averagedSamplesBuffer.get(data);
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
    void addToSampleBuffer(@NonNull SamplesWithEvents samplesWithEvents) {
        // add samples to signal ring buffer
        if (sampleBuffer != null) sampleBuffer.put(samplesWithEvents.samples, 0, samplesWithEvents.sampleCount);

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
            int baseIndex = sampleBuffer.capacity() - samplesWithEvents.sampleCount;
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
     * Adds specified {@code samplesWithEvents} to the averaged samples ring buffer and events collections.
     */
    void addToAveragedSamplesBuffer(@NonNull SamplesWithEvents samplesWithEvents) {
        // add samples to averaged signal ring buffer
        averagedSamplesBuffer.put(samplesWithEvents.samples, 0, samplesWithEvents.sampleCount);
    }

    /**
     * Clears the sample data ring buffer, events collections and resets last read byte position
     */
    @SuppressWarnings("unused") public void clearBuffer() {
        if (sampleBuffer != null) sampleBuffer.clear();
        if (averagedSamplesBuffer != null) averagedSamplesBuffer.clear();
        eventCount = 0;
        lastSampleIndex = 0;
    }
}
