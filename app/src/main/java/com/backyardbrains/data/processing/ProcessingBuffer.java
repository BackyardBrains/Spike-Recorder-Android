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
    private int[] eventIndices;
    private String[] eventNames;
    private int bufferSize = MAX_BUFFER_SIZE;
    private long lastSampleIndex;

    // Private constructor through which we create singleton instance
    private ProcessingBuffer() {
        sampleBuffer = new SampleBuffer(bufferSize);
        eventIndices = new int[0];
        eventNames = new String[0];
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

        eventIndices = new int[0];
        eventNames = new String[0];

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
     * Returns an array of indices of events accompanying sample data currently in the buffer.
     */
    public int[] getEventIndices() {
        return eventIndices;
    }

    /**
     * Returns an array of event names accompanying sample data currently in the buffer.
     */
    @NonNull public String[] getEventNames() {
        return eventNames;
        //return eventBuffer != null ? eventBuffer.getArray() : new String[0];
    }

    /**
     * Returns index of the last sample in the buffer. By default the value is {@code 0}, and is set only when
     * processing samples during playback.
     */
    public long getLastSampleIndex() {
        return lastSampleIndex;
    }

    /**
     * Adds specified {@code samplesWithMarkers} to the sample ring buffer and events collections.
     */
    public void addToBuffer(@NonNull DataProcessor.SamplesWithMarkers samplesWithMarkers) {
        // add samples to ring buffer
        if (sampleBuffer != null) sampleBuffer.add(samplesWithMarkers.samples);

        // add new events, update indices of existing events and remove events that are no longer visible
        int removeIndices;
        for (removeIndices = 0; removeIndices < eventIndices.length; removeIndices++) {
            if (eventIndices[removeIndices] - samplesWithMarkers.samples.length < 0) continue;

            break;
        }
        int newLen = eventIndices.length - removeIndices + samplesWithMarkers.eventIndices.length;
        int[] newEventIndices = new int[newLen];
        String[] newEventLabels = new String[newLen];
        int eventCounter = 0;
        for (int i = removeIndices; i < eventIndices.length; i++) {
            newEventIndices[eventCounter] = eventIndices[i] - samplesWithMarkers.samples.length;
            newEventLabels[eventCounter++] = eventNames[i];
        }
        int baseIndex = bufferSize - samplesWithMarkers.samples.length;
        for (int i = 0; i < samplesWithMarkers.eventIndices.length; i++) {
            newEventIndices[eventCounter] = baseIndex + samplesWithMarkers.eventIndices[i];
            newEventLabels[eventCounter++] = samplesWithMarkers.eventLabels[i];
        }
        eventIndices = newEventIndices;
        eventNames = newEventLabels;

        // save last sample index (playhead)
        lastSampleIndex = samplesWithMarkers.lastSampleIndex;
    }

    /**
     * Clears the sample data ring buffer, events collections and resets last read byte position
     */
    public void clearBuffer() {
        if (sampleBuffer != null) sampleBuffer.clear();
        eventIndices = new int[0];
        eventNames = new String[0];
        this.lastSampleIndex = 0;
    }
}
