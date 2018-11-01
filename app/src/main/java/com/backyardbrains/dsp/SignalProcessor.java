package com.backyardbrains.dsp;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.Benchmark;
import com.backyardbrains.utils.JniUtils;
import java.util.concurrent.atomic.AtomicBoolean;
import org.greenrobot.essentials.io.CircularByteBuffer;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
class SignalProcessor implements SignalSource.Processor {

    // Maximum time that should be processed when processing audio signal
    private static final float MAX_AUDIO_PROCESSING_TIME = 6f; // 6 seconds
    // Maximum time that should be processed when processing sample stream
    private static final float MAX_SAMPLE_STREAM_PROCESSING_TIME = 12f; // 12 seconds
    // Maximum time that should be processed when averaging signal
    private static final float MAX_THRESHOLD_PROCESSING_TIME = 2.4f; // 2.4 seconds
    // Default samples buffer size
    private static final int DEFAULT_SAMPLE_BUFFER_SIZE =
        (int) (MAX_AUDIO_PROCESSING_TIME * AudioUtils.DEFAULT_SAMPLE_RATE);
    // Default channel count
    private static final int DEFAULT_CHANNEL_COUNT = 1;

    /**
     * Interface definition for a callback to be invoked after incoming data has been processed.
     */
    interface OnProcessingListener {
        void onDataProcessed(@NonNull SamplesWithEvents samplesWithEvents);

        void onDataSampleRateChange(int sampleRate);

        void onDataChannelCountChange(int channelCount);
    }

    private final OnProcessingListener listener;

    /**
     * Background thread that processes the data from the local buffer filled by sample source and passes it to {@link
     * SignalSource.Processor}
     */
    protected class ProcessingThread extends Thread {

        ProcessingThread() {
            working.set(true);
        }

        @Override public void run() {
            while (working.get()) {
                if (!paused.get()) {
                    int length = ringBuffer.get(buffer);
                    if (length > 0) processData(buffer, length);
                }
            }
        }
    }

    private ProcessingThread processingThread;
    @SuppressWarnings("WeakerAccess") AtomicBoolean working = new AtomicBoolean();
    @SuppressWarnings("WeakerAccess") AtomicBoolean paused = new AtomicBoolean();
    @SuppressWarnings("WeakerAccess") CircularByteBuffer ringBuffer;
    @SuppressWarnings("WeakerAccess") byte[] buffer;
    SamplesWithEvents samplesWithEvents = new SamplesWithEvents(DEFAULT_CHANNEL_COUNT, DEFAULT_SAMPLE_BUFFER_SIZE);

    // Reference to the data manager that stores and processes the data
    private ProcessingBuffer processingBuffer;

    private AbstractSignalSource signalSource;

    // Whether incoming signal should be averaged or not
    private boolean signalAveraging;
    // 0-based index of the selected channel
    private int selectedChannel;

    SignalProcessor(@Nullable OnProcessingListener onProcessingListener) {
        this.listener = onProcessingListener;

        processingThread = new ProcessingThread();
        processingBuffer = ProcessingBuffer.get();

        setBufferSizes(AudioUtils.DEFAULT_SAMPLE_RATE);
    }

    /**
     * {@inheritDoc}
     *
     * @param data The buffer that contains received data.
     * @param length The length of the received data.
     */
    @Override public void onDataReceived(@NonNull byte[] data, int length) {
        ringBuffer.put(data, 0, length);
    }

    /**
     * {@inheritDoc}
     *
     * @param sampleRate New sample rate.
     */
    @Override public void onSampleRateChanged(int sampleRate) {
        if (sampleRate <= 0) return; // sample rate needs to be positive

        setBufferSizes(sampleRate);

        if (listener != null) listener.onDataSampleRateChange(sampleRate);
    }

