package com.backyardbrains.dsp;

import android.support.annotation.NonNull;
import com.backyardbrains.drawing.FftDrawBuffer;
import com.backyardbrains.drawing.MultichannelSignalDrawBuffer;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.CircularFloatArrayBuffer;
import com.backyardbrains.utils.CircularShortBuffer;
import com.backyardbrains.utils.EventUtils;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class ProcessingBuffer {

    // Lock used when reading/writing samples and events
    private static final Object lock = new Object();

    private static ProcessingBuffer INSTANCE;

    // Circular buffer array that holds incoming samples by channel
    private CircularShortBuffer[] sampleBuffers;
    // Circular buffers that holds averaged incoming samples by channel
    private CircularShortBuffer[] averagedSamplesBuffers;
    // Size of the sample buffer
    private int sampleBufferSize = SignalProcessor.DEFAULT_PROCESSED_SAMPLES_PER_CHANNEL_COUNT;
    // Temp buffer used to copy buffered samples to draw buffer
    private short[] samples = new short[SignalProcessor.MAX_PROCESSED_SAMPLES_COUNT];
    // Array of processed event indices
    private final int[] eventIndices;
    // Array of processed event names
    private final String[] eventNames;
    // Number of processed events
    private int eventCount;
    // Index of the sample that was processed last (used only during playback)
    private long lastSampleIndex;

    // Buffer for the FFT data
    private CircularFloatArrayBuffer fftBuffer = new CircularFloatArrayBuffer(500, 500);
    // Temp buffer used to copy buffered fft data to draw buffer (500x500 is enough for any sample rate we use)
    private float[][] fft = new float[500][500];

    // Private constructor through which we create singleton instance
    private ProcessingBuffer() {
        createSampleBuffers(AudioUtils.DEFAULT_CHANNEL_COUNT);
        createAveragedSamplesBuffer(AudioUtils.DEFAULT_CHANNEL_COUNT);
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
     * Copies as many samples, averaged samples, event indices, event names and FFT data accompanying the sample data
     * currently in the buffer as available to the specified {@code signalDrawBuffer}, {@code averagedSignalDrawBuffer},
     * {@code eventIndices}, {@code eventNames} and @{code fftDrawBuffer}.
     *
     * @return Number of copied events.
     */
    public int copy(@NonNull MultichannelSignalDrawBuffer signalDrawBuffer,
        @NonNull MultichannelSignalDrawBuffer averagedSignalDrawBuffer, @NonNull int[] eventIndices,
        @NonNull String[] eventNames, @NonNull FftDrawBuffer fftDrawBuffer) {
        synchronized (lock) {
            // copy samples
            if (sampleBuffers != null) {
                for (int i = 0; i < sampleBuffers.length; i++) {
                    if (sampleBuffers[i] != null) {
                        int count = sampleBuffers[i].get(samples);
                        if (count > 0) signalDrawBuffer.add(i, samples, count);
                    }
                }
            }
            // copy averaged samples
            if (averagedSamplesBuffers != null) {
                for (int i = 0; i < averagedSamplesBuffers.length; i++) {
                    if (averagedSamplesBuffers[i] != null) {
                        int count = averagedSamplesBuffers[i].get(samples);
                        if (count > 0) averagedSignalDrawBuffer.add(i, samples, count);
                    }
                }
            }
            // copy events
            System.arraycopy(this.eventIndices, 0, eventIndices, 0, eventCount);
            System.arraycopy(this.eventNames, 0, eventNames, 0, eventCount);
            // copy fft data
            if (fftBuffer != null) {
                int count = fftBuffer.get(fft);
                if (count > 0) fftDrawBuffer.add(fft, count);
            }

            return eventCount;
        }
    }

    /**
     * Adds specified {@code signalData}, {@code averagedSamples} and {@code fftData} to the buffer.
     */
    void add(@NonNull SignalData signalData, @NonNull SignalData averagedSamples, @NonNull FftData fftData) {
        synchronized (lock) {
            // add samples to signal ring buffer
            if (sampleBuffers != null && sampleBuffers.length == signalData.samples.length) {
                for (int i = 0; i < signalData.samples.length; i++) {
                    if (sampleBuffers[i] != null) {
                        sampleBuffers[i].put(signalData.samples[i], 0, signalData.sampleCounts[i]);
                    }
                }
            }
            // add samples to averaged signal ring buffer
            if (averagedSamplesBuffers != null && averagedSamplesBuffers.length == averagedSamples.samples.length) {
                for (int i = 0; i < averagedSamples.samples.length; i++) {
                    if (averagedSamplesBuffers[i] != null) {
                        averagedSamplesBuffers[i].put(averagedSamples.samples[i], 0, averagedSamples.sampleCounts[i]);
                    }
                }
            }

            // skip events that are no longer visible (they will be overwritten when adding new ones)
            int removeIndices;
            for (removeIndices = 0; removeIndices < eventCount; removeIndices++) {
                if (eventIndices[removeIndices] - signalData.sampleCounts[0] < 0) continue;

                break;
            }
            // update indices of existing events
            int eventCounter = 0;
            for (int i = removeIndices; i < eventCount; i++) {
                eventIndices[eventCounter] = eventIndices[i] - signalData.sampleCounts[0];
                eventNames[eventCounter++] = eventNames[i];
            }
            // add new events
            int baseIndex = Math.max(0, sampleBufferSize - signalData.sampleCounts[0]);
            for (int i = 0; i < signalData.eventCount; i++) {
                eventIndices[eventCounter] = baseIndex + signalData.eventIndices[i];
                eventNames[eventCounter++] = signalData.eventNames[i];
            }
            eventCount = eventCount - removeIndices + signalData.eventCount;

            // save last sample index (playhead)
            lastSampleIndex = signalData.lastSampleIndex;

            // add fft data to buffer
            if (fftBuffer != null) fftBuffer.put(fftData.fft, 0, fftData.windowCount);
        }
    }

    /**
     * Resets sample buffers.
     */
    void resetAllSampleBuffers(int channelCount, int visibleChannelCount) {
        // reset sample buffer
        clearBuffers();
        createSampleBuffers(channelCount);
        // reset averaged samples buffer
        clearAveragedSamplesBuffer();
        createAveragedSamplesBuffer(visibleChannelCount);
    }

    /**
     * Resets averaged samples buffer.
     */
    void resetAveragedSamplesBuffer(int visibleChannelCount) {
        clearAveragedSamplesBuffer();
        createAveragedSamplesBuffer(visibleChannelCount);
    }

    /**
     * Clears sample buffer, averaged samples buffers, events collections and resets last read byte position.
     */
    void clearAllBuffers() {
        clearBuffers();
        clearAveragedSamplesBuffer();
    }

    /**
     * Returns index of the last sample in the buffer. By default the value is {@code 0}, and is set only when
     * processing samples during playback.
     */
    public long getLastSampleIndex() {
        return lastSampleIndex;
    }

    // Creates sample data buffers
    private void createSampleBuffers(int channelCount) {
        synchronized (lock) {
            sampleBuffers = new CircularShortBuffer[channelCount];
            sampleBufferSize = SignalProcessor.getProcessedSamplesPerChannelCount();
            for (int i = 0; i < channelCount; i++) {
                sampleBuffers[i] = new CircularShortBuffer(sampleBufferSize);
            }
            //fftBuffer = new CircularFloatArrayBuffer(SignalProcessor.getProcessedFftWindowCount(),
            //    SignalProcessor.getProcessedFftWindowSize());
        }
    }

    // Clears the sample data ring buffer, events collections and resets last read byte position.
    private void clearBuffers() {
        synchronized (lock) {
            if (sampleBuffers != null) {
                for (CircularShortBuffer sb : sampleBuffers) {
                    if (sb != null) sb.clear();
                }
            }
            eventCount = 0;
            lastSampleIndex = 0;
            fftBuffer.clear();
        }
    }

    // Creates averaged samples data buffers
    private void createAveragedSamplesBuffer(int channelCount) {
        synchronized (lock) {
            averagedSamplesBuffers = new CircularShortBuffer[channelCount];
            for (int i = 0; i < channelCount; i++) {
                averagedSamplesBuffers[i] = new CircularShortBuffer(SignalProcessor.getProcessedAveragedSamplesPerChannelCount());
            }
        }
    }

    // Clears the averaged samples data ring buffers.
    private void clearAveragedSamplesBuffer() {
        synchronized (lock) {
            if (averagedSamplesBuffers != null) {
                for (CircularShortBuffer sb : averagedSamplesBuffers) {
                    if (sb != null) sb.clear();
                }
            }
        }
    }
}
