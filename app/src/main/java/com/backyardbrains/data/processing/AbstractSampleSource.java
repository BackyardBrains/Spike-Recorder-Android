package com.backyardbrains.data.processing;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.audio.Filters;
import com.backyardbrains.filters.Filter;
import java.util.concurrent.atomic.AtomicBoolean;
import org.greenrobot.essentials.io.CircularByteBuffer;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public abstract class AbstractSampleSource implements SampleSource {

    // Additional filters that should be applied to input data
    protected static final Filters FILTERS = new Filters();

    /**
     * Background thread that reads data from the local buffer filled by the derived class and passes it to {@link
     * ProcessingThread}.
     */
    protected class ReadThread extends Thread {

        private AtomicBoolean working = new AtomicBoolean(true);
        private AtomicBoolean paused = new AtomicBoolean(false);

        @Override public void run() {
            while (working.get()) {
                if (!paused.get()) {
                    int size = readBuffer.get(readBufferData);
                    if (size > 0) processingBuffer.put(readBufferData, 0, size);
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

    /**
     * Background thread that processes the data read by the {@link ReadThread} and passes it to {@link
     * SampleSourceListener}.
     */
    protected class ProcessingThread extends Thread {

        private AtomicBoolean working = new AtomicBoolean(true);
        private AtomicBoolean paused = new AtomicBoolean(false);

        @Override public void run() {
            while (working.get()) {
                if (!paused.get()) {
                    int size = processingBuffer.get(processingBufferData);
                    if (size > 0) {
                        if (sampleSourceListener == null) {
                            // we should process the incoming data even if there is no listener
                            processIncomingData(processingBufferData, size);
                        } else {
                            // forward received samples to SampleSourceListener
                            synchronized (sampleSourceListener) {
                                sampleSourceListener.onSamplesReceived(processIncomingData(processingBufferData, size));
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

    @SuppressWarnings("WeakerAccess") final SampleSourceListener sampleSourceListener;

    private ReadThread readThread;
    @SuppressWarnings("WeakerAccess") CircularByteBuffer readBuffer;
    @SuppressWarnings("WeakerAccess") byte[] readBufferData;

    private ProcessingThread processingThread;
    @SuppressWarnings("WeakerAccess") CircularByteBuffer processingBuffer;
    @SuppressWarnings("WeakerAccess") byte[] processingBufferData;

    private int bufferSize;
    private int sampleRate;
    private int channelCount;

    public AbstractSampleSource(int bufferSize, @Nullable SampleSourceListener listener) {
        this.bufferSize = bufferSize;
        this.sampleSourceListener = listener;

        readBuffer = new CircularByteBuffer(bufferSize * 2);
        readBufferData = new byte[bufferSize];

        processingBuffer = new CircularByteBuffer(bufferSize * 2);
        processingBufferData = new byte[bufferSize];
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
            // reset filters
            FILTERS.setSampleRate(sampleRate);

            // inform interested parties what is the sample rate of this sample source
            if (sampleSourceListener != null) sampleSourceListener.onSampleRateDetected(sampleRate);

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
     * Whether this is a microphone input source.
     */
    public boolean isMicrophone() {
        return getType() == Type.MICROPHONE;
    }

    /**
     * Whether this is an usb input source.
     */
    public boolean isUsb() {
        return getType() == Type.USB;
    }

    /**
     * Whether this is a file input source.
     */
    public boolean isFile() {
        return getType() == Type.FILE;
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
        // start the read thread
        if (readThread == null) {
            readThread = new ReadThread();
            readThread.start();
        }
        // give chance to subclass to init resources and start writing data to buffer
        onInputStart();
    }

    /**
     * {@inheritDoc}
     */
    @Override public final void pause() {
        // pause threads
        if (readThread != null) readThread.pauseWorking();
        if (processingThread != null) processingThread.pauseWorking();
    }

    /**
     * {@inheritDoc}
     */
    @Override public final void resume() {
        // resume threads
        if (processingThread != null) processingThread.resumeWorking();
        if (readThread != null) readThread.resumeWorking();
    }

    /**
     * {@inheritDoc}
     */
    @Override public final void stop() {
        // give chance to subclass to clean resources
        onInputStop();
        // stop the read thread
        if (readThread != null) {
            readThread.stopWorking();
            readThread = null;
        }
        // stop the processing thread
        if (processingThread != null) {
            processingThread.stopWorking();
            processingThread = null;
        }
    }

    /**
     * Sets the size of the reading and processing buffers.
     *
     * @param size Size of the buffers in bytes.
     */
    protected final void setBufferSize(int size) {
        if (bufferSize < size) {
            bufferSize = size;

            readBuffer = new CircularByteBuffer(bufferSize);
            readBufferData = new byte[bufferSize];

            processingBuffer = new CircularByteBuffer(bufferSize);
            processingBufferData = new byte[bufferSize];
        }
    }

    /**
     * Subclasses should write any received data to buffer for further processing.
     */
    protected final void writeToBuffer(@NonNull byte[] data, int offset, int length) {
        readBuffer.put(data, offset, length);
        //processingBuffer.put(data, offset, length);
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
     * Called by {@link SampleSourceListener} before triggering the listener to convert incoming byte data to
     * sample data. If available (i.e. during playback) caller should also pass the index of the last passed byte
     * (playhead).
     * <p>
     * This method is called from background thread so implementation should not communicate with UI thread
     * directly.
     */
    @NonNull protected abstract SamplesWithEvents processIncomingData(byte[] data, int length);
}
