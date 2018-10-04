package com.backyardbrains.data.processing;

import android.support.annotation.NonNull;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.Benchmark;
import com.backyardbrains.utils.EventUtils;
import com.backyardbrains.utils.JniUtils;
import com.backyardbrains.utils.SampleStreamUtils;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class ProcessingBuffer {

    private static final String TAG = makeLogTag(ProcessingBuffer.class);

    // Maximum time that should be processed when processing audio signal
    private static final double MAX_AUDIO_PROCESSING_TIME = 6; // 6 seconds
    // Maximum time that should be processed when processing sample stream
    private static final double MAX_SAMPLE_STREAM_PROCESSING_TIME = 12; // 12 seconds
    // Maximum time that should be processed when averaging signal
    private static final double MAX_THRESHOLD_PROCESSING_TIME = 2.4; // 2.4 seconds

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
    private int sampleRate = AudioUtils.SAMPLE_RATE;
    private boolean averagingSignal;
    private boolean playback;

    // Private constructor through which we create singleton instance
    private ProcessingBuffer() {
        sampleBuffer = new CircularShortBuffer((int) (MAX_AUDIO_PROCESSING_TIME * sampleRate));
        averagedSamplesBuffer = new CircularShortBuffer((int) (MAX_THRESHOLD_PROCESSING_TIME * sampleRate));
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
    //  PUBLIC METHODS
    //======================================================================

    /**
     * Sets sample rate of the incoming signal.
     */
    public void setSampleRate(int sampleRate) {
        LOGD(TAG, "setSampleRate(" + sampleRate + ")");

        if (this.sampleRate == sampleRate) return;
        if (sampleRate <= 0) return;

        final int bufferSize;
        if (this.sampleRate == SampleStreamUtils.SAMPLE_RATE && !playback) {
            bufferSize = (int) (MAX_SAMPLE_STREAM_PROCESSING_TIME * sampleRate);
        } else {
            bufferSize = (int) (MAX_AUDIO_PROCESSING_TIME * sampleRate);
        }
        sampleBuffer.clear();
        sampleBuffer = new CircularShortBuffer(bufferSize);

        averagedSamplesBuffer.clear();
        averagedSamplesBuffer = new CircularShortBuffer((int) (MAX_THRESHOLD_PROCESSING_TIME * sampleRate));

        eventCount = 0;

        lastSampleIndex = 0;

        this.sampleRate = sampleRate;
    }

    /**
     * Sets whether incoming signal is being averaged
     */
    public void setAveragingSignal(boolean averagingSignal) {
        if (this.averagingSignal == averagingSignal) return;

        this.averagingSignal = averagingSignal;
    }

    /**
     * Sets whether incoming signal source is a file.
     */
    public void setPlayback(boolean playback) {
        if (this.playback == playback) return;

        this.playback = playback;
    }

    /**
     * Returns size of the currently used sample buffer depending on whether averaging is on of off.
     */
    public int getSize() {
        return averagingSignal ? averagedSamplesBuffer.capacity() : sampleBuffer.capacity();
    }

    /**
     * Returns size of the sample buffer.
     */
    public int getBufferSize() {
        return sampleBuffer.capacity();
    }

    /**
     * Returns size of the averaged samples buffer.
     */
    public int getThresholdBufferSize() {
        return averagedSamplesBuffer.capacity();
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

    private final Benchmark benchmark = new Benchmark("THRESHOLD").warmUp(50)
        .sessions(10)
        .measuresPerSession(50)
        .logBySession(false)
        .listener(new Benchmark.OnBenchmarkListener() {
            @Override public void onEnd() {
                //EventBus.getDefault().post(new ShowToastEvent("PRESS BACK BUTTON!!!!"));
            }
        });

    /**
     * Adds specified {@code samplesWithEvents} to the sample ring buffer and events collections.
     */
    public void addToBuffer(@NonNull SamplesWithEvents samplesWithEvents) {
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

        // average signal if necessary
        if (averagingSignal) {
            //benchmark.start();
            JniUtils.processThreshold(samplesWithEvents);
            //benchmark.end();

            // add samples to threshold ring buffer
            averagedSamplesBuffer.put(samplesWithEvents.samples, 0, samplesWithEvents.sampleCount);
        }

        // save last sample index (playhead)
        lastSampleIndex = samplesWithEvents.lastSampleIndex;
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
