package com.backyardbrains.data.processing;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import com.backyardbrains.usb.AbstractUsbInputSource;
import com.backyardbrains.utils.AudioUtils;
import java.nio.ByteBuffer;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class ProcessingBuffer implements AbstractUsbInputSource.OnSpikerBoxEventMessageReceivedListener {

    private static final String TAG = makeLogTag(ProcessingBuffer.class);

    //
    private static final int DEFAULT_BUFFER_SIZE = AudioUtils.SAMPLE_RATE * 6; // 6 seconds

    private static ProcessingBuffer INSTANCE;

    private SampleBuffer sampleBuffer;
    private RingBuffer<String> eventBuffer;
    private int bufferSize = DEFAULT_BUFFER_SIZE;
    private long lastBytePosition;

    private final SparseArray<String> tmpEventMap;

    // Private constructor through which we create singleton instance
    private ProcessingBuffer() {
        sampleBuffer = new SampleBuffer(bufferSize);
        eventBuffer = new RingBuffer<>(String.class, bufferSize);
        tmpEventMap = new SparseArray<>();
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

        tmpEventMap.clear();

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
     * Adds specified {@code data} to ring buffer and saves position of the last added byte
     */
    public void addToBuffer(@Nullable ByteBuffer data, long lastBytePosition) {
        // just return if data is null
        if (data == null) return;

        // add data to ring buffer
        if (sampleBuffer != null) sampleBuffer.add(data);

        // last played byte position
        this.lastBytePosition = lastBytePosition;
    }

    /**
     * Adds specified {@code samples} to the ring buffer and returns all the events from this sample batch if any.
     */
    public SparseArray<String> addToBufferAndGetEvents(@Nullable short[] samples) {
        // just return if data is null
        if (samples == null) return new SparseArray<>();

        // add samples to ring buffer
        if (sampleBuffer != null) sampleBuffer.add(samples);
        // add event
        String[] events = new String[samples.length];
        int index;
        int len = tmpEventMap.size();
        for (int i = 0; i < len; i++) {
            LOGD(TAG, "ADDING NEW EVENT " + tmpEventMap.valueAt(i) + " TO BUFFER AT " + tmpEventMap.keyAt(i));
            index = tmpEventMap.keyAt(i) >= events.length ? events.length - 1 : tmpEventMap.keyAt(i);
            events[index] = tmpEventMap.valueAt(i);
        }
        // add events from this sample batch to event ring buffer
        if (eventBuffer != null) eventBuffer.add(events);
        // we need to return events for the current batch
        SparseArray<String> eventsFromSampleBatch = tmpEventMap.clone();
        // clear temporary event map to get it ready for next batch
        tmpEventMap.clear();

        return eventsFromSampleBatch;
    }

    /**
     * Clears the ring buffer and resets last read byte position
     */
    public void clearBuffer() {
        if (sampleBuffer != null) {
            sampleBuffer.clear();
            lastBytePosition = 0;
        }
        if (eventBuffer != null) eventBuffer.clear();
        tmpEventMap.clear();
    }

    /**
     * Returns last read byte position.
     */
    public long getLastBytePosition() {
        return lastBytePosition;
    }

    //======================================================================
    //  IMPLEMENTATION OF OnSpikerBoxEventMessageReceivedListener INTERFACE
    //======================================================================

    @Override public void onEventReceived(@NonNull String event, int sampleIndex) {
        tmpEventMap.put(sampleIndex, event);
    }
}