    /**
     * {@inheritDoc}
     *
     * @param channelCount New channel count.
     */
    @Override public void onChannelCountChanged(int channelCount) {
        if (channelCount < 1) return; // channel count needs to be greater or equal to 1

        samplesWithEvents = new SamplesWithEvents(channelCount, DEFAULT_SAMPLE_BUFFER_SIZE);

        if (listener != null) listener.onDataChannelCountChange(channelCount);
    }

    /**
     * A data source that will provide data to data processor and notify it when different events occur.
     */
    void setSignalSource(@NonNull AbstractSignalSource signalSource) {
        signalSource.setProcessor(this);

        this.signalSource = signalSource;

        // let's notify any interested party of the data source initial sample rate and channel count
        onSampleRateChanged(signalSource.getSampleRate());
        onChannelCountChanged(signalSource.getChannelCount());
    }

    /**
     * Sets whether incoming signal should be averaged.
     */
    void setSignalAveraging(boolean signalAveraging) {
        this.signalAveraging = signalAveraging;
    }

    /**
     * Sets index of the currently selected channel.
     */
    void setSelectedChannel(int selectedChannel) {
        this.selectedChannel = selectedChannel;
    }

    /**
     * Starts processing incoming data and passes it to the set {@link OnProcessingListener}.
     */
    void start() {
        if (processingThread != null) processingThread.start();
    }

    /**
     * Pauses processing sample source data.
     */
    void pause() {
        paused.set(true);
    }

    /**
     * Resumes processing sample source data.
     */
    void resume() {
        paused.set(false);
    }

    /**
     * Stops processing sample source data.
     */
    void stop() {
        working.set(false);
        if (processingThread != null) processingThread = null;
        // destroy processing buffer
        processingBuffer = null;
    }

    private final Benchmark benchmark = new Benchmark("AUDIO_DATA_PROCESSING").warmUp(200)
        .sessions(10)
        .measuresPerSession(200)
        .logBySession(false)
        .listener(() -> {
            //EventBus.getDefault().post(new ShowToastEvent("PRESS BACK BUTTON!!!!"));
        });

    private final Benchmark benchmarkT =
        new Benchmark("THRESHOLD").warmUp(5).sessions(10).measuresPerSession(10).logBySession(false).listener(() -> {
            //EventBus.getDefault().post(new ShowToastEvent("PRESS BACK BUTTON!!!!"));
        });

    @SuppressWarnings("WeakerAccess") void processData(@NonNull byte[] buffer, int length) {
        //benchmark.start();
        signalSource.processIncomingData(buffer, length, samplesWithEvents);

        if (listener != null) {
            // forward received samples to Processor
            synchronized (SignalProcessor.class) {
                listener.onDataProcessed(samplesWithEvents);
            }
        }

        // add to buffer
        if (processingBuffer != null) processingBuffer.addToSampleBuffer(samplesWithEvents);
        //benchmark.end();

        // average signal if necessary
        if (signalAveraging) {
            benchmarkT.start();
            JniUtils.processThreshold(samplesWithEvents);
            benchmarkT.end();

            // add to buffer
            if (processingBuffer != null) processingBuffer.addToAveragedSamplesBuffer(samplesWithEvents);
        }
    }

    // Sets buffer sizes to global and local processing buffers
    private void setBufferSizes(int sampleRate) {
        if (processingBuffer != null) {
            int bufferSize =
                signalSource != null && signalSource.isUsb() ? (int) (MAX_SAMPLE_STREAM_PROCESSING_TIME * sampleRate)
                    : (int) (MAX_AUDIO_PROCESSING_TIME * sampleRate);
            processingBuffer.setSampleBufferSize(bufferSize);
            processingBuffer.setAveragedSamplesBufferSize((int) (MAX_THRESHOLD_PROCESSING_TIME * sampleRate));

            bufferSize = signalAveraging ? processingBuffer.getAveragedSamplesBufferSize()
                : processingBuffer.getSampleBufferSize();
            ringBuffer = new CircularByteBuffer(bufferSize * 2);
            buffer = new byte[bufferSize * 2];
        }
    }
}
