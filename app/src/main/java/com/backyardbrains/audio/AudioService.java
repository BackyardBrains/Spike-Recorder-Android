/*
 * Backyard Brains Android App
 * Copyright (C) 2011 Backyard Brains
 * by Nathan Dotz <nate (at) backyardbrains.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.backyardbrains.audio;

import android.app.Service;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.data.processing.AbstractSampleSource;
import com.backyardbrains.data.processing.ProcessingBuffer;
import com.backyardbrains.data.processing.SampleProcessor;
import com.backyardbrains.events.AmModulationDetectionEvent;
import com.backyardbrains.events.AudioPlaybackProgressEvent;
import com.backyardbrains.events.AudioPlaybackStartedEvent;
import com.backyardbrains.events.AudioPlaybackStoppedEvent;
import com.backyardbrains.events.AudioRecordingProgressEvent;
import com.backyardbrains.events.AudioRecordingStartedEvent;
import com.backyardbrains.events.AudioRecordingStoppedEvent;
import com.backyardbrains.events.SampleRateChangeEvent;
import com.backyardbrains.events.SpikerBoxHardwareTypeDetectionEvent;
import com.backyardbrains.events.UsbCommunicationEvent;
import com.backyardbrains.events.UsbDeviceConnectionEvent;
import com.backyardbrains.events.UsbPermissionEvent;
import com.backyardbrains.filters.Filter;
import com.backyardbrains.usb.AbstractUsbSampleSource;
import com.backyardbrains.usb.SamplesWithMarkers;
import com.backyardbrains.usb.UsbHelper;
import com.backyardbrains.utils.ApacheCommonsLang3Utils;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.SampleStreamUtils;
import com.backyardbrains.utils.SpikerBoxHardwareType;
import com.backyardbrains.utils.ViewUtils;
import com.crashlytics.android.Crashlytics;
import java.io.IOException;
import java.lang.ref.WeakReference;
import org.greenrobot.eventbus.EventBus;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.LOGW;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * Manages a thread which monitors default audio input and pushes raw audio data to bound activities.
 *
 * @author Nathan Dotz <nate@backyardbrains.com>
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 * @version 1
 */
public class AudioService extends Service implements ReceivesAudio, AbstractSampleSource.OnSamplesReceivedListener {

    static final String TAG = makeLogTag(AudioService.class);

    private enum InputSourceType {
        NONE, MICROPHONE, USB
    }

    private static final Filters FILTERS = new Filters();

    private static final AmModulationProcessor.AmModulationDetectionListener AM_MODULATION_DETECTION_LISTENER =
        new AmModulationProcessor.AmModulationDetectionListener() {
            @Override public void onAmModulationStart() {
                EventBus.getDefault().post(new AmModulationDetectionEvent(true));
            }

            @Override public void onAmModulationEnd() {
                EventBus.getDefault().post(new AmModulationDetectionEvent(false));
            }
        };
    private final AmModulationProcessor AM_MODULATION_DATA_PROCESSOR =
        new AmModulationProcessor(AM_MODULATION_DETECTION_LISTENER, FILTERS);

    private final IBinder binder = new ServiceBinder();

    // Reference to the data manager that stores and processes the data
    ProcessingBuffer processingBuffer;
    // Reference to the sample processor that will additionally process the samples
    private WeakReference<SampleProcessor> sampleProcessorRef;

    // Reference to the microphone data source
    private MicListener micThread;
    // Reference to the USB serial data source
    private UsbHelper usbHelper;
    // Reference to the audio recorder
    private RecordingSaver recordingSaver;

    // Whether service is created
    private boolean created;
    // Current sample rate
    private int sampleRate;
    // Maximum number of seconds data manager should hold at any time
    private double maxTime;
    // Current input source
    private InputSourceType source = InputSourceType.NONE;

    // Reference to currently active USB sample source
    private AbstractUsbSampleSource usbSampleSource;
    // Reference to playback sample source
    private PlaybackSampleSource playbackSampleSource;

    /**
     * Provides a reference to {@link AudioService} to all bound clients.
     */
    public class ServiceBinder extends Binder {
        public AudioService getService() {
            return AudioService.this;
        }
    }

