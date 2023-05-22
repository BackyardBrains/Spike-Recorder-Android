package com.backyardbrains.dsp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.backyardbrains.dsp.audio.PlaybackSignalSource;
import com.backyardbrains.dsp.usb.AbstractUsbSignalSource;
import com.backyardbrains.utils.AudioUtils;
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

    @SuppressWarnings("unused")
    private static final String TAG = makeLogTag(SignalProcessor.class);

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
    private static final int FFT_WINDOW_OVERLAP_PERCENT = 99;
    // Length of a single FFT window in seconds
    private static final float FFT_WINDOW_TIME_LENGTH = 4f; // 2^2
    // Sample rate that is used when processing FFT
    private static final int FFT_SAMPLE_RATE = 128; // 2^7
    // Number of samples in a single FFT window
    private static final int FFT_WINDOW_SAMPLE_COUNT = (int) (FFT_WINDOW_TIME_LENGTH * FFT_SAMPLE_RATE); // 2^9
    // Number of samples between two FFT windows
    private static final int FFT_WINDOW_SAMPLE_DIFF_COUNT =
            (int) (FFT_WINDOW_SAMPLE_COUNT * (1.0f - (FFT_WINDOW_OVERLAP_PERCENT / 100.0f)));
    // Number of FFT windows needed to render 6s of signal
    public static final int FFT_WINDOW_COUNT =
            (int) ((MAX_AUDIO_PROCESSING_TIME * FFT_SAMPLE_RATE) / FFT_WINDOW_SAMPLE_DIFF_COUNT);
    // Size of of a single FFT
    public static final int FFT_WINDOW_SIZE = (int) (FFT_30HZ_LENGTH * FFT_WINDOW_TIME_LENGTH); // 32 / (128 / 512)

    // Default sample rate
    public static final int DEFAULT_SAMPLE_RATE = AudioUtils.DEFAULT_SAMPLE_RATE;
    // Default channel count
    public static final int DEFAULT_CHANNEL_COUNT = AudioUtils.DEFAULT_CHANNEL_COUNT;

    // Number of samples per channel that is being processed when the app starts
    // 6s * 44100Hz per channel
    public static final int DEFAULT_PROCESSED_SAMPLES_PER_CHANNEL_COUNT =
            (int) (MAX_AUDIO_PROCESSING_TIME * DEFAULT_SAMPLE_RATE);
    // Number of samples per channel that is being processed during signal averaging when the app starts
    public static final int DEFAULT_PROCESSED_AVERAGED_SAMPLES_PER_CHANNEL_COUNT =
            (int) (MAX_THRESHOLD_PROCESSING_TIME * DEFAULT_SAMPLE_RATE);

    // Max number of samples in a single FFT window achievable
    private static final int MAX_FFT_WINDOW_SAMPLE_COUNT = (int) (FFT_WINDOW_TIME_LENGTH * DEFAULT_SAMPLE_RATE);
    // Max number of samples between two FFT windows achievable
    private static final int MAX_FFT_WINDOW_SAMPLE_DIFF_COUNT =
            (int) (MAX_FFT_WINDOW_SAMPLE_COUNT * (1.0f - (FFT_WINDOW_OVERLAP_PERCENT / 100.0f)));
    // Max number of samples that can be processed at any given moment
    // 153 FFT windows of 4s * 44100Hz * 8 channels
    public static final int MAX_PROCESSED_SAMPLES_COUNT =
            (FFT_WINDOW_COUNT * MAX_FFT_WINDOW_SAMPLE_DIFF_COUNT + MAX_FFT_WINDOW_SAMPLE_COUNT
                    - MAX_FFT_WINDOW_SAMPLE_DIFF_COUNT) * 8;

    private static int processedSamplesPerChannelCount = DEFAULT_PROCESSED_SAMPLES_PER_CHANNEL_COUNT;
    private static int processedAveragedSamplesPerChannelCount = DEFAULT_PROCESSED_AVERAGED_SAMPLES_PER_CHANNEL_COUNT;
    private static int drawnSamplesCount = DEFAULT_PROCESSED_SAMPLES_PER_CHANNEL_COUNT;

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

        @Override
        public void run() {
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
    @SuppressWarnings("WeakerAccess")
    AtomicBoolean working = new AtomicBoolean();
    // Whether processing is temporarily paused
    @SuppressWarnings("WeakerAccess")
    AtomicBoolean paused = new AtomicBoolean();
    // Ring buffer that holds raw signal data
    @SuppressWarnings("WeakerAccess")
    final CircularByteBuffer ringBuffer =
            new CircularByteBuffer(MAX_PROCESSED_SAMPLES_COUNT * 2);
    // Holds raw data during processing
    @SuppressWarnings("WeakerAccess")
    final byte[] buffer = new byte[MAX_PROCESSED_SAMPLES_COUNT * 2];
    // Holds data retrieved from the playback signal source when switching between channels (complete screen render)
    private final byte[] playbackBuffer = new byte[MAX_PROCESSED_SAMPLES_COUNT * 2];

    // Holds signal data after processing raw signal together with processed events
    private SignalData signalData =
            new SignalData(AudioUtils.DEFAULT_CHANNEL_COUNT, DEFAULT_PROCESSED_SAMPLES_PER_CHANNEL_COUNT,
                    AudioUtils.DEFAULT_BITS_PER_SAMPLE);
    // Holds processed signal data of only visible channels
    private SignalData visibleSignalData =
            new SignalData(AudioUtils.DEFAULT_CHANNEL_COUNT, DEFAULT_PROCESSED_SAMPLES_PER_CHANNEL_COUNT,
                    AudioUtils.DEFAULT_BITS_PER_SAMPLE);
    // Holds processed signal data of only visible channels after averaging
    private SignalData averagedSignalData =
            new SignalData(AudioUtils.DEFAULT_CHANNEL_COUNT, DEFAULT_PROCESSED_AVERAGED_SAMPLES_PER_CHANNEL_COUNT,
                    AudioUtils.DEFAULT_BITS_PER_SAMPLE);
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
                    setChannelConfig(new boolean[]{true, false});
                }
            };

    private final AbstractUsbSignalSource.OnExpansionBoardTypeDetectionListener expansionBoardDetectionListener =
            expansionBoardType -> {
                if (expansionBoardType == ExpansionBoardType.HAMMER || expansionBoardType == ExpansionBoardType.JOYSTICK) {
                    setChannelConfig(new boolean[]{true, false, true});
                } else if (expansionBoardType == ExpansionBoardType.NONE) {
                    setChannelConfig(new boolean[]{true, false});
                }
            };
    private final AbstractUsbSignalSource.onHumanSpikerP300AudioStateListener onSpikerP300AudioStateListener =
            humanSpikerAudioState -> {

            };
    private final AbstractUsbSignalSource.onHumanSpikerP300StateListener onHumanSpikerP300StateListener =
            humanSpikerBoardState -> {
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
     * @param data   The buffer that contains received data.
     * @param length The length of the received data.
     */
    @Override
    public void onDataReceived(@NonNull byte[] data, int length) {
        ringBuffer.put(data, 0, length);
    }

    /**
     * {@inheritDoc}
     *
     * @param sampleRate New sample rate.
     */
    @Override
    public void onSampleRateChanged(int sampleRate) {
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
    @Override
    public void onChannelCountChanged(int channelCount) {
        synchronized (lock) {
            // update signal configuration
            signalConfiguration.setChannelCount(channelCount);

            final int visibleChannelCount = signalConfiguration.getVisibleChannelCount();
            final int bitsPerSample = signalConfiguration.getBitsPerSample();

            // reset processing buffer
            processingBuffer.resetAllSampleBuffers(channelCount, visibleChannelCount);

            // reset buffers
            signalData = new SignalData(channelCount, processedSamplesPerChannelCount, bitsPerSample);
            visibleSignalData = new SignalData(visibleChannelCount, processedSamplesPerChannelCount, bitsPerSample);
            averagedSignalData =
                    new SignalData(visibleChannelCount, processedAveragedSamplesPerChannelCount, bitsPerSample);
        }

        // pass channel count to native code
        JniUtils.setChannelCount(channelCount);
    }

    /**
     * {@inheritDoc}
     *
     * @param bitsPerSample New number of bits per sample.
     */
    @Override
    public void onBitsPerSampleChanged(int bitsPerSample) {
        // update signal configuration
        signalConfiguration.setBitsPerSample(bitsPerSample);

        final int visibleChannelCount = signalConfiguration.getVisibleChannelCount();

        // reset buffers
        signalData =
                new SignalData(signalConfiguration.getChannelCount(), processedSamplesPerChannelCount, bitsPerSample);
        visibleSignalData = new SignalData(visibleChannelCount, processedSamplesPerChannelCount, bitsPerSample);
        averagedSignalData =
                new SignalData(visibleChannelCount, processedAveragedSamplesPerChannelCount, bitsPerSample);

        // pass bits per sample to native code
        JniUtils.setBitsPerSample(bitsPerSample);
    }

    /**
     * Max number of samples that can be processed by the current signal source.
     */
    public static int getProcessedSamplesPerChannelCount() {
        return processedSamplesPerChannelCount;
    }

    /**
     * Max number of averaged samples that can be processed by the current signal source.
     */
    public static int getProcessedAveragedSamplesPerChannelCount() {
        return processedAveragedSamplesPerChannelCount;
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
     * Returns number of bits per sample of the current signal source.
     */
    int getBitsPerSample() {
        return signalConfiguration.getBitsPerSample();
    }

    /**
     * Returns whether channel at specified {@code channelIndex} is visible or not.
     */
    boolean isChannelVisible(int channelIndex) {
        return signalConfiguration.isChannelVisible(channelIndex);
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
            final int bitsPerSample = signalConfiguration.getBitsPerSample();

            // reset processing buffer
            processingBuffer.resetAveragedSamplesBuffer(visibleChannelCount);

            visibleSignalData = new SignalData(visibleChannelCount, processedSamplesPerChannelCount, bitsPerSample);
            averagedSignalData =
                    new SignalData(visibleChannelCount, processedAveragedSamplesPerChannelCount, bitsPerSample);
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
            final int bitsPerSample = signalConfiguration.getBitsPerSample();

            // reset processing buffer
            processingBuffer.resetAveragedSamplesBuffer(visibleChannelCount);

            // reset buffer
            visibleSignalData = new SignalData(visibleChannelCount, processedSamplesPerChannelCount, bitsPerSample);
            averagedSignalData =
                    new SignalData(visibleChannelCount, processedAveragedSamplesPerChannelCount, bitsPerSample);
        }
    }

    /**
     * Hides incoming signal's channel at specified {@code channelIndex}.
     */
    void hideChannel(int channelIndex) {
        synchronized (lock) {
            // if we are hiding currently selected channel we should switch to default (0) channel
            if (channelIndex == signalConfiguration.getSelectedChannel()) setSelectedChannel(0);

            // update signal configuration
            signalConfiguration.setChannelVisible(channelIndex, false);

            final int visibleChannelCount = signalConfiguration.getVisibleChannelCount();
            final int bitsPerSample = signalConfiguration.getBitsPerSample();

            // reset processing buffer
            processingBuffer.resetAveragedSamplesBuffer(visibleChannelCount);

            // reset buffers
            visibleSignalData = new SignalData(visibleChannelCount, processedSamplesPerChannelCount, bitsPerSample);
            averagedSignalData =
                    new SignalData(visibleChannelCount, processedAveragedSamplesPerChannelCount, bitsPerSample);
        }
    }

    /**
     * Returns currently selected channel.
     */
    int getSelectedChannel() {
        return signalConfiguration.getSelectedChannel();
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

            if (signalConfiguration.isFftProcessing() && signalSource.isFile()) {
                final PlaybackSignalSource pss = (PlaybackSignalSource) signalSource;
                if (pss != null) {
                    // we need enough bytes to render full screen
                    int len = processedSamplesPerChannelCount * signalConfiguration.getChannelCount() * 2;
                    pss.readLast(playbackBuffer, len);
                    processData(playbackBuffer, len);
                }
            }
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
        synchronized (lock) {
            // update signal configuration
            signalConfiguration.setSignalSeeking(signalSeeking);
        }
    }

    /**
     * Sets current BYB board type.
     */
    void setBoardType(@SpikerBoxHardwareType int boardType) {
        synchronized (lock) {
            // update signal configuration
            signalConfiguration.setBoardType(boardType);
        }
    }

    /**
     * Sets current expansion board type.
     */
    void setExpansionBoardType(@ExpansionBoardType int expansionBoardType) {
        synchronized (lock) {
            // update signal configuration
            signalConfiguration.setExpansionBoardType(expansionBoardType);
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
            ((AbstractUsbSignalSource) this.signalSource).removeOnHumanSpikerP300AudioStateListener(
                    onSpikerP300AudioStateListener);
            ((AbstractUsbSignalSource) this.signalSource).removeOnHumanSpikerBoxp300State(
                    onHumanSpikerP300StateListener);
        }

        this.signalSource = signalSource;

        processingBuffer.clearAllBuffers();

        // let's notify any interested party of the data source initial sample rate, channel count and bits/sample
        onSampleRateChanged(signalSource.getSampleRate());
        onChannelCountChanged(signalSource.getChannelCount());
        onBitsPerSampleChanged(signalSource.getBitsPerSample());
        // reset to first channel
        setSelectedChannel(0);

        if (this.signalSource instanceof AbstractUsbSignalSource) {
            ((AbstractUsbSignalSource) this.signalSource).addOnSpikerBoxHardwareTypeDetectionListener(
                    spikerBoxDetectionListener);
            ((AbstractUsbSignalSource) this.signalSource).addOnExpansionBoardTypeDetectionListener(
                    expansionBoardDetectionListener);
            ((AbstractUsbSignalSource) this.signalSource).addOnHumanSpikerBoxp300State(
                    onHumanSpikerP300StateListener);
            ((AbstractUsbSignalSource) this.signalSource).addOnHumanSpikerP300AudioStateListener(
                    onSpikerP300AudioStateListener);
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
    //    new Benchmark("FFT_PROCESSING").warmUp(200).sessions(10).measuresPerSession(200).logBySession(false);

    @SuppressWarnings("WeakerAccess")
    void processData(@NonNull byte[] buffer, int length) {
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
                //benchmark.start();
                if (!signalAveraging) JniUtils.processFft(fft, visibleSignalData);
                //benchmark.end();

                // forward received samples to Processing Service
                if (listener != null) listener.onDataProcessed(visibleSignalData);
            }

            // add to buffer
            processingBuffer.add(signalData, averagedSignalData, fft);
        }
    }

    // Set max number of samples that can bEe processed in normal processing, in threshold and in fft
    private void calculateMaxNumberOfProcessedSamples(int sampleRate) {
        processedSamplesPerChannelCount = (int) (MAX_AUDIO_PROCESSING_TIME * sampleRate);
        processedAveragedSamplesPerChannelCount = (int) (MAX_THRESHOLD_PROCESSING_TIME * sampleRate);
        if (signalSource != null) {
            if (signalSource.isUsb()) {
                processedSamplesPerChannelCount = (int) (MAX_SAMPLE_STREAM_PROCESSING_TIME * sampleRate);
            } else if (signalSource.isFile()) {
                int windowSampleCount = (int) (FFT_WINDOW_TIME_LENGTH * sampleRate);
                int windowSampleDiffCount = (int) (windowSampleCount * (1.0f - (FFT_WINDOW_OVERLAP_PERCENT / 100.0f)));
                processedSamplesPerChannelCount =
                        FFT_WINDOW_COUNT * windowSampleDiffCount + windowSampleCount - windowSampleDiffCount;
                processedAveragedSamplesPerChannelCount = (int) (MAX_THRESHOLD_PROCESSING_TIME * sampleRate);
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
