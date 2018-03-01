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

    private SampleBuffer dataBuffer;
    private RingBuffer<String> markerBuffer;
    private int bufferSize = DEFAULT_BUFFER_SIZE;
    private long lastBytePosition;

    private final SparseArray<String> markerMap;

    // Private constructor through which we create singleton instance
    private ProcessingBuffer() {
        dataBuffer = new SampleBuffer(bufferSize);
        markerBuffer = new RingBuffer<>(String.class, bufferSize);
        markerMap = new SparseArray<>();
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

        dataBuffer.clear();
        dataBuffer = new SampleBuffer(bufferSize);

        markerBuffer.clear();
        markerBuffer = new RingBuffer<>(String.class, bufferSize);

        markerMap.clear();

        this.bufferSize = bufferSize;
    }

    /**
     * Returns an array of shorts that are representing the sample data.
     *
     * @return a ordinate-corrected version of the audio buffer
     */
    public short[] getData() {
        return dataBuffer != null ? dataBuffer.getArray() : new short[0];
    }

    /**
     * Returns an array of shorts that are representing
     */
    public String[] getMarkers() {
        return markerBuffer != null ? markerBuffer.getArray() : new String[0];
    }

    /**
     * Adds specified {@code data} to ring buffer and saves position of the last added byte
     */
    public void addToBuffer(@Nullable ByteBuffer data, long lastBytePosition) {
        // just return if data is null
        if (data == null) return;

        // add data to ring buffer
        if (dataBuffer != null) dataBuffer.add(data);

        // last played byte position
        this.lastBytePosition = lastBytePosition;
    }

    /**
     * Adds specified {@code data} to ring buffer
     */
    public void addToBuffer(@Nullable short[] data) {
        // just return if data is null
        if (data == null) return;

        // add data to ring buffer
        if (dataBuffer != null) dataBuffer.add(data);
        // add markers
        String[] markers = new String[data.length];
        int index;
        for (int i = 0; i < markerMap.size(); i++) {
            LOGD(TAG, "ADDING NEW EVENT " + markerMap.valueAt(i) + " TO BUFFER AT " + markerMap.keyAt(i));
            index = markerMap.keyAt(i) >= markers.length ? markers.length - 1 : markerMap.keyAt(i);
            markers[index] = markerMap.valueAt(i);
        }
        if (markerBuffer != null) markerBuffer.add(markers);
        markerMap.clear();
    }

    /**
     * Clears the ring buffer and resets last read byte position
     */
    public void clearBuffer() {
        if (dataBuffer != null) {
            dataBuffer.clear();
            lastBytePosition = 0;
        }
        if (markerBuffer != null) markerBuffer.clear();
        markerMap.clear();
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
        markerMap.put(sampleIndex, event);
    }
}
