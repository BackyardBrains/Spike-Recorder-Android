package com.backyardbrains.dsp;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.dsp.usb.AbstractUsbSignalSource;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.ExpansionBoardType;
import com.backyardbrains.utils.JniUtils;
import com.backyardbrains.utils.SignalAveragingTriggerType;
import com.backyardbrains.utils.SpikerBoxHardwareType;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import org.greenrobot.essentials.io.CircularByteBuffer;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class SignalProcessor implements SignalSource.Processor {

    @SuppressWarnings("unused") private static final String TAG = makeLogTag(SignalProcessor.class);

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

    // Default sample rate
    public static final int DEFAULT_SAMPLE_RATE = AudioUtils.DEFAULT_SAMPLE_RATE;
    // Default channel count
    public static final int DEFAULT_CHANNEL_COUNT = AudioUtils.DEFAULT_CHANNEL_COUNT;
    // Default channel config
    public static final boolean[] DEFAULT_CHANNEL_CONFIG =
        Arrays.copyOf(AudioUtils.DEFAULT_CHANNEL_CONFIG, AudioUtils.DEFAULT_CHANNEL_COUNT);
    // Default samples buffer size
    static final int DEFAULT_SAMPLE_BUFFER_SIZE = (int) (MAX_AUDIO_PROCESSING_TIME * AudioUtils.DEFAULT_SAMPLE_RATE);
    // Default averaged samples buffer size
    public static final int DEFAULT_AVERAGED_SAMPLE_BUFFER_SIZE =
        (int) (MAX_THRESHOLD_PROCESSING_TIME * AudioUtils.DEFAULT_SAMPLE_RATE);
    // Default frame size (samples per channel)
    public static final int DEFAULT_FRAME_SIZE =
        (int) Math.floor((float) DEFAULT_SAMPLE_BUFFER_SIZE / AudioUtils.DEFAULT_CHANNEL_COUNT);
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
        void onDataProcessed(@NonNull SignalData signalData);
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

    // Processing thread
    private ProcessingThread processingThread;
    // Whether processing is currently in progress
    @SuppressWarnings("WeakerAccess") AtomicBoolean working = new AtomicBoolean();
    // Whether processing is temporarily paused
    @SuppressWarnings("WeakerAccess") AtomicBoolean paused = new AtomicBoolean();
    // Ring buffer that holds raw signal data
    @SuppressWarnings("WeakerAccess") final CircularByteBuffer ringBuffer =
        new CircularByteBuffer(DEFAULT_SAMPLE_BUFFER_SIZE * 2);
    // Used for holding raw data during processing
    @SuppressWarnings("WeakerAccess") final byte[] buffer = new byte[DEFAULT_SAMPLE_BUFFER_SIZE * 2];

    // Holds signal data after processing raw signal together with processed events
    private SignalData signalData = new SignalData(AudioUtils.DEFAULT_CHANNEL_COUNT, DEFAULT_FRAME_SIZE);
    // Holds processed signal data of only visible channels
    private SignalData visibleSignalData = new SignalData(AudioUtils.DEFAULT_CHANNEL_COUNT, DEFAULT_FRAME_SIZE);
    // Holds processed signal data of only visible channels after averaging
    private SignalData averagedSignalData =
        new SignalData(AudioUtils.DEFAULT_CHANNEL_COUNT, DEFAULT_AVERAGED_SAMPLE_BUFFER_SIZE);
    // Holds processed signal data after FFT processing
    private FftData fft = new FftData(DEFAULT_FFT_WINDOW_COUNT, DEFAULT_FFT_30HZ_WINDOW_SIZE);

    // Reference to the singleton buffer for storing data after processing
    private final ProcessingBuffer processingBuffer;

    //
    private final SignalConfiguration signalConfiguration;

    // Incoming signal source
    private AbstractSignalSource signalSource;

    private final AbstractUsbSignalSource.OnSpikerBoxHardwareTypeDetectionListener spikerBoxDetectionListener =
        hardwareType -> {
            if (hardwareType == SpikerBoxHardwareType.NEURON_PRO || hardwareType == SpikerBoxHardwareType.MUSCLE_PRO) {
                setChannelConfig(new boolean[] { true, false });
            }
        };

    private final AbstractUsbSignalSource.OnExpansionBoardTypeDetectionListener expansionBoardDetectionListener =
        expansionBoardType -> {
            if (expansionBoardType == ExpansionBoardType.HAMMER) {
                setChannelConfig(new boolean[] { true, false, true });
            } else if (expansionBoardType == ExpansionBoardType.NONE) {
                setChannelConfig(new boolean[] { true, false });
            }
        };

    SignalProcessor(@Nullable OnProcessingListener listener) {
        this.listener = listener;

        processingThread = new ProcessingThread();
        processingBuffer = ProcessingBuffer.get();
        signalConfiguration = SignalConfiguration.get();
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
        // calculate max number of processed samples
        calculateMaxNumberOfProcessedSamples(sampleRate, signalConfiguration.isSignalAveraging());

        // update signal configuration
        signalConfiguration.setSampleRate(sampleRate);

        // reset processing buffer
        processingBuffer.resetAllSampleBuffers(signalConfiguration.getChannelCount(),
            signalConfiguration.getVisibleChannelCount());

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
        // update signal configuration
        signalConfiguration.setChannelCount(channelCount);

        // reset processing buffer
        processingBuffer.resetAllSampleBuffers(channelCount, signalConfiguration.getVisibleChannelCount());

        synchronized (lock) {
            // reset buffers
            signalData = new SignalData(channelCount, DEFAULT_FRAME_SIZE);
            visibleSignalData = new SignalData(signalConfiguration.getVisibleChannelCount(), DEFAULT_FRAME_SIZE);
            averagedSignalData =
                new SignalData(signalConfiguration.getVisibleChannelCount(), DEFAULT_AVERAGED_SAMPLE_BUFFER_SIZE);
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
        return signalConfiguration.getSampleRate();
    }

    /**
     * Returns number of channel of the current signal source.
     */
    int getChannelCount() {
        return signalConfiguration.getChannelCount();
    }

    /**
     * Sets what incoming signal's channels are shown/hidden.
     */
    private void setChannelConfig(boolean[] channelConfig) {
        if (channelConfig.length != getChannelCount()) return;

        // update signal configuration
        signalConfiguration.setChannelConfig(channelConfig);

        final int visibleChannelCount = signalConfiguration.getVisibleChannelCount();

        // reset processing buffer
        processingBuffer.resetAveragedSamplesBuffer(visibleChannelCount);

        synchronized (lock) {
            visibleSignalData = new SignalData(visibleChannelCount, DEFAULT_FRAME_SIZE);
            averagedSignalData = new SignalData(visibleChannelCount, DEFAULT_AVERAGED_SAMPLE_BUFFER_SIZE);
        }
    }

    /**
     * Shows incoming signal's channel at specified {@code channelIndex}.
     */
    void showChannel(int channelIndex) {
        // update signal configuration
        signalConfiguration.setChannelVisible(channelIndex, true);

        final int visibleChannelCount = signalConfiguration.getVisibleChannelCount();

        // reset processing buffer
        processingBuffer.resetAveragedSamplesBuffer(visibleChannelCount);

        synchronized (lock) {
            // reset buffer
            visibleSignalData = new SignalData(visibleChannelCount, DEFAULT_FRAME_SIZE);
            averagedSignalData = new SignalData(visibleChannelCount, DEFAULT_AVERAGED_SAMPLE_BUFFER_SIZE);
        }
    }

    /**
     * Hides incoming signal's channel at specified {@code channelIndex}.
     */
    void hideChannel(int channelIndex) {
        // update signal configuration
        signalConfiguration.setChannelVisible(channelIndex, false);

        final int visibleChannelCount = signalConfiguration.getVisibleChannelCount();

        // reset processing buffer
        processingBuffer.resetAveragedSamplesBuffer(visibleChannelCount);

        synchronized (lock) {
            // reset buffers
            visibleSignalData = new SignalData(visibleChannelCount, DEFAULT_FRAME_SIZE);
            averagedSignalData = new SignalData(visibleChannelCount, DEFAULT_AVERAGED_SAMPLE_BUFFER_SIZE);
        }
    }

    /**
     * Sets the selected channel of incoming signal.
     */
    public void setSelectedChannel(int channelIndex) {
        // update signal configuration
        signalConfiguration.setSelectedChannel(channelIndex);

        // pass selected channel to native code
        JniUtils.setSelectedChannel(channelIndex);
    }

    /**
     * Sets whether incoming signal should be averaged.
     */
    void setSignalAveraging(boolean signalAveraging) {
        LOGD(TAG, "setSignalAveraging(" + signalAveraging + ")");

        // calculate max number of processed samples
        calculateMaxNumberOfProcessedSamples(signalConfiguration.getSampleRate(), signalAveraging);

        // update signal configuration
        signalConfiguration.setSignalAveraging(signalAveraging);

        // reset processing buffer
        processingBuffer.resetAveragedSamplesBuffer(signalConfiguration.getVisibleChannelCount());
    }

    /**
     * Sets type of triggering that will trigger signal averaging.
     */
    void setSignalAveragingTriggerType(@SignalAveragingTriggerType int triggerType) {
        // update signal configuration
        signalConfiguration.setSignalAveragingTriggerType(triggerType);

        // pass signal averaging trigger type to native code
        JniUtils.setAveragingTriggerType(triggerType);
    }

    /**
     * A data source that will provide data to data processor and notify it when different events occur.
     */
    void setSignalSource(@NonNull AbstractSignalSource signalSource) {
        signalSource.setProcessor(this);

        if (this.signalSource instanceof AbstractUsbSignalSource) {
            ((AbstractUsbSignalSource) this.signalSource).removeOnSpikerBoxHardwareTypeDetectionListener(
                spikerBoxDetectionListener);
            ((AbstractUsbSignalSource) this.signalSource).removeOnExpansionBoardTypeDetectionListener(
                expansionBoardDetectionListener);
        }

        this.signalSource = signalSource;

        processingBuffer.clearAllBuffers();

        // let's notify any interested party of the data source initial sample rate and channel count
        onSampleRateChanged(signalSource.getSampleRate());
        onChannelCountChanged(signalSource.getChannelCount());

        if (this.signalSource instanceof AbstractUsbSignalSource) {
            ((AbstractUsbSignalSource) this.signalSource).addOnSpikerBoxHardwareTypeDetectionListener(
                spikerBoxDetectionListener);
            ((AbstractUsbSignalSource) this.signalSource).addOnExpansionBoardTypeDetectionListener(
                expansionBoardDetectionListener);
        }
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
    }

    //private final Benchmark benchmark =
    //    new Benchmark("DATA_PROCESSING").warmUp(200).sessions(10).measuresPerSession(200).logBySession(false);

    @SuppressWarnings("WeakerAccess") void processData(@NonNull byte[] buffer, int length) {
        synchronized (lock) {
            // process incoming signal
            //benchmark.start();
            signalSource.processIncomingData(signalData, buffer, length);
            //benchmark.end();

            if (visibleSignalData.channelCount > 0) { // only configure channels if there is at least one visible
                final boolean signalAveraging = signalConfiguration.isSignalAveraging();
                // configure channels of processed signal
                signalData.copyReconfigured(visibleSignalData, signalConfiguration);
                // average processed signal
                JniUtils.processThreshold(averagedSignalData, visibleSignalData, signalAveraging);
                // TODO: 06-Mar-19 UNCOMMENT THIS WHEN FFT PROCESSING DEVELOPMENT CONTINUES
                //if (!signalAveraging) JniUtils.processFft(fft, signalData);

                // forward received samples to Processing Service
                if (listener != null) listener.onDataProcessed(visibleSignalData);
            }

            // add to buffer
            processingBuffer.add(signalData, averagedSignalData, fft);
        }
    }

    // Set max number of samples that can be processed in normal processing, in threshold and in fft
    private void calculateMaxNumberOfProcessedSamples(int sampleRate, boolean signalAveraging) {
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
