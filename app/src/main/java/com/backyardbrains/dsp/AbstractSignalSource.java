package com.backyardbrains.dsp;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public abstract class AbstractSignalSource implements SignalSource {

    @SuppressWarnings("WeakerAccess") static final String TAG = makeLogTag(AbstractSignalSource.class);

    // Reference to the data processor that's listening to this sample source
    private Processor processor;

    private int sampleRate;
    private int channelCount;
    private int bitsPerSample;

    public AbstractSignalSource(int sampleRate, int channelCount, int bitsPerSample) {
        setSampleRate(sampleRate);
        setChannelCount(channelCount);
        setBitsPerSample(bitsPerSample);
    }

    /**
     * Returns sample rate for this signal source.
     */
    public int getSampleRate() {
        return sampleRate;
    }

    /**
     * Sets sample rate for this signal source.
     */
    @CallSuper protected void setSampleRate(int sampleRate) {
        if (this.sampleRate != sampleRate) {
            LOGD(TAG, "setSampleRate(" + sampleRate + ")");

            // inform interested parties what is the sample rate of this sample source
            if (processor != null) processor.onSampleRateChanged(sampleRate);

            this.sampleRate = sampleRate;
        }
    }

    /**
     * Returns channel count for this input source.
     */
    public int getChannelCount() {
        return channelCount;
    }

    /**
     * Sets number of channels for this input source.
     */
    @CallSuper protected void setChannelCount(int channelCount) {
        if (this.channelCount != channelCount) {
            LOGD(TAG, "setChannelCount(" + channelCount + ")");

            // inform interested parties what is the channel count of this sample source
            if (processor != null) processor.onChannelCountChanged(channelCount);

            this.channelCount = channelCount;
        }
    }

    /**
     * Returns bits per sample for this signal source;
     */
    public int getBitsPerSample() {
        return bitsPerSample;
    }

    /**
     * Sets number of bits per sample for this input source.
     */
    @CallSuper protected void setBitsPerSample(int bitsPerSample) {
        if (this.bitsPerSample != bitsPerSample) {
            LOGD(TAG, "setBitsPerSample(" + bitsPerSample + ")");

            // inform interested parties what is the channel count of this sample source
            if (processor != null) processor.onBitsPerSampleChanged(bitsPerSample);

            this.bitsPerSample = bitsPerSample;
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
     * Sets a data processor to be notified on different sample stream event messages.
     *
     * @param processor The data processor that will be notified. This value may be {@code null}.
     */
    public void setProcessor(@Nullable Processor processor) {
        this.processor = processor;
    }

    /**
     * Subclasses should write any received data to buffer for further processing.
     */
    protected final void writeToBuffer(@NonNull byte[] data, int length) {
        if (processor != null) processor.onDataReceived(data, length);
    }

    /**
     * {@inheritDoc}
     */
    @Override public void pause() {
    }

    /**
     * {@inheritDoc}
     */
    @Override public void resume() {
    }

    /**
     * Called by {@link Processor} to convert incoming byte data to sample data. If available (i.e. during playback)
     * caller should also pass the index of the last passed byte (playhead). Processed samples should be passed back
     * inside provided {@link SignalData} object.
     * <p>
     * This method is called from background thread so implementation should not communicate with UI thread
     * directly.
     */
    public abstract void processIncomingData(@NonNull SignalData outData, byte[] inData, int inDataLength);

    /**
     * Returns type of the sample source. One of {@link SignalSource.Type} constants.
     */
    public abstract @SignalSource.Type int getType();
}
