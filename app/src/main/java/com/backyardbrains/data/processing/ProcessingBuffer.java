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

    //
    public static final int MAX_BUFFER_SIZE = BufferUtils.MAX_SAMPLE_BUFFER_SIZE; // 6 seconds of audio data

    private static final Object eventBufferLock = new Object();

    private static ProcessingBuffer INSTANCE;

    private SampleBuffer sampleBuffer;
    private int[] eventIndices;
    private String[] eventNames;
    private int eventCount;
    private int bufferSize = MAX_BUFFER_SIZE;
    private long lastSampleIndex;

    // Private constructor through which we create singleton instance
    private ProcessingBuffer() {
        sampleBuffer = new SampleBuffer(bufferSize);
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
    public void setBufferSize(int bufferSize) {
        LOGD(TAG, "setBufferSize(" + bufferSize + ")");

        if (this.bufferSize == bufferSize) return;
        if (bufferSize <= 0) return;

        sampleBuffer.clear();
        sampleBuffer = new SampleBuffer(bufferSize);

        synchronized (eventBufferLock) {
            eventIndices = new int[EventUtils.MAX_EVENT_COUNT];
            eventNames = new String[EventUtils.MAX_EVENT_COUNT];
            eventCount = 0;
        }

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
        if (sampleBuffer != null) sampleBuffer.add(samplesWithEvents.samples, samplesWithEvents.sampleCount);

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

        // add samples to ring buffer
        //if (sampleBuffer != null) sampleBuffer.add(samplesWithEvents.samples);
        //
        //// add new events, update indices of existing events and remove events that are no longer visible
        //synchronized (eventBufferLock) {
        //    int removeIndices;
        //    for (removeIndices = 0; removeIndices < eventCount; removeIndices++) {
        //        if (eventIndices[removeIndices] - samplesWithEvents.samples.length < 0) continue;
        //
        //        break;
        //    }
        //    int eventCounter = 0;
        //    for (int i = removeIndices; i < eventCount; i++) {
        //        eventIndices[eventCounter] = eventIndices[i] - samplesWithEvents.samples.length;
        //        eventNames[eventCounter++] = eventNames[i];
        //    }
        //    int baseIndex = bufferSize - samplesWithEvents.samples.length;
        //    for (int i = 0; i < samplesWithEvents.eventIndices.length; i++) {
        //        eventIndices[eventCounter] = baseIndex + samplesWithEvents.eventIndices[i];
        //        eventNames[eventCounter++] = samplesWithEvents.eventLabels[i];
        //    }
        //    eventCount = eventCount - removeIndices + samplesWithEvents.eventIndices.length;
        //}
        //
        //// save last sample index (playhead)
        //lastSampleIndex = samplesWithEvents.lastSampleIndex;
    }

    /**
     * Clears the sample data ring buffer, events collections and resets last read byte position
     */
    public void clearBuffer() {
        if (sampleBuffer != null) sampleBuffer.clear();
        eventIndices = new int[EventUtils.MAX_EVENT_COUNT];
        eventNames = new String[EventUtils.MAX_EVENT_COUNT];
        eventCount = 0;
        lastSampleIndex = 0;
    }
}
