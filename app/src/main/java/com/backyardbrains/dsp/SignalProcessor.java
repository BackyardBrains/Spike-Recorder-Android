package com.backyardbrains.dsp;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.dsp.usb.AbstractUsbSignalSource;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.Benchmark;
import com.backyardbrains.utils.ExpansionBoardType;
import com.backyardbrains.utils.JniUtils;
import com.backyardbrains.utils.SignalAveragingTriggerType;
import com.backyardbrains.utils.SpikerBoxHardwareType;
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
    // Maximum time that should be processed when processing FFT
    private static final float MAX_FFT_PROCESSING_TIME = 6f;
    // We only look at approx. 30% of the fft data cause we only want to analyze low frequency
    private static final int FFT_30HZ_LENGTH = 32; // ~30%
    // Percent of overlap between to consecutive FFT windows
    public static final int FFT_WINDOW_OVERLAP_PERCENT = 99;
    // Length of a single FFT window in seconds
    public static final int FFT_WINDOW_TIME_LENGTH = 4; // 2^2
    // Sample rate that is used when processing FFT
    private static final int FFT_SAMPLE_RATE = 128; // 2^7
    // Number of samples in a single FFT window
    private static final int FFT_WINDOW_SAMPLE_COUNT = FFT_WINDOW_TIME_LENGTH * FFT_SAMPLE_RATE; // 2^9
    // Number of samples between two FFT windows
    private static final int FFT_WINDOW_SAMPLE_DIFF_COUNT =
        (int) (FFT_WINDOW_SAMPLE_COUNT * (1.0f - (FFT_WINDOW_OVERLAP_PERCENT / 100.0f)));
    // Number of FFT windows needed to render 6s of signal
    public static final int FFT_WINDOW_COUNT =
        (int) ((MAX_AUDIO_PROCESSING_TIME * FFT_SAMPLE_RATE) / FFT_WINDOW_SAMPLE_DIFF_COUNT);
    // Size of of a single FFT
    public static final int FFT_WINDOW_SIZE = FFT_30HZ_LENGTH * FFT_WINDOW_TIME_LENGTH; // 32 / (128 / 512)

    // Default sample rate
    public static final int DEFAULT_SAMPLE_RATE = AudioUtils.DEFAULT_SAMPLE_RATE;
    // Default channel count
    public static final int DEFAULT_CHANNEL_COUNT = AudioUtils.DEFAULT_CHANNEL_COUNT;

    // Max number of samples that can be processed at any given time during live signal processing
    public static final int DEFAULT_LIVE_MAX_PROCESSED_SAMPLES_COUNT =
        (int) (MAX_AUDIO_PROCESSING_TIME * DEFAULT_SAMPLE_RATE * DEFAULT_CHANNEL_COUNT);
    // Max number of samples that can be processed at any given time during playback (it's during seek)
    public static final int DEFAULT_PLAYBACK_MAX_PROCESSED_SAMPLES_COUNT =
        DEFAULT_LIVE_MAX_PROCESSED_SAMPLES_COUNT + 2 * (FFT_WINDOW_SAMPLE_COUNT - FFT_WINDOW_SAMPLE_DIFF_COUNT);
    // Max number of samples that can be processed at an given time during live signal averaging
    public static final int DEFAULT_MAX_PROCESSED_AVERAGED_SAMPLES_COUNT =
        (int) (MAX_THRESHOLD_PROCESSING_TIME * DEFAULT_SAMPLE_RATE);

    private static int processedSamplesCount = DEFAULT_LIVE_MAX_PROCESSED_SAMPLES_COUNT;
    private static int processedAveragedSamplesCount = DEFAULT_MAX_PROCESSED_AVERAGED_SAMPLES_COUNT;
    private static int drawnSamplesCount = DEFAULT_LIVE_MAX_PROCESSED_SAMPLES_COUNT;

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
    // Size is determined by the max number of samples that can be processed in one incoming batch which is during seek
    @SuppressWarnings("WeakerAccess") final CircularByteBuffer ringBuffer =
        new CircularByteBuffer(DEFAULT_PLAYBACK_MAX_PROCESSED_SAMPLES_COUNT * 2);
    // Used for holding raw data during processing
    @SuppressWarnings("WeakerAccess") final byte[] buffer = new byte[DEFAULT_PLAYBACK_MAX_PROCESSED_SAMPLES_COUNT * 2];

    // Holds signal data after processing raw signal together with processed events
    private SignalData signalData =
        new SignalData(AudioUtils.DEFAULT_CHANNEL_COUNT, DEFAULT_LIVE_MAX_PROCESSED_SAMPLES_COUNT);
    // Holds processed signal data of only visible channels
    private SignalData visibleSignalData =
        new SignalData(AudioUtils.DEFAULT_CHANNEL_COUNT, DEFAULT_LIVE_MAX_PROCESSED_SAMPLES_COUNT);
    // Holds processed signal data of only visible channels after averaging
    private SignalData averagedSignalData =
        new SignalData(AudioUtils.DEFAULT_CHANNEL_COUNT, DEFAULT_MAX_PROCESSED_AVERAGED_SAMPLES_COUNT);
    // Holds processed signal data after FFT processing
    private FftData fft = new FftData(FFT_WINDOW_COUNT, FFT_WINDOW_SIZE);

    // Reference to the singleton buffer for storing data after processing
    private final ProcessingBuffer processingBuffer;

    // Configuration of the incoming processed signal
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
            if (expansionBoardType == ExpansionBoardType.HAMMER || expansionBoardType == ExpansionBoardType.JOYSTICK) {
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
        synchronized (lock) {
            // calculate max number of processed and drawn samples
            calculateMaxNumberOfProcessedSamples(sampleRate);
            calculateMaxNumberOfDrawnSamples(sampleRate, signalConfiguration.isSignalAveraging(),
                signalConfiguration.isFftProcessing());

            // update signal configuration
            signalConfiguration.setSampleRate(sampleRate);

            // reset processing buffer
            processingBuffer.resetAllSampleBuffers(signalConfiguration.getChannelCount(),
                signalConfiguration.getVisibleChannelCount());
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
        synchronized (lock) {
            // update signal configuration
            signalConfiguration.setChannelCount(channelCount);

            // reset processing buffer
            processingBuffer.resetAllSampleBuffers(channelCount, signalConfiguration.getVisibleChannelCount());

            // reset buffers
            signalData = new SignalData(channelCount, processedSamplesCount);
            visibleSignalData = new SignalData(signalConfiguration.getVisibleChannelCount(), processedSamplesCount);
            averagedSignalData =
                new SignalData(signalConfiguration.getVisibleChannelCount(), processedAveragedSamplesCount);
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
     * Max number of samples that should be drawn by the renderer.
     */
    public static int getDrawnSamplesCount() {
        return drawnSamplesCount;
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
     * Returns number of visible channels of the current signal source.
     */
    int getVisibleChannelCount() {
        return signalConfiguration.getVisibleChannelCount();
    }

    /**
     * Returns whether channel at specified {@code channelIndex} is visible or not.
     */
    boolean isChannelVisible(int channelIndex) {
        return signalConfiguration.isChannelVisible(channelIndex);
    }

    /**
     * Returns currently selected channel.
     */
    int getSelectedChannel() {
        return signalConfiguration.getSelectedChannel();
    }

    /**
     * Sets what incoming signal's channels are shown/hidden.
     */
    private void setChannelConfig(boolean[] channelConfig) {
        if (channelConfig.length != getChannelCount()) return;

        synchronized (lock) {
            // update signal configuration
            signalConfiguration.setChannelConfig(channelConfig);

            final int visibleChannelCount = signalConfiguration.getVisibleChannelCount();

            // reset processing buffer
            processingBuffer.resetAveragedSamplesBuffer(visibleChannelCount);

            visibleSignalData = new SignalData(visibleChannelCount, processedSamplesCount);
            averagedSignalData = new SignalData(visibleChannelCount, processedAveragedSamplesCount);
        }
    }

    /**
     * Shows incoming signal's channel at specified {@code channelIndex}.
     */
    void showChannel(int channelIndex) {
        synchronized (lock) {
            // update signal configuration
            signalConfiguration.setChannelVisible(channelIndex, true);

            final int visibleChannelCount = signalConfiguration.getVisibleChannelCount();

            // reset processing buffer
            processingBuffer.resetAveragedSamplesBuffer(visibleChannelCount);

            // reset buffer
            visibleSignalData = new SignalData(visibleChannelCount, processedSamplesCount);
            averagedSignalData = new SignalData(visibleChannelCount, processedAveragedSamplesCount);
        }
    }

    /**
     * Hides incoming signal's channel at specified {@code channelIndex}.
     */
    void hideChannel(int channelIndex) {
        synchronized (lock) {
            // update signal configuration
            signalConfiguration.setChannelVisible(channelIndex, false);

            final int visibleChannelCount = signalConfiguration.getVisibleChannelCount();

            // reset processing buffer
            processingBuffer.resetAveragedSamplesBuffer(visibleChannelCount);

            // reset buffers
            visibleSignalData = new SignalData(visibleChannelCount, processedSamplesCount);
            averagedSignalData = new SignalData(visibleChannelCount, processedAveragedSamplesCount);
        }
    }

    /**
     * Sets the selected channel of incoming signal.
     */
    public void setSelectedChannel(int channelIndex) {
        synchronized (lock) {
            // update signal configuration
            signalConfiguration.setSelectedChannel(channelIndex);

            // pass selected channel to native code
            JniUtils.setSelectedChannel(channelIndex);
            // fft should be reset every time channel is switched
            if (signalConfiguration.isFftProcessing()) JniUtils.resetFft();
        }
    }

    /**
     * Sets whether incoming signal should be averaged.
     */
    void setSignalAveraging(boolean signalAveraging) {
        LOGD(TAG, "setSignalAveraging(" + signalAveraging + ")");

        synchronized (lock) {
            // calculate max number of drawn samples
            calculateMaxNumberOfDrawnSamples(signalConfiguration.getSampleRate(), signalAveraging,
                signalConfiguration.isFftProcessing());

            // update signal configuration
            signalConfiguration.setSignalAveraging(signalAveraging);

            // reset processing buffer
            processingBuffer.resetAveragedSamplesBuffer(signalConfiguration.getVisibleChannelCount());
        }
    }

    /**
     * Sets type of triggering that will trigger signal averaging.
     */
    void setSignalAveragingTriggerType(@SignalAveragingTriggerType int triggerType) {
        LOGD(TAG, "setSignalAveragingTriggerType(" + triggerType + ")");

        synchronized (lock) {
            // update signal configuration
            signalConfiguration.setSignalAveragingTriggerType(triggerType);

            // pass signal averaging trigger type to native code
            JniUtils.setAveragingTriggerType(triggerType);
        }
    }

    /**
     * Sets whether incoming signal should be processed with FFT.
     */
    void setFftProcessing(boolean fftProcessing) {
        LOGD(TAG, "fftProcessing(" + fftProcessing + ")");

        synchronized (lock) {
            // calculate max number of drawn samples
            calculateMaxNumberOfDrawnSamples(signalConfiguration.getSampleRate(),
                signalConfiguration.isSignalAveraging(), fftProcessing);

            // update signal configuration
            signalConfiguration.setFftProcessing(fftProcessing);
        }
    }

    /**
     * Sets whether incoming signal is being sought or not.
     */
    void setSignalSeeking(boolean signalSeeking) {
        LOGD(TAG, "setSignalSeeking(" + signalSeeking + ")");

        synchronized (lock) {
            // update signal configuration
            signalConfiguration.setSignalSeeking(signalSeeking);
        }
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

    private final Benchmark benchmark =
        new Benchmark("FFT_PROCESSING").warmUp(200).sessions(10).measuresPerSession(200).logBySession(false);

    @SuppressWarnings("WeakerAccess") void processData(@NonNull byte[] buffer, int length) {
        synchronized (lock) {
            // process incoming signal
            //benchmark.start();
            signalSource.processIncomingData(signalData, buffer, length);
            //benchmark.end();

            if (visibleSignalData.channelCount > 0) { // only configure channels if there is at least one visible
                final boolean signalAveraging = signalConfiguration.isSignalAveraging();
                final boolean fftProcessing = signalConfiguration.isFftProcessing();
                final boolean signalSeeking = signalConfiguration.isSignalSeeking();
                // configure channels of processed signal
                signalData.copyReconfigured(visibleSignalData, signalConfiguration);
                // average processed signal
                JniUtils.processThreshold(averagedSignalData, visibleSignalData, signalAveraging);
                if (!signalAveraging/* && fftProcessing*/) {
                    //benchmark.start();
                    JniUtils.processFft(fft, visibleSignalData, signalSeeking);
                    //benchmark.end();
                }

                // forward received samples to Processing Service
                if (listener != null) listener.onDataProcessed(visibleSignalData);
            }

            // add to buffer
            processingBuffer.add(signalData, averagedSignalData, fft);
        }
    }

    // Set max number of samples that can be processed in normal processing, in threshold and in fft
    private void calculateMaxNumberOfProcessedSamples(int sampleRate) {
        processedSamplesCount = (int) (MAX_AUDIO_PROCESSING_TIME * sampleRate);
        processedAveragedSamplesCount = (int) (MAX_THRESHOLD_PROCESSING_TIME * sampleRate);
        if (signalSource != null) {
            if (signalSource.isUsb()) {
                processedSamplesCount = (int) (MAX_SAMPLE_STREAM_PROCESSING_TIME * sampleRate);
            } else if (signalSource.isFile()) {
                int windowSampleCount = FFT_WINDOW_TIME_LENGTH * sampleRate;
                int windowSampleDiffCount = (int) (windowSampleCount * (1.0f - (FFT_WINDOW_OVERLAP_PERCENT / 100.0f)));
                processedSamplesCount =
                    FFT_WINDOW_COUNT * windowSampleDiffCount + windowSampleCount - windowSampleDiffCount;
                processedAveragedSamplesCount = (int) (MAX_THRESHOLD_PROCESSING_TIME * sampleRate);
            }
        }
    }

    private void calculateMaxNumberOfDrawnSamples(int sampleRate, boolean signalAveraging, boolean fftProcessing) {
        final float time;
        if (signalAveraging) {
            time = MAX_THRESHOLD_PROCESSING_TIME;
        } else if (fftProcessing) {
            time = MAX_FFT_PROCESSING_TIME;
        } else {
            time = signalSource != null && signalSource.isUsb() ? MAX_SAMPLE_STREAM_PROCESSING_TIME
                : MAX_AUDIO_PROCESSING_TIME;
        }
        // this is queried by renderer to know how wide drawing window should be
        drawnSamplesCount = (int) (time * sampleRate);
    }
}
