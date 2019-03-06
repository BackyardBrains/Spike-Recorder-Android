package com.backyardbrains.dsp;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.drawing.FftDrawBuffer;
import com.backyardbrains.drawing.MultichannelSignalDrawBuffer;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.CircularFloatArrayBuffer;
import com.backyardbrains.utils.CircularShortBuffer;
import com.backyardbrains.utils.EventUtils;

import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class ProcessingBuffer {

    private static final String TAG = makeLogTag(ProcessingBuffer.class);

    // Lock used when reading/writing samples and events
    private static final Object lock = new Object();

    private static ProcessingBuffer INSTANCE;

    // Signal sample rate
    private int sampleRate = AudioUtils.DEFAULT_SAMPLE_RATE;
    // Number of processed channels
    private int channelCount = SignalProcessor.DEFAULT_CHANNEL_COUNT;

    // Circular buffer array that holds incoming samples by channel
    private CircularShortBuffer[] sampleBuffers;
    // Circular buffers that holds averaged incoming samples by channel
    private CircularShortBuffer[] averagedSamplesBuffers;
    // Size of the sample buffer
    private int sampleBufferSize = SignalProcessor.DEFAULT_SAMPLE_BUFFER_SIZE;
    // Temp buffer used to copy buffered samples to draw buffer
    private short[] samples = new short[SignalProcessor.DEFAULT_SAMPLE_BUFFER_SIZE];
    // Array of processed event indices
    private final int[] eventIndices;
    // Array of processed event names
    private final String[] eventNames;
    // Number of processed events
    private int eventCount;
    // Index of the sample that was processed last (used only during playback)
    private long lastSampleIndex;
    // Buffer for the FFT data
    private CircularFloatArrayBuffer fftBuffer = new CircularFloatArrayBuffer(SignalProcessor.DEFAULT_FFT_WINDOW_COUNT,
        SignalProcessor.DEFAULT_FFT_30HZ_WINDOW_SIZE);
    // Temp buffer used to copy buffered fft data to draw buffer
    private float[][] fft =
        new float[SignalProcessor.DEFAULT_FFT_WINDOW_COUNT][SignalProcessor.DEFAULT_FFT_30HZ_WINDOW_SIZE];

    /**
     * Interface definition for a callback to be invoked when signal sample rate or number of channels changes.
     */
    public interface OnSignalPropertyChangeListener {
        /**
         * Called when signal sample rate changes.
         *
         * @param sampleRate The new sample rate.
         */
        void onSampleRateChange(int sampleRate);

        /**
         * Called when number of channels changes.
         *
         * @param channelCount The new number of channels.
         */
        void onChannelCountChange(int channelCount);
    }

    private OnSignalPropertyChangeListener listener;

    // Private constructor through which we create singleton instance
    private ProcessingBuffer() {
        createSampleBuffers(channelCount);
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
     * Registers a callback to be invoked when signal sample rate or number of channel changes.
     *
     * @param listener The callback that will be run. This value may be {@code null}.
     */
    public void setOnSignalPropertyChangeListener(@Nullable OnSignalPropertyChangeListener listener) {
        this.listener = listener;
    }

    /**
     * Sets sample rate of the processed signal.
     */
    void setSampleRate(int sampleRate) {
        if (this.sampleRate == sampleRate) return;

        clearBuffers();
        createSampleBuffers(channelCount);

        this.sampleRate = sampleRate;

        if (listener != null) listener.onSampleRateChange(sampleRate);
    }

    /**
     * Sets number of processed channels.
     */
    void setChannelCount(int channelCount) {
        if (this.channelCount == channelCount) return;

        clearBuffers();
        createSampleBuffers(channelCount);

        this.channelCount = channelCount;

        if (listener != null) listener.onChannelCountChange(channelCount);
    }

    /**
     * Copies as many samples, averaged samples, event indices and event names accompanying the sample data currently
     * in the buffer as available to the specified {@code samleBuffer}, {@code averagedSamplesBuffer}, {@code indices}
     * and {@code events}.
     *
     * @return Number of copied events.
     */
    public int copy(@NonNull MultichannelSignalDrawBuffer sampleDrawBuffer,
        @NonNull MultichannelSignalDrawBuffer averagedDrawSamplesBuffer, @NonNull int[] indices,
        @NonNull String[] events, @NonNull FftDrawBuffer fftDrawBuffer) {
        synchronized (lock) {
            // copy samples
            if (sampleBuffers != null) {
                for (int i = 0; i < channelCount; i++) {
                    if (sampleBuffers[i] != null) {
                        int count = sampleBuffers[i].get(samples);
                        if (count > 0) sampleDrawBuffer.add(i, samples, count);
                    }
                }
            }
            // copy averaged samples
            if (averagedSamplesBuffers != null) {
                for (int i = 0; i < channelCount; i++) {
                    if (averagedSamplesBuffers[i] != null) {
                        int count = averagedSamplesBuffers[i].get(samples);
                        if (count > 0) averagedDrawSamplesBuffer.add(i, samples, count);
                    }
                }
            }
            // copy events
            System.arraycopy(eventIndices, 0, indices, 0, eventCount);
            System.arraycopy(eventNames, 0, events, 0, eventCount);
            // copy fft data
            // TODO: 06-Mar-19 UNCOMMENT THIS WHEN FFT PROCESSING DEVELOPMENT CONTINUES
            //if (fftBuffer != null) {
            //    int count = fftBuffer.get(fft);
            //    if (count > 0) fftDrawBuffer.add(fft, count);
            //}

            return eventCount;
        }
    }

    /**
     * Adds specified {@code samplesWithEvents} and {@code averagedSamples} to the buffer.
     */
    void add(@NonNull SamplesWithEvents samplesWithEvents, @NonNull SamplesWithEvents averagedSamples,
        @NonNull FftData fftData) {
        synchronized (lock) {
            // add samples to signal ring buffer
            if (sampleBuffers != null && sampleBuffers.length == samplesWithEvents.samplesM.length) {
                for (int i = 0; i < samplesWithEvents.samplesM.length; i++) {
                    if (sampleBuffers[i] != null) {
                        sampleBuffers[i].put(samplesWithEvents.samplesM[i], 0, samplesWithEvents.sampleCountM[i]);
                    }
                }
            }
            // add samples to averaged signal ring buffer
            if (averagedSamplesBuffers != null && averagedSamplesBuffers.length == averagedSamples.samplesM.length) {
                for (int i = 0; i < averagedSamples.samplesM.length; i++) {
                    if (averagedSamplesBuffers[i] != null) {
                        averagedSamplesBuffers[i].put(averagedSamples.samplesM[i], 0, averagedSamples.sampleCountM[i]);
                    }
                }
            }

            // skip events that are no longer visible (they will be overwritten when adding new ones)
            int removeIndices;
            for (removeIndices = 0; removeIndices < eventCount; removeIndices++) {
                if (eventIndices[removeIndices] - samplesWithEvents.sampleCount < 0) continue;

                break;
            }
            // update indices of existing events
            int eventCounter = 0;
            for (int i = removeIndices; i < eventCount; i++) {
                eventIndices[eventCounter] = eventIndices[i] - samplesWithEvents.sampleCount;
                eventNames[eventCounter++] = eventNames[i];
            }
            // add new events
            int baseIndex = sampleBufferSize - samplesWithEvents.sampleCount;
            for (int i = 0; i < samplesWithEvents.eventCount; i++) {
                eventIndices[eventCounter] = baseIndex + samplesWithEvents.eventIndices[i];
                eventNames[eventCounter++] = samplesWithEvents.eventNames[i];
            }
            eventCount = eventCount - removeIndices + samplesWithEvents.eventCount;

            // save last sample index (playhead)
            lastSampleIndex = samplesWithEvents.lastSampleIndex;

            // add fft data to buffer
            // TODO: 06-Mar-19 UNCOMMENT THIS WHEN FFT PROCESSING DEVELOPMENT CONTINUES
            //if (fftBuffer != null) fftBuffer.put(fftData.fft, 0, fftData.windowCount);
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
     * Clears the sample data ring buffers, events collections and resets last read byte position.
     */
    void clearBuffers() {
        synchronized (lock) {
            if (sampleBuffers != null) {
                for (CircularShortBuffer sb : sampleBuffers) {
                    if (sb != null) sb.clear();
                }
            }
            if (averagedSamplesBuffers != null) {
                for (CircularShortBuffer sb : averagedSamplesBuffers) {
                    if (sb != null) sb.clear();
                }
            }
            eventCount = 0;
            lastSampleIndex = 0;
            fftBuffer.clear();
        }
    }

    // Creates all sample data buffers
    private void createSampleBuffers(int channelCount) {
        synchronized (lock) {
            sampleBuffers = new CircularShortBuffer[channelCount];
            averagedSamplesBuffers = new CircularShortBuffer[channelCount];
            sampleBufferSize = SignalProcessor.getProcessedSamplesCount();
            for (int i = 0; i < channelCount; i++) {
                sampleBuffers[i] = new CircularShortBuffer(sampleBufferSize);
                // Size of the average samples buffer
                averagedSamplesBuffers[i] = new CircularShortBuffer(SignalProcessor.getProcessedAveragedSamplesCount());
            }
            fftBuffer = new CircularFloatArrayBuffer(SignalProcessor.getProcessedFftWindowCount(),
                SignalProcessor.getProcessedFftWindowSize());
        }
    }
}
