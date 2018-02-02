package com.backyardbrains.audio;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.angrygoat.buffer.CircularByteBuffer;
import com.backyardbrains.filters.Filter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <ticapeca at gmail.com.
 */
public abstract class InputSource {

    private static final String TAG = makeLogTag(InputSource.class);

    /**
     * Defines different input source types.
     */
    @Retention(RetentionPolicy.SOURCE) @IntDef({
        Type.AUDIO, Type.USB
    }) protected @interface Type {
        /**
         * Audio input source.
         */
        int AUDIO = 0;
        /**
         * Usb input source.
         */
        int USB = 1;
    }

    // Additional filters that should be applied to input data
    protected static final Filters FILTERS = new Filters();

    // 16MB for initial buffer size until sample rate is set
    private static final int DEFAULT_BUFFER_SIZE = 16 * 1024;
    // After sample rate is set buffer will change to be 2 seconds long
    private static final int BUFFERED_SECONDS = 2;

    /**
     * Interface definition for a callback to be invoked when chunk of data is received from the input source.
     */
    public interface OnSamplesReceivedListener {
        /**
         * Called when chunk of data is received.
         *
         * @param data The received data.
         */
        void onSamplesReceived(short[] data);
    }

    private final OnSamplesReceivedListener listener;

    /**
     * Background thread that reads data from the local buffer filled by the subclass and passes it to {@link
     * OnSamplesReceivedListener}.
     */
    protected class ReadThread extends Thread {

        private AtomicBoolean working = new AtomicBoolean(true);
        private AtomicBoolean paused = new AtomicBoolean(false);

        @Override public void run() {
            while (working.get()) {
                if (!paused.get()) {
                    int size = readBuffer.peekSize();
                    if (size > 0) {
                        byte[] data = new byte[size];
                        readBuffer.read(data, data.length, false);

                        processingBuffer.write(data);
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

    protected class ProcessingThread extends Thread {

        private AtomicBoolean working = new AtomicBoolean(true);

        @Override public void run() {
            while (working.get()) {
                int size = processingBuffer.peekSize();
                if (size > 0) {
                    byte[] data = new byte[size];
                    processingBuffer.read(data, data.length, false);

                    // forward received samples to OnSamplesReceivedListener
                    synchronized (listener) {
                        listener.onSamplesReceived(processIncomingData(data));
                    }
                }
            }
        }

        void stopWorking() {
            working.set(false);
        }
    }

    private ReadThread readThread;
    private CircularByteBuffer readBuffer = new CircularByteBuffer(DEFAULT_BUFFER_SIZE);
    private ProcessingThread processingThread;
    private CircularByteBuffer processingBuffer = new CircularByteBuffer(DEFAULT_BUFFER_SIZE);

    private int sampleRate;
    private int channelCount;

    public InputSource(@NonNull OnSamplesReceivedListener listener) {
        this.listener = listener;
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
    protected void setChannelCount(int channelCount) {
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
     * Starts reading input source data and passing it to the set {@link OnSamplesReceivedListener}.
     */
    public final void start() {
        // start the processing thread
        if (processingBuffer != null) {
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
     * Pauses reading input source data.
     */
    public final void pause() {
        if (readThread != null) readThread.pauseWorking();
    }

    /**
     * Resumes reading input source data.
     */
    public final void resume() {
        if (readThread != null) readThread.resumeWorking();
    }

    /**
     * Stops reading input source data.
     */
    public final void stop() {
        // give chance to subclass to clean resources
        onInputStop();
        // stop the processing thread
        if (processingThread != null) {
            processingThread.stopWorking();
            processingThread = null;
        }
        // stop the read thread
        if (readThread != null) {
            readThread.stopWorking();
            readThread = null;
        }
    }

    /**
     * Subclasses should write any received data to buffer for further processing.
     */
    protected final void writeToBuffer(@NonNull byte[] data) {
        readBuffer.write(data);
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
    protected abstract short[] processIncomingData(byte[] data);

    /**
     * Implementation should return type of the input source. One of {@link Type} available options.
     */
    protected abstract @Type int getType();
}