    //=================================================
    //  LIFECYCLE OVERRIDES
    //=================================================

    @Override public void onCreate() {
        super.onCreate();
        LOGD(TAG, "onCreate()");

        processingBuffer = ProcessingBuffer.get();
        // we need to listen for USB attach/detach
        startUsbDetection();

        // set current sample rate
        setSampleRate(AudioUtils.SAMPLE_RATE);

        created = true;
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override public void onDestroy() {
        created = false;

        LOGD(TAG, "onDestroy()");
        stopUsbDetection();
        turnOffMicThread();
        turnOffPlayback();

        processingBuffer = null;

        super.onDestroy();
    }

    //=================================================
    //  BIND
    //=================================================

    /**
     * return a binding pointer for GL threads to reference this object
     *
     * @return binding reference to this object
     * @see Service#onBind(Intent)
     */
    @Override public IBinder onBind(Intent arg0) {
        return binder;
    }

    @Override public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    //=================================================
    //  DATA PROCESSING
    //=================================================

    // Returns the activity reference and if reference is lost, logs the calling method.
    @Nullable @SuppressWarnings("WeakerAccess") SampleProcessor getProcessor() {
        // case if data processor is not set at all
        if (sampleProcessorRef == null) return null;

        return sampleProcessorRef.get();
    }

    /**
     * Sets the sample processor that will be used to additionally process incoming samples.
     */
    public void setSampleProcessor(@NonNull SampleProcessor processor) {
        LOGD(TAG, "setDataProcessor() - " + processor.getClass().getName());
        sampleProcessorRef = new WeakReference<>(processor);
    }

    /**
     * Clears sample processor.
     */
    public void clearSampleProcessor() {
        LOGD(TAG, "clearSampleProcessor()");
        sampleProcessorRef = null;
    }

    /**
     * Sets the maximum time of incoming data to be processed at any given moment in seconds.
     */
    public void setMaxProcessingTimeInSeconds(double maxSeconds) {
        LOGD(TAG, "setMaxProcessingTimeInSeconds(" + maxSeconds + ")");
        if (maxSeconds <= 0) return; // max time needs to be positive

        if (processingBuffer != null) processingBuffer.setBufferSize((int) (maxSeconds * sampleRate));

        this.maxTime = maxSeconds;
    }

    /**
     * Returns current sample rate.
     */
    public int getSampleRate() {
        return sampleRate;
    }

    // Set's current sample rate
    void setSampleRate(int sampleRate) {
        LOGD(TAG, "setSampleRate(" + sampleRate + ")");
        if (sampleRate <= 0) return; // sample rate needs to be positive

        this.sampleRate = sampleRate;

        // recalculate max render time
        setMaxProcessingTimeInSeconds(maxTime);
        // reset filters
        FILTERS.setSampleRate(sampleRate);

        // inform all interested parties that sample rate has changed
        EventBus.getDefault().post(new SampleRateChangeEvent(sampleRate));
    }

    //=================================================
    //  FILTERS
    //=================================================

    /**
     * Returns filter that is additionally applied when processing incoming data.
     */
    public Filter getFilter() {
        if (usbSampleSource != null) return usbSampleSource.getFilter();
        return FILTERS.getFilter();
    }

    /**
     * Sets predefined filters to be applied when processing incoming data.
     */
    public void setFilter(@Nullable Filter filter) {
        if (usbSampleSource != null) usbSampleSource.setFilter(filter);
        FILTERS.setFilter(filter);
    }

    //=================================================
    //  IMPLEMENTATIONS OF OnSamplesReceivedListener INTERFACE
    //=================================================

    /**
     * Adds received samples and events to the ring buffer. If we're recording, it also passes it to the recording
     * saver.
     *
     * @see AbstractSampleSource.OnSamplesReceivedListener#onSamplesReceived(SamplesWithMarkers)
     */
    @Override public void onSamplesReceived(@NonNull SamplesWithMarkers samplesWithMarkers) {
        passToDataManager(samplesWithMarkers);
    }

    //=================================================
    //  IMPLEMENTATIONS OF ReceivesAudio INTERFACE
    //=================================================

    private static final SamplesWithMarkers TEMP_SAMPLES_WITH_MARKERS =
        new SamplesWithMarkers(new int[0], new String[0]);

    /**
     * Adds received audio to the ring buffer. If we're recording, it also passes it to the recording saver.
     *
     * @see ReceivesAudio#receiveAudio(short[])
     */
    @Override public void receiveAudio(@NonNull short[] data) {
        // any received audio needs to be process with AM Modulation processor
        TEMP_SAMPLES_WITH_MARKERS.samples = AM_MODULATION_DATA_PROCESSOR.process(data);
        passToDataManager(TEMP_SAMPLES_WITH_MARKERS);
    }

    // Passes data to data manager so it can be consumed by renderer
    private void passToDataManager(@NonNull SamplesWithMarkers samplesWithMarkers) {
        // data -> ProcessingBuffer up to 2 secs
        if (processingBuffer != null) {
            if (getProcessor() != null) {
                // additionally process data if processor is provided before passing it to data manager
                TEMP_SAMPLES_WITH_MARKERS.samples = getProcessor().process(samplesWithMarkers.samples);
                processingBuffer.addToBuffer(TEMP_SAMPLES_WITH_MARKERS);
            } else {
                // pass data to data manager
                processingBuffer.addToBuffer(samplesWithMarkers);
            }
        }

        // pass data to RecordingSaver
        passToRecorder(samplesWithMarkers);
    }

    // Passes data to audio recorder
    private void passToRecorder(@NonNull SamplesWithMarkers samplesWithMarkers) {
        if (recordingSaver != null) record(samplesWithMarkers);
    }

    //=================================================
    //  CURRENT INPUT SOURCE
    //=================================================

    /**
     * Starts processing active input. If there is no active input Microphone is set as one.
     */
    public void startActiveInputSource() {
        if (created) {
            switch (source) {
                case NONE:
                case MICROPHONE:
                    turnOnMicThread();
                    break;
                case USB:
                    turnOnUsb();
                    break;
            }
        }
    }

    /**
     * Stops processing active input.
     */
    public void stopActiveInputSource() {
        if (created) {
            turnOffMicThread();
            turnOffUsb();
        }
    }

    /**
     * Whether USB is active input source.
     */
    public boolean isUsbActiveInput() {
        return source == InputSourceType.USB;
    }

    //=================================================
    //  MICROPHONE
    //=================================================

    /**
     * Starts processing Microphone input. (Default)
     */
    public void startMicrophone() {
        if (created) turnOnMicThread();
    }

    /**
     * Stops processing Microphone input (Default).
     */
    public void stopMicrophone() {
        if (created) turnOffMicThread();
    }

    private void turnOnMicThread() {
        LOGD(TAG, "turnOnMicThread()");
        turnOffUsb();
        turnOffPlayback();

        if (micThread == null) {
            source = InputSourceType.MICROPHONE;

            // set sample rate for microphone input
            setSampleRate(AudioUtils.SAMPLE_RATE);

            micThread = null;
            micThread = new MicListener(this);
            // we should clear buffer
            if (processingBuffer != null) processingBuffer.clearBuffer();

            micThread.start();
            LOGD(TAG, "Microphone thread started");
        }
    }

    private void turnOffMicThread() {
        LOGD(TAG, "turnOffMicThread()");
        stopRecording();
        if (micThread != null) {
            micThread.requestStop();
            micThread = null;
            LOGD(TAG, "Microphone Thread stopped");

            // we should clear buffer so that next buffer user doesn't have any residue
            if (processingBuffer != null) processingBuffer.clearBuffer();
        }
    }

    //=================================================
    //  AM MODULATION
    //=================================================

    /**
     * Whether AM modulation is currently detected.
     */
    public boolean isAmModulationDetected() {
        return AM_MODULATION_DATA_PROCESSOR.isAmModulationDetected();
    }

    //=================================================
    //  USB
    //=================================================

    /**
     * Initiates communication with USB device with the specified {@code deviceName}.
     */
    public void startUsb(@NonNull String deviceName) throws IllegalArgumentException {
        if (created) usbHelper.requestPermission(getApplicationContext(), deviceName, false);
    }

    /**
     * Closes the communication with the currently connected USB device.
     */
    public void stopUsb() {
        if (created) usbHelper.close();
    }

    /**
     * Returns {@code true} if currently active input is USB input and it's type is equal to the specified {@code
     * hardwareType}, {@code false} otherwise.
     */
    public boolean isActiveUsbInputOfType(@SpikerBoxHardwareType int hardwareType) {
        return created && usbSampleSource != null && usbSampleSource.isUsb()
            && usbSampleSource.getHardwareType() == hardwareType;
    }

    /**
     * Returns number of connected serial devices.
     */
    public int getDeviceCount() {
        return created ? usbHelper.getDevicesCount() : 0;
    }

    /**
     * Temporary method that returns USB device under specified {@code index}.
     */
    public UsbDevice getDevice(int index) {
        return usbHelper.getDevice(index);
    }

    //public boolean isCurrent

    // Turns on USB input processing
    void turnOnUsb() {
        LOGD(TAG, "turnOnUsb()");
        turnOffMicThread();
        turnOffPlayback();

        source = InputSourceType.USB;

        // set current USB input source
        usbSampleSource = usbHelper.getUsbDevice();
        if (usbSampleSource != null) {
            if (usbSampleSource.getHardwareType() != SpikerBoxHardwareType.UNKNOWN) {
                EventBus.getDefault().post(new SpikerBoxHardwareTypeDetectionEvent(usbSampleSource.getHardwareType()));
            } else {
                usbSampleSource.setOnSpikerBoxHardwareTypeDetectionListener(
                    new AbstractUsbSampleSource.OnSpikerBoxHardwareTypeDetectionListener() {
                        @Override public void onHardwareTypeDetected(int hardwareType) {
                            LOGD(TAG, "HARDWARE TYPE DETECTED: " + hardwareType);
                            EventBus.getDefault().post(new SpikerBoxHardwareTypeDetectionEvent(hardwareType));
                        }
                    });
            }
        }

        // set sample rate for USB serial input
        setSampleRate(SampleStreamUtils.SAMPLE_RATE);

        // resume communication with USB
        usbHelper.resume();
        LOGD(TAG, "USB communication started");

        // we should clear buffer
        if (processingBuffer != null) processingBuffer.clearBuffer();
    }

    // Turns off USB input processing
    void turnOffUsb() {
        LOGD(TAG, "turnOffUsb()");
        stopRecording();

        // remove current USB input source
        usbSampleSource = null;

        // pause communication with USB
        usbHelper.pause();

        // we should clear buffer so that next buffer user doesn't have any residue
        if (processingBuffer != null) processingBuffer.clearBuffer();
        LOGD(TAG, "USB communication ended");
    }

    // Starts listening attaching/detaching of USB devices
    private void startUsbDetection() {
        LOGD(TAG, "startUsbDetection()");
        if (usbHelper == null) {
            usbHelper = new UsbHelper(getApplicationContext(), this, new UsbHelper.UsbListener() {
                @Override
                public void onDeviceAttached(@NonNull String deviceName, @SpikerBoxHardwareType int hardwareType) {
                    EventBus.getDefault().post(new UsbDeviceConnectionEvent(true));
                }

                @Override public void onDeviceDetached(@NonNull String deviceName) {
                    EventBus.getDefault().post(new UsbDeviceConnectionEvent(false));
                }

                @Override public void onPermissionGranted() {
                    EventBus.getDefault().post(new UsbPermissionEvent(true));
                }

                @Override public void onPermissionDenied() {
                    EventBus.getDefault().post(new UsbPermissionEvent(false));
                }

                @Override public void onDataTransferStart() {
                    LOGD(TAG, "onDataTransferStart()");
                    turnOnUsb();

                    EventBus.getDefault().post(new UsbCommunicationEvent(true));
                }

                @Override public void onDataTransferEnd() {
                    LOGD(TAG, "onDataTransferEnd()");
                    turnOffUsb();

                    EventBus.getDefault().post(new UsbCommunicationEvent(false));
                }
            });

            usbHelper.start(getApplicationContext());
            LOGD(TAG, "USB helper started");
        }
    }

    // Stops listening attaching/detaching of USB devices
    private void stopUsbDetection() {
        LOGD(TAG, "stopUsbDetection()");
        if (usbHelper != null) {
            usbHelper.stop(getApplicationContext());
            usbHelper = null;
            LOGD(TAG, "USB helper stopped");
        }
    }

    //=================================================
    //  PLAYBACK
    //=================================================

    /**
     * Triggers loading and playback of the file at specified {@code filePath}. If {@code autoPlay} is {@code true} file
     * starts playing as soon as first samples are loaded, if it's {@code false} file is initially paused.
     */
    public void startPlayback(@NonNull String filePath, boolean autoPlay) {
        if (created) startPlaybackSource(filePath, autoPlay);
    }

    /**
     * Plays or pauses the playback depending on the {@code play} parameter.
     *
     * @param play Determines whether playback needs to be continued or paused.
     */
    public void togglePlayback(boolean play) {
        if (created && playbackSampleSource != null) {
            if (play) {
                playbackSampleSource.resumePlayback();
            } else {
                playbackSampleSource.pausePlayback();
            }
        }
    }

    /**
     * Stops the playback.
     */
    public void stopPlayback() {
        if (created) turnOffPlayback();
    }

    /**
     * Marks the start of the playback seek. Prepares the playback thread for the seek sequence. The playback
     * controller needs to call this method before starting the seek sequence. It should be called even if the seek
     * sequence is not really a sequence but just a simple "jump" to a specific playback point in time.
     */
    public void startPlaybackSeek() {
        if (created && playbackSampleSource != null) playbackSampleSource.seek(true);
    }

    /**
     * Rewinds or forwards the playback to the specified sample {@code position}.
     */
    public void seekPlayback(int position) {
        if (created && playbackSampleSource != null) playbackSampleSource.seek(AudioUtils.getByteCount(position));
    }

    /**
     * Marks the end of the playback seek. Informs the playback thread to stop the seek sequence. The playback
     * controller needs to call this method after finishing the seek sequence. It should be called even if the seek
     * sequence is not really a sequence but just a simple "jump" to a specific playback point in time.
     */
    public void stopPlaybackSeek() {
        if (created && playbackSampleSource != null) playbackSampleSource.seek(false);
    }

    /**
     * Returns number of playback samples.
     */
    public long getPlaybackLength() {
        if (isPlaybackMode()) return AudioUtils.getSampleCount(playbackSampleSource.getLength());

        return 0;
    }

    /**
     * Whether we are currently in the playback mode.
     */
    public boolean isPlaybackMode() {
        return playbackSampleSource != null;
    }

    /**
     * Whether playback is currently playing.
     */
    public boolean isAudioPlaying() {
        return isPlaybackMode() && playbackSampleSource.isPlaying();
    }

    /**
     * Whether playback is currenly in the seek mode.
     */
    public boolean isAudioSeeking() {
        return isPlaybackMode() && playbackSampleSource.isSeeking();
    }

    private void turnOnPlayback() {
        LOGD(TAG, "turnOnPlayback()");

        turnOffMicThread();
        turnOffUsb();

        if (playbackSampleSource != null) playbackSampleSource.start();
    }

    private void turnOffPlayback() {
        LOGD(TAG, "turnOffPlayback() - playbackSampleSource " + (playbackSampleSource != null ? "not null (stopping)"
            : "null"));

        // remove current USB input source
        if (playbackSampleSource != null) {
            playbackSampleSource.stop();
            playbackSampleSource = null;

            // we should clear buffer so that next buffer user doesn't have any residue
            if (processingBuffer != null) processingBuffer.clearBuffer();

            // post event that audio playback has stopped
            EventBus.getDefault().post(new AudioPlaybackStoppedEvent(true));
        }
    }

    private void startPlaybackSource(@NonNull final String filePath, boolean autoPlay) {
        if (ApacheCommonsLang3Utils.isNotBlank(filePath)) {
            turnOffPlayback();
            playbackSampleSource = new PlaybackSampleSource(filePath, autoPlay, this);
            playbackSampleSource.setOnPlaybackListener(new PlaybackSampleSource.PlaybackListener() {
                @Override public void onStart(long length, int sampleRate) {
                    // set file sample rate to be used while playing
                    setSampleRate(sampleRate);

                    // post event that audio playback has started, but post a sticky event
                    // because the view might sill not be initialized
                    EventBus.getDefault()
                        .postSticky(new AudioPlaybackStartedEvent(AudioUtils.getSampleCount(length), sampleRate));
                }

                @Override public void onResume(int sampleRate) {
                    // post event that audio playback has started
                    EventBus.getDefault().post(new AudioPlaybackStartedEvent(-1, sampleRate));
                }

                @Override public void onProgress(long progress, int sampleRate) {
                    EventBus.getDefault()
                        .post(new AudioPlaybackProgressEvent(AudioUtils.getSampleCount(progress), sampleRate));
                }

                @Override public void onPause() {
                    // post event that audio playback has started
                    EventBus.getDefault().post(new AudioPlaybackStoppedEvent(false));
                }

                @Override public void onStop() {
                    // we should clear buffer
                    if (processingBuffer != null) processingBuffer.clearBuffer();
                    // post event that audio playback has started
                    EventBus.getDefault().post(new AudioPlaybackStoppedEvent(true));
                }
            });
            turnOnPlayback(); // this will stop the microphone and in progress recording if any
        }
    }

    //=================================================
    //  RECORDING
    //=================================================

    /**
     * Starts recording from the active input source. If there is no active source microphone is turned on and recorded.
     */
    public void startRecording() {
        LOGD(TAG, "startRecording()");
        if (recordingSaver != null) return;

        try {
            // if there is not input source start the mic otherwise use the currently active input
            if (source == InputSourceType.NONE) turnOnMicThread();

            recordingSaver = new RecordingSaver();
            recordingSaver.setSampleRate(sampleRate);

            // post that recording of audio has started
            EventBus.getDefault().post(new AudioRecordingStartedEvent());
        } catch (IllegalStateException e) {
            Crashlytics.logException(e);
            ViewUtils.toast(getApplicationContext(), "No SD Card is available. Recording is disabled");
            stopRecording();
        } catch (IOException e) {
            Crashlytics.logException(e);
            ViewUtils.toast(getApplicationContext(),
                "Error occurred while trying to initiate recording. Please try again.");
            stopRecording();
        }
    }

    /**
     * Stop recording the active input source.
     */
    public void stopRecording() {
        LOGD(TAG, "stopRecording()");
        if (recordingSaver == null) return;

        try {
            // set current sample rate to be used when saving WAV file
            recordingSaver.requestStop();
            recordingSaver = null;

            // post that recording of audio has started
            EventBus.getDefault().post(new AudioRecordingStoppedEvent());
        } catch (IllegalStateException e) {
            Crashlytics.logException(e);
            ViewUtils.toast(getApplicationContext(),
                "Error occurred while trying to stop recording. Please check if your file recorded correctly.");
        }
    }

    /**
     * Whether active input source is being recorded or not.
     *
     * @return boolean {@code True} if active input is being recorded, {@code false} otherwise.
     */
    public boolean isRecording() {
        return (recordingSaver != null);
    }

    // Pass audio and events to the active RecordingSaver instance
    private void record(@NonNull SamplesWithMarkers samplesWithMarkers) {
        try {
            if (recordingSaver != null) {
                recordingSaver.writeAudioWithEvents(samplesWithMarkers);

                // post current recording progress
                EventBus.getDefault()
                    .post(new AudioRecordingProgressEvent(AudioUtils.getSampleCount(recordingSaver.getAudioLength()),
                        sampleRate));
            }
        } catch (IllegalStateException e) {
            Crashlytics.logException(e);
            LOGW(TAG, "Ignoring bytes received while not synced: " + e.getMessage());
        }
    }
}
