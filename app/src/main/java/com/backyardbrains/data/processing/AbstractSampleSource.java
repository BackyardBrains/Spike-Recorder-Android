package com.backyardbrains.data.processing;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.audio.Filters;
import com.backyardbrains.filters.Filter;
import com.backyardbrains.utils.JniUtils;
import java.util.concurrent.atomic.AtomicBoolean;
import org.greenrobot.essentials.io.CircularByteBuffer;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public abstract class AbstractSampleSource implements SampleSource {

    @SuppressWarnings("WeakerAccess") static final String TAG = makeLogTag(AbstractSampleSource.class);

    // Additional filters that should be applied to input data
    protected static final Filters FILTERS = new Filters();

    /**
     * Background thread that processes the data from the local buffer filled by the derived class and passes it to
     * {@link SampleSourceListener}
     */
    protected class ProcessingThread extends Thread {
        @Override public void run() {
            while (working.get()) {
                if (!paused.get()) {
                    int size = ringBuffer.get(buffer);
                    if (size > 0) {
                        //LOGD(TAG, "PROCESSING: " + size);
                        if (sampleSourceListener == null) {
                            // we should process the incoming data even if there is no listener
                            processIncomingData(buffer, size);
                        } else {
                            // forward received samples to SampleSourceListener
                            synchronized (sampleSourceListener) {
                                sampleSourceListener.onSamplesReceived(processIncomingData(buffer, size));
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("WeakerAccess") final SampleSourceListener sampleSourceListener;

    @SuppressWarnings("WeakerAccess") AtomicBoolean working = new AtomicBoolean(true);
    @SuppressWarnings("WeakerAccess") AtomicBoolean paused = new AtomicBoolean(false);
    @SuppressWarnings("WeakerAccess") CircularByteBuffer ringBuffer;
    @SuppressWarnings("WeakerAccess") byte[] buffer;

    private ProcessingThread processingThread;

    // Updated during processing and on every cycle
    protected SamplesWithEvents samplesWithEvents;

    private int bufferSize;
    private int sampleRate;
    private int channelCount;

    public AbstractSampleSource(int bufferSize, @Nullable SampleSourceListener listener) {
        this.bufferSize = bufferSize;
        this.sampleSourceListener = listener;

        ringBuffer = new CircularByteBuffer(bufferSize * 2);
        buffer = new byte[bufferSize];

        samplesWithEvents = new SamplesWithEvents((int) (bufferSize * .5f));
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

        // pass filters to native code
        float low = (float) (filter != null ? filter.getLowCutOffFrequency() : -1f);
        float high = (float) (filter != null ? filter.getHighCutOffFrequency() : -1f);
        JniUtils.setFilters(low, high);
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
            LOGD(TAG, "SAMPLE RATE: " + sampleRate);

            // reset filters
            FILTERS.setSampleRate(sampleRate);

            // pass sample rate to native code
            JniUtils.setSampleRate(sampleRate);

            // inform interested parties what is the sample rate of this sample source
            if (sampleSourceListener != null) sampleSourceListener.onSampleRateDetected(sampleRate);

            this.sampleRate = sampleRate;
        }
    }

    /**
     * Sets the size of the processing buffer.
     *
     * @param bufferSize Size of the buffer in bytes.
     */
    public final void setBufferSize(int bufferSize) {
        if (this.bufferSize != bufferSize) {
            ringBuffer = new CircularByteBuffer(bufferSize * 2);
            buffer = new byte[bufferSize];
            samplesWithEvents = new SamplesWithEvents(bufferSize);

            this.bufferSize = bufferSize;
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
        if (this.channelCount != channelCount) {
            LOGD(TAG, "CHANNEL COUNT: " + channelCount);

            this.channelCount = channelCount;
        }
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
        // give chance to subclass to init resources and start writing data to buffer
        onInputStart();
    }

    /**
     * {@inheritDoc}
     */
    @Override public final void pause() {
        // pause
        paused.set(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override public final void resume() {
        // resume threads
        paused.set(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override public final void stop() {
        // give chance to subclass to clean resources
        onInputStop();
        // stop the processing thread
        working.set(false);
        if (processingThread != null) processingThread = null;
    }

    /**
     * Subclasses should write any received data to buffer for further processing.
     */
    protected final void writeToBuffer(@NonNull byte[] data, int offset, int length) {
        ringBuffer.put(data, offset, length);
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
