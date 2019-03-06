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
    // We only look at approx. 30% of the fft data cause we only want to analyze low frequency
    private static final int FFT_30HZ_LENGTH = 32; // ~30%
    // Percent of overlap between to consecutive FFT windows
    private static final int WINDOW_OVERLAP_PERCENT = 99;

    // Computes base 2 logarithm of x
    private static double LOG2(float x) {
        return Math.log(x) / Math.log(2);
    }

    // Default samples buffer size
    static final int DEFAULT_SAMPLE_BUFFER_SIZE = (int) (MAX_AUDIO_PROCESSING_TIME * AudioUtils.DEFAULT_SAMPLE_RATE);
    // Default averaged samples buffer size
    public static final int DEFAULT_AVERAGED_SAMPLE_BUFFER_SIZE =
        (int) (MAX_THRESHOLD_PROCESSING_TIME * AudioUtils.DEFAULT_SAMPLE_RATE);
    // Default channel count
    public static final int DEFAULT_CHANNEL_COUNT = AudioUtils.DEFAULT_CHANNEL_COUNT;
    // Default frame size (samples per channel)
    public static final int DEFAULT_FRAME_SIZE =
        (int) Math.floor((float) DEFAULT_SAMPLE_BUFFER_SIZE / DEFAULT_CHANNEL_COUNT);
    // Default size of a single FFT window
    private static final int DEFAULT_FFT_FULL_WINDOW_SIZE = (int) Math.pow(2, LOG2(AudioUtils.DEFAULT_SAMPLE_RATE) + 2);

    // Default number of windows used for FFT analysis
    public static final int DEFAULT_FFT_WINDOW_COUNT;
    // Default number of FFF frequencies we want to take into account
    public static final int DEFAULT_FFT_30HZ_WINDOW_SIZE;

    static {
        DEFAULT_FFT_WINDOW_COUNT =
            (int) (DEFAULT_SAMPLE_BUFFER_SIZE / (DEFAULT_FFT_FULL_WINDOW_SIZE * (1.0f - (WINDOW_OVERLAP_PERCENT
                / 100.0f))));
        DEFAULT_FFT_30HZ_WINDOW_SIZE =
            (DEFAULT_FFT_FULL_WINDOW_SIZE * FFT_30HZ_LENGTH) / AudioUtils.DEFAULT_SAMPLE_RATE;
    }

    private static int processedSamplesCount = DEFAULT_SAMPLE_BUFFER_SIZE;
    private static int processedAveragedSamplesCount = DEFAULT_AVERAGED_SAMPLE_BUFFER_SIZE;
    private static int maxProcessedSamplesCount = processedSamplesCount;
    private static int processedFftWindowCount = DEFAULT_FFT_WINDOW_COUNT;
    private static int processedFftWindowSize = DEFAULT_FFT_30HZ_WINDOW_SIZE;
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
    private FftData fft = new FftData(DEFAULT_FFT_WINDOW_COUNT, DEFAULT_FFT_30HZ_WINDOW_SIZE);

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

        synchronized (lock) {
            // reset temp buffer for fft data
            fft = new FftData(DEFAULT_FFT_WINDOW_COUNT, processedFftWindowSize);
        }

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
     * Max number of FFT windows that can be processed by the current signal source.
     */
    public static int getProcessedFftWindowCount() {
        return processedFftWindowCount;
    }

    /**
     * Size of a single FFT window that can be processed by the current signal source.
     */
    public static int getProcessedFftWindowSize() {
        return processedFftWindowSize;
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

    //private final Benchmark benchmark =
    //    new Benchmark("AUDIO_DATA_PROCESSING").warmUp(200).sessions(10).measuresPerSession(200).logBySession(false);

    //private final Benchmark benchmarkT =
    //    new Benchmark("THRESHOLD").warmUp(10).sessions(10).measuresPerSession(10).logBySession(false);

    //private final Benchmark benchmarkF =
    //    new Benchmark("FFT").warmUp(10).sessions(10).measuresPerSession(10).logBySession(false);

    @SuppressWarnings("WeakerAccess") void processData(@NonNull byte[] buffer, int length) {
        synchronized (lock) {
            //benchmark.start();
            signalSource.processIncomingData(samplesWithEvents, buffer, length);
            //benchmark.end();
            //benchmarkT.start();
            JniUtils.processThreshold(averagedSamplesWithEvents, samplesWithEvents, signalAveraging);
            //benchmarkT.end();
            //benchmarkF.start();
            // TODO: 06-Mar-19 UNCOMMENT THIS WHEN FFT PROCESSING DEVELOPMENT CONTINUES
            //if (!signalAveraging) JniUtils.processFft(fft, samplesWithEvents);
            //benchmarkF.end();

            // forward received samples to Processing Service
            if (listener != null) listener.onDataProcessed(samplesWithEvents);

            // add to buffer
            if (processingBuffer != null) processingBuffer.add(samplesWithEvents, averagedSamplesWithEvents, fft);
        }
    }

    // Set max number of samples that can be processed in normal processing, in threshold and in fft
    private void calculateMaxNumberOfProcessedSamples(int sampleRate) {
        processedSamplesCount =
            signalSource != null && signalSource.isUsb() ? (int) (MAX_SAMPLE_STREAM_PROCESSING_TIME * sampleRate)
                : (int) (MAX_AUDIO_PROCESSING_TIME * sampleRate);
        processedAveragedSamplesCount = (int) (MAX_THRESHOLD_PROCESSING_TIME * sampleRate);
        // this is queried by renderer to know how big drawing surface should be
        maxProcessedSamplesCount = signalAveraging ? processedAveragedSamplesCount : processedSamplesCount;
        // fft
        int processedFftFullWindowSize = (int) Math.pow(2, LOG2(sampleRate) + 2);
        processedFftWindowSize = (processedFftFullWindowSize * FFT_30HZ_LENGTH) / sampleRate;
        processedFftWindowCount =
            (int) ((MAX_AUDIO_PROCESSING_TIME * sampleRate) / (processedFftFullWindowSize * (1.0f - (
                WINDOW_OVERLAP_PERCENT / 100.0f))));
    }
}
