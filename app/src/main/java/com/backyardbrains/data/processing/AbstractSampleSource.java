package com.backyardbrains.data.processing;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.audio.Filters;
import com.backyardbrains.filters.Filter;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.BufferUtils;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.greenrobot.essentials.io.CircularByteBuffer;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public abstract class AbstractSampleSource implements SampleSource {

    // Number of seconds temp processing buffer should hold by default
    private static final int DEFAULT_PROCESSING_BUFFER_SIZE_IN_SEC = 2;
    // Number of seconds

    // Additional filters that should be applied to input data
    protected static final Filters FILTERS = new Filters();

    /**
     * Background thread that processes the data read from the local buffer filled by the derived class and passes it to
     * {@link OnSamplesReceivedListener}.
     */
    protected class ProcessingThread extends Thread {

        private AtomicBoolean working = new AtomicBoolean(true);
        private AtomicBoolean paused = new AtomicBoolean(false);
        private AtomicLong lastByteIndex = new AtomicLong(0);

        @Override public void run() {
            while (working.get()) {
                if (!paused.get()) {
                    int size = processingBuffer.get(processingBufferData);
                    if (size > 0) {
                        if (listener == null) {
                            // we should process the incoming data even if there is no listener
                            processIncomingData(processingBufferData, size, lastByteIndex.get());
                        } else {
                            // forward received samples to OnSamplesReceivedListener
                            synchronized (listener) {
                                listener.onSamplesReceived(
                                    processIncomingData(processingBufferData, size, lastByteIndex.get()));
                            }
                        }
                    }
                }
            }
        }

        void pauseWorking() {
            paused.set(true);
        }

        void resumeWorking() {
            paused.set(false);
        }

        void stopWorking() {
            working.set(false);
        }

        void setLastByteIndex(long lastByteIndex) {
            this.lastByteIndex.set(lastByteIndex);
        }
    }

    @SuppressWarnings("WeakerAccess") final OnSamplesReceivedListener listener;

    private ProcessingThread processingThread;
    @SuppressWarnings("WeakerAccess") CircularByteBuffer processingBuffer;
    @SuppressWarnings("WeakerAccess") byte[] processingBufferData;

    private int sampleRate;
    private int channelCount;

    public AbstractSampleSource(@Nullable OnSamplesReceivedListener listener) {
        this.listener = listener;

        sampleRate = AudioUtils.SAMPLE_RATE;

        processingBuffer = new CircularByteBuffer(BufferUtils.MAX_SAMPLE_BUFFER_SIZE * 2);
        processingBufferData = new byte[sampleRate * DEFAULT_PROCESSING_BUFFER_SIZE_IN_SEC];

        FILTERS.setSampleRate(sampleRate);
    }

    /**
     * Returns {@link Filter} that is used to additionally filter incoming data.
     */
    @Nullable public Filter getFilter() {
        return FILTERS.getFilter();
    }

    /**
     * Sets {@link Filter} that is used to additionally filter incoming data.
     */
    public void setFilter(@Nullable Filter filter) {
        FILTERS.setFilter(filter);
    }

    /**
     * Returns sample rate for this input source.
     */
    public int getSampleRate() {
        return sampleRate;
    }

    /**
     * Sets sample rate for this input source.
     */
    protected void setSampleRate(int sampleRate) {
        if (this.sampleRate != sampleRate) {
            processingBufferData = new byte[sampleRate * DEFAULT_PROCESSING_BUFFER_SIZE_IN_SEC];

            // reset filters
            FILTERS.setSampleRate(sampleRate);

            this.sampleRate = sampleRate;
        }
    }

    /**
     * Returns number of channels for this input source.
     */
    public int getChannelCount() {
        return channelCount;
    }

    /**
     * Sets number of channels for this input source.
     */
    @CallSuper protected void setChannelCount(int channelCount) {
        this.channelCount = channelCount;
    }

    /**
     * Whether this is an audio input source.
     */
    public boolean isAudio() {
        return getType() == Type.AUDIO;
    }

    /**
     * Whether this is an USB input source.
     */
    public boolean isUsb() {
        return getType() == Type.USB;
    }

    /**
     * {@inheritDoc}
     */
    @Override public final void start() {
        // start the processing thread
        if (processingThread == null) {
            processingThread = new ProcessingThread();
            processingThread.start();
        }
        // give chance to subclass to init resources and start writing data to buffer
        onInputStart();
    }

    /**
     * {@inheritDoc}
     */
    @Override public final void pause() {
        // pause thread
        if (processingThread != null) processingThread.pauseWorking();
    }

    /**
     * {@inheritDoc}
     */
    @Override public final void resume() {
        // resume thread
        if (processingThread != null) processingThread.resumeWorking();
    }

    /**
     * {@inheritDoc}
     */
    @Override public final void stop() {
        // give chance to subclass to clean resources
        onInputStop();
        // stop the processing thread
        if (processingThread != null) {
            processingThread.stopWorking();
            processingThread = null;
        }
    }

    /**
     * Sets the size of the processing buffer.
     *
     * @param size Size of the buffer in bytes.
     */
    protected final void setProcessingBufferSize(int size) {
        if (processingBufferData.length < size) processingBufferData = new byte[size];
    }

    /**
     * Subclasses should write any received data to buffer for further processing.
     */
    protected final void writeToBuffer(@NonNull byte[] data, int offset, int length) {
        processingBuffer.put(data, offset, length);
    }

    /**
     * Subclasses should write any received data to buffer for further processing. If available (when doing playback),
     * subclasses should also pass an index of the last written byte.
     */
    protected final void writeToBuffer(@NonNull byte[] data, int offset, int length, long lastByteIndex) {
        processingBuffer.put(data, offset, length);
        if (processingThread != null) processingThread.setLastByteIndex(lastByteIndex);
    }

    /**
     * Called during data reading initialization from the input stream. Implementation should start the actual reading
     * of data from the concrete source.
     */
    protected abstract void onInputStart();

    /**
     * Called during data reading finalization from the input stream. Implementation should stop the actual reading
     * of data from the concrete source and clear all resources.
     */
    protected abstract void onInputStop();

    /**
     * Called by {@link OnSamplesReceivedListener} before triggering the listener to convert incoming byte data to
     * sample data. If available (i.e. during playback) caller should also pass the index of the last passed byte
     * (playhead).
     * <p>
     * This method is called from background thread so implementation should not communicate with UI thread
     * directly.
     */
    @NonNull protected abstract SamplesWithEvents processIncomingData(byte[] data, int length, long lastByteIndex);
}
