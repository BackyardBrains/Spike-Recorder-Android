package com.backyardbrains.dsp;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.JniUtils;
import java.util.concurrent.atomic.AtomicBoolean;
import org.greenrobot.essentials.io.CircularByteBuffer;

import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class SignalProcessor implements SignalSource.Processor {

    private static final String TAG = makeLogTag(SignalProcessor.class);

    // Maximum time that should be processed when processing audio signal
    private static final float MAX_AUDIO_PROCESSING_TIME = 6f; // 6 seconds
    // Maximum time that should be processed when processing sample stream
    private static final float MAX_SAMPLE_STREAM_PROCESSING_TIME = 12f; // 12 seconds
    // Maximum time that should be processed when averaging signal
    private static final float MAX_THRESHOLD_PROCESSING_TIME = 2.4f; // 2.4 seconds

    // Default samples buffer size
    public static final int DEFAULT_SAMPLE_BUFFER_SIZE =
        (int) (MAX_AUDIO_PROCESSING_TIME * AudioUtils.DEFAULT_SAMPLE_RATE);
    // Default averaged samples buffer size
    public static final int DEFAULT_AVERAGED_SAMPLE_BUFFER_SIZE =
        (int) (MAX_THRESHOLD_PROCESSING_TIME * AudioUtils.DEFAULT_SAMPLE_RATE);
    // Default channel count
    public static final int DEFAULT_CHANNEL_COUNT = AudioUtils.DEFAULT_CHANNEL_COUNT;
    // Default frame size (samples per channel)
    public static final int DEFAULT_FRAME_SIZE =
        (int) Math.floor((float) DEFAULT_SAMPLE_BUFFER_SIZE / DEFAULT_CHANNEL_COUNT);

    private static int processedSamplesCount = (int) (MAX_AUDIO_PROCESSING_TIME * AudioUtils.DEFAULT_SAMPLE_RATE);
    private static int processedAveragedSamplesCount =
        (int) (MAX_THRESHOLD_PROCESSING_TIME * AudioUtils.DEFAULT_SAMPLE_RATE);
    private static int maxProcessedSamplesCount = processedSamplesCount;
    // Lock used when reading/writing samples and events
    private static final Object lock = new Object();

    /**
     * Interface definition for a callback to be invoked after incoming data has been processed.
     */
    interface OnProcessingListener {
        void onDataProcessed(@NonNull SamplesWithEvents samplesWithEvents);
    }

    private OnProcessingListener listener;

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
    @SuppressWarnings("WeakerAccess") final CircularByteBuffer ringBuffer =
        new CircularByteBuffer(DEFAULT_SAMPLE_BUFFER_SIZE * 2);
    @SuppressWarnings("WeakerAccess") final byte[] buffer = new byte[DEFAULT_SAMPLE_BUFFER_SIZE * 2];

    private SamplesWithEvents samplesWithEvents = new SamplesWithEvents(DEFAULT_CHANNEL_COUNT, DEFAULT_FRAME_SIZE);
    private SamplesWithEvents averagedSamplesWithEvents =
        new SamplesWithEvents(DEFAULT_CHANNEL_COUNT, DEFAULT_AVERAGED_SAMPLE_BUFFER_SIZE);

    // Reference to the data manager that stores and processes the data
    private ProcessingBuffer processingBuffer;

    // Source if the incoming signal
    private AbstractSignalSource signalSource;

    // Whether incoming signal should be averaged or not
    private boolean signalAveraging;

    SignalProcessor(@Nullable OnProcessingListener listener) {
        this.listener = listener;

        processingThread = new ProcessingThread();
        processingBuffer = ProcessingBuffer.get();
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

        // calculate max number of processed samples
        calculateMaxNumberOfProcessedSamples(sampleRate);

        // pass sample rate to processing buffer
        if (processingBuffer != null) processingBuffer.setSampleRate(sampleRate);

        // pass sample rate to native code
        JniUtils.setSampleRate(sampleRate);
    }

    /**
     * {@inheritDoc}
     *
     * @param channelCount New channel count.
     */
    @Override public void onChannelCountChanged(int channelCount) {
        if (channelCount < 1) return; // channel count needs to be greater or equal to 1

        // pass channel count to processing buffer
        if (processingBuffer != null) processingBuffer.setChannelCount(channelCount);

        synchronized (lock) {
            // reset temp buffer for samples and events
            samplesWithEvents = new SamplesWithEvents(channelCount, DEFAULT_FRAME_SIZE);
            // reset temp buffer for threshold samples and events
            averagedSamplesWithEvents = new SamplesWithEvents(channelCount, DEFAULT_AVERAGED_SAMPLE_BUFFER_SIZE);
        }

        // pass channel count to native code
        JniUtils.setChannelCount(channelCount);
    }

    /**
     * Max number of samples that can be processed by the current signal source.
     */
    public static int getProcessedSamplesCount() {
        return processedSamplesCount;
    }

    /**
     * Max number of averaged samples that can be processed by the current signal source.
     */
    public static int getProcessedAveragedSamplesCount() {
        return processedAveragedSamplesCount;
    }

    /**
     * Max number of samples that can be processed by the current signal source. The difference between this method and
     * {@link #getProcessedSamplesCount()} is that this method takes into account whether signal is being averaged or
     * not.
     */
    public static int getMaxProcessedSamplesCount() {
        return maxProcessedSamplesCount;
    }

    /**
     * Returns sample rate for the current signal source.
     */
    int getSampleRate() {
        return signalSource != null ? signalSource.getSampleRate() : AudioUtils.DEFAULT_SAMPLE_RATE;
    }

    /**
     * Returns number of channel of the current signal source.
     */

    int getChannelCount() {
        return signalSource != null ? signalSource.getChannelCount() : DEFAULT_CHANNEL_COUNT;
    }

    /**
     * A data source that will provide data to data processor and notify it when different events occur.
     */
    void setSignalSource(@NonNull AbstractSignalSource signalSource) {
        signalSource.setProcessor(this);

        this.signalSource = signalSource;

        if (processingBuffer != null) processingBuffer.clearBuffers();

        // let's notify any interested party of the data source initial sample rate and channel count
        onSampleRateChanged(signalSource.getSampleRate());
        onChannelCountChanged(signalSource.getChannelCount());
    }

    /**
     * Sets whether incoming signal should be averaged.
     */
    void setSignalAveraging(boolean signalAveraging) {
        this.signalAveraging = signalAveraging;

        // calculate max number of processed samples
        calculateMaxNumberOfProcessedSamples(getSampleRate());
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

    //private final Benchmark benchmark = new Benchmark("AUDIO_DATA_PROCESSING").warmUp(200)
    //    .sessions(10)
    //    .measuresPerSession(200)
    //    .logBySession(false)
    //    .listener(() -> {
    //        //EventBus.getDefault().post(new ShowToastEvent("PRESS BACK BUTTON!!!!"));
    //    });

    //private final Benchmark benchmarkT =
    //    new Benchmark("THRESHOLD").warmUp(10).sessions(10).measuresPerSession(10).logBySession(false).listener(() -> {
    //        //EventBus.getDefault().post(new ShowToastEvent("PRESS BACK BUTTON!!!!"));
    //    });

    @SuppressWarnings("WeakerAccess") void processData(@NonNull byte[] buffer, int length) {
        synchronized (lock) {
            //benchmark.start();
            signalSource.processIncomingData(samplesWithEvents, buffer, length);
            //benchmark.end();
            //benchmarkT.start();
            JniUtils.processThreshold(averagedSamplesWithEvents, samplesWithEvents, signalAveraging);
            //benchmarkT.end();

            // forward received samples to Processing Service
            if (listener != null) listener.onDataProcessed(samplesWithEvents);

            // add to buffer
            if (processingBuffer != null) processingBuffer.add(samplesWithEvents, averagedSamplesWithEvents);
        }
    }

    // Set max number of samples that can be processed usually and in threshold
    private void calculateMaxNumberOfProcessedSamples(int sampleRate) {
        processedSamplesCount =
            signalSource != null && signalSource.isUsb() ? (int) (MAX_SAMPLE_STREAM_PROCESSING_TIME * sampleRate)
                : (int) (MAX_AUDIO_PROCESSING_TIME * sampleRate);
        processedAveragedSamplesCount = (int) (MAX_THRESHOLD_PROCESSING_TIME * sampleRate);
        // this is queried by renderer to know how big drawing surface should be
        maxProcessedSamplesCount = signalAveraging ? processedAveragedSamplesCount : processedSamplesCount;
    }
}
