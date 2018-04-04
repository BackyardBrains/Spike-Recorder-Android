package com.backyardbrains.data.processing;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.angrygoat.buffer.CircularByteBuffer;
import com.backyardbrains.audio.Filters;
import com.backyardbrains.filters.Filter;
import com.backyardbrains.utils.SampleStreamUtils;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Tihomir Leka <ticapeca at gmail.com.
 */
public abstract class AbstractSampleSource implements SampleSource {

    // Additional filters that should be applied to input data
    protected static final Filters FILTERS = new Filters();

    // Number of seconds processing buffer should hold
    private static final int BUFFERED_SECONDS = 2;
    // Initial processing buffer size
    private static final int DEFAULT_PROCESSING_BUFFER_SIZE = BUFFERED_SECONDS * SampleStreamUtils.SAMPLE_RATE;

    @SuppressWarnings("WeakerAccess") final OnSamplesReceivedListener listener;

    /**
     * Background thread that processes the data read from the local buffer filled by the derived class and passes it to
     * {@link OnSamplesReceivedListener}.
     */
    protected class ProcessingThread extends Thread {

        private AtomicBoolean working = new AtomicBoolean(true);
        private AtomicBoolean paused = new AtomicBoolean(false);

        @Override public void run() {
            while (working.get()) {
                if (!paused.get()) {
                    if (processingBuffer.peekSize() > 0) {
                        if (processingBufferData == null
                            || processingBufferData.length != processingBuffer.peekSize()) {
                            processingBufferData = new byte[processingBuffer.peekSize()];
                        }
                        processingBuffer.read(processingBufferData, processingBufferData.length, true);

                        if (listener == null) {
                            // we should process the incoming data even if there is no listener
                            processIncomingData(processingBufferData);
                        } else {
                            // forward received samples to OnSamplesReceivedListener
                            synchronized (listener) {
                                listener.onSamplesReceived(processIncomingData(processingBufferData));
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
    }

    private ProcessingThread processingThread;
    @SuppressWarnings("WeakerAccess") final CircularByteBuffer processingBuffer;
    @SuppressWarnings("WeakerAccess") byte[] processingBufferData;

    private int sampleRate;
    private int channelCount;

    public AbstractSampleSource(@Nullable OnSamplesReceivedListener listener) {
        this.listener = listener;

        this.processingBuffer = new CircularByteBuffer(DEFAULT_PROCESSING_BUFFER_SIZE);
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
        this.sampleRate = sampleRate;
        // recreate buffer to hold 2 seconds of data
        this.processingBuffer.setCapacity(sampleRate * BUFFERED_SECONDS);
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
        // pause processing thread
        if (processingThread != null) processingThread.pauseWorking();
    }

    /**
     * {@inheritDoc}
     */
    @Override public final void resume() {
        // resume processing thread
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
     * Returns size of the processing buffer.
     *
     * @return Size of the buffer in bytes.
     */
    protected final int getProcessingBufferSize() {
        return processingBuffer.getCapacity();
    }

    /**
     * Sets the size of the processing buffer.
     *
     * @param size Size of the buffer in bytes.
     */
    protected final void setProcessingBufferSize(int size) {
        processingBuffer.setCapacity(size);
    }

    /**
     * Subclasses should write any received data to buffer for further processing.
     */
    protected final void writeToBuffer(@NonNull byte[] data) {
        processingBuffer.write(data, 0, data.length, true);
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
     * sample data.
     * <p>
     * This method is called from background thread so implementation should not communicate with UI thread
     * directly.
     */
    @NonNull protected abstract DataProcessor.SamplesWithMarkers processIncomingData(byte[] data);
}
