package com.backyardbrains.data.processing;

import android.support.annotation.Nullable;
import android.util.SparseArray;
import com.backyardbrains.utils.AudioUtils;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class ProcessingBuffer {

    private static final String TAG = makeLogTag(ProcessingBuffer.class);

    //
    private static final int DEFAULT_BUFFER_SIZE = AudioUtils.SAMPLE_RATE * 6; // 6 seconds

    private static ProcessingBuffer INSTANCE;

    private SampleBuffer sampleBuffer;
    private RingBuffer<String> eventBuffer;
    private int bufferSize = DEFAULT_BUFFER_SIZE;

    // Private constructor through which we create singleton instance
    private ProcessingBuffer() {
        sampleBuffer = new SampleBuffer(bufferSize);
        eventBuffer = new RingBuffer<>(String.class, bufferSize);
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

        this.bufferSize = bufferSize;
    }

    /**
     * Returns an array of shorts that are representing the sample data.
     *
     * @return a ordinate-corrected version of the audio buffer
     */
    public short[] getData() {
        return sampleBuffer != null ? sampleBuffer.getArray() : new short[0];
    }

    /**
     * Returns an array of Strings that are representing all the events accompanying sames data.
     */
    public String[] getEvents() {
        return eventBuffer != null ? eventBuffer.getArray() : new String[0];
    }

    /**
     * Adds specified {@code samples} to the ring buffer and returns all the events from this sample batch if any.
     */
    public void addToBuffer(@Nullable short[] samples, SparseArray<String> events) {
        // just return if data is null
        if (samples == null) return;

        // add samples to ring buffer
        if (sampleBuffer != null) sampleBuffer.add(samples);
        // add event
        String[] e = new String[samples.length];
        int index;
        int len = events.size();
        for (int i = 0; i < len; i++) {
            //LOGD(TAG, "ADDING NEW EVENT " + tmpEventMap.valueAt(i) + " TO BUFFER AT " + tmpEventMap.keyAt(i));
            index = events.keyAt(i) >= e.length ? e.length - 1 : events.keyAt(i);
            e[index] = events.valueAt(i);
        }
        // add events from this sample batch to event ring buffer
        if (eventBuffer != null) eventBuffer.add(e);
    }

    /**
     * Clears the ring buffer and resets last read byte position
     */
    public void clearBuffer() {
        if (sampleBuffer != null) {
            sampleBuffer.clear();
        }
        if (eventBuffer != null) eventBuffer.clear();
    }
}
