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

package com.backyardbrains.dsp;

import android.app.Service;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.os.Binder;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.backyardbrains.dsp.audio.AudioHelper;
import com.backyardbrains.dsp.audio.PlaybackSignalSource;
import com.backyardbrains.dsp.audio.Recorder;
import com.backyardbrains.dsp.usb.AbstractUsbSignalSource;
import com.backyardbrains.dsp.usb.AbstractUsbSignalSource.OnExpansionBoardTypeDetectionListener;
import com.backyardbrains.dsp.usb.AbstractUsbSignalSource.OnSpikerBoxHardwareTypeDetectionListener;
import com.backyardbrains.dsp.usb.AbstractUsbSignalSource.OnUsbSignalSourceDisconnectListener;
import com.backyardbrains.dsp.usb.UsbHelper;
import com.backyardbrains.events.AudioPlaybackProgressEvent;
import com.backyardbrains.events.AudioPlaybackStartedEvent;
import com.backyardbrains.events.AudioPlaybackStoppedEvent;
import com.backyardbrains.events.AudioRecordingProgressEvent;
import com.backyardbrains.events.AudioRecordingStartedEvent;
import com.backyardbrains.events.AudioRecordingStoppedEvent;
import com.backyardbrains.events.ExpansionBoardTypeDetectionEvent;
import com.backyardbrains.events.SpikerBoxHardwareTypeDetectionEvent;
import com.backyardbrains.events.UsbCommunicationEvent;
import com.backyardbrains.events.UsbDeviceConnectionEvent;
import com.backyardbrains.events.UsbPermissionEvent;
import com.backyardbrains.events.UsbSignalSourceDisconnectEvent;
import com.backyardbrains.filters.BandFilter;
import com.backyardbrains.filters.NotchFilter;
import com.backyardbrains.ui.MainActivity;
import com.backyardbrains.utils.ApacheCommonsLang3Utils;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.ExpansionBoardType;
import com.backyardbrains.utils.JniUtils;
import com.backyardbrains.utils.SignalAveragingTriggerType;
import com.backyardbrains.utils.SpikerBoxHardwareType;
import com.backyardbrains.utils.ViewUtils;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import java.io.IOException;
import org.greenrobot.eventbus.EventBus;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.LOGW;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * Manages a thread which monitors default audio input and pushes raw audio data to bound activities.
 *
 * @author Nathan Dotz <nate@backyardbrains.com>
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class ProcessingService extends Service implements SignalProcessor.OnProcessingListener {

    static final String TAG = makeLogTag(ProcessingService.class);

    private static final Filters FILTERS = new Filters();

    private final IBinder binder = new ServiceBinder();

    // Reference to data processor
    private final SignalProcessor signalProcessor;

    // Reference to the audio helper
    private AudioHelper audioHelper;
    // Reference to the USB helper
    private UsbHelper usbHelper;
    // Reference to the audio recorder
    private Recorder recorder;

    // Whether service is created
    private boolean created;
    //
    private boolean usbDisconnecting;

    // Reference to currently active signal source
    private AbstractSignalSource signalSource;

    /**
     * Provides a reference to {@link ProcessingService} to all bound clients.
     */
    public class ServiceBinder extends Binder {
        public ProcessingService getService() {
            return ProcessingService.this;
        }
    }

    /**
     * Constructor
     */
    public ProcessingService() {
        signalProcessor = new SignalProcessor(this);
    }

    //========================================================
    //  LIFECYCLE OVERRIDES
    //========================================================

    /**
     * {@inheritDoc}
     */
    @Override public void onCreate() {
        super.onCreate();
        LOGD(TAG, "onCreate()");

        // start processing signal
        signalProcessor.start();

        // we listen for audio input source change
        startAudioDetection();
        // we listen for USB attach/detach
        startUsbDetection();
        // start the recorder
        turnOnRecorder();

        created = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override public void onDestroy() {
        created = false;

        LOGD(TAG, "onDestroy()");
        stopAudioDetection();
        stopUsbDetection();
        turnOffMicrophone();
        turnOffPlayback();
        turnOffRecorder();

        signalProcessor.stop();

        super.onDestroy();
    }

    //========================================================
    //  BIND
    //========================================================

    /**
     * Returns a binding pointer for {@link MainActivity} to reference this object
     *
     * @return binding reference to this object
     * @see Service#onBind(Intent)
     */
    @Override public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    //========================================================
    //  SIGNAL PROCESSING
    //========================================================

    /**
     * Returns current sample rate.
     */
    public int getSampleRate() {
        return signalProcessor.getSampleRate();
    }

    /**
     * Returns current channel count.
     */
    public int getChannelCount() {
        return signalProcessor.getChannelCount();
    }

    /**
     * Returns index of the currently selected channel.
     */
    public int getSelectedChanel() {
        return signalProcessor.getSelectedChannel();
    }

    /**
     * Returns number of currently visible channels.
     */
    public int getVisibleChannelCount() {
        return signalProcessor.getVisibleChannelCount();
    }

    /**
     * Returns {@code true} if channel at specified {@code channelIndex} is visible. {@code false} otherwise.
     */
    public boolean isChannelVisible(int channelIndex) {
        return signalProcessor.isChannelVisible(channelIndex);
    }

    /**
     * Shows channel at {@code channelIndex}.
     */
    public void showChannel(int channelIndex) {
        signalProcessor.showChannel(channelIndex);
    }

    /**
     * Hides channel at {@code channelIndex}.
     */
    public void hideChannel(int channelIndex) {
        signalProcessor.hideChannel(channelIndex);
    }

    /**
     * Sets currently selected channel.
     */
    public void setSelectedChannel(int channelIndex) {
        signalProcessor.setSelectedChannel(channelIndex);
    }

    /**
     * Passes information about whether incoming signal should be averaged or not to data processor.
     */
    public void setSignalAveraging(boolean signalAveraging) {
        signalProcessor.setSignalAveraging(signalAveraging);
    }

    /**
     * Passes information about signal averaging trigger type to data processor.
     */
    public void setSignalAveragingTriggerType(@SignalAveragingTriggerType int triggerType) {
        signalProcessor.setSignalAveragingTriggerType(triggerType);
    }

    /**
     * Passes information about whether incoming signal should be processed through FFT.
     */
    public void setFftProcessing(boolean fftProcessing) {
        signalProcessor.setFftProcessing(fftProcessing);
    }

    /**
     * Returns filter that is additionally applied when processing incoming data.
     */
    public BandFilter getBandFilter() {
        return FILTERS.getBandFilter();
    }

    /**
     * Sets predefined band filters to be applied when processing incoming data.
     */
    public void setBandFilter(@Nullable BandFilter filter) {
        // pass filters to native code
        float low = (float) (filter != null ? filter.getLowCutOffFrequency() : -1f);
        float high = (float) (filter != null ? filter.getHighCutOffFrequency() : -1f);
        JniUtils.setBandFilter(low, high);

        FILTERS.setBandFilter(filter);
    }

    /**
     * Returns filter that is additionally applied when processing incoming data.
     */
    public NotchFilter getNotchFilter() {
        return FILTERS.getNotchFilter();
    }

    /**
     * Sets predefined band filters to be applied when processing incoming data.
     */
    public void setNotchFilter(@Nullable NotchFilter filter) {
        // pass filters to native code
        float centerFreq = (float) (filter != null ? filter.getCenterFrequency() : -1f);
        JniUtils.setNotchFilter(centerFreq);

        FILTERS.setNotchFilter(filter);
    }

    private void updateSignalProcessorBoardType(@SpikerBoxHardwareType int boardType) {
        signalProcessor.setBoardType(boardType);
    }

    private void updateSignalProcessorExpansionBoardType(@ExpansionBoardType int expansionBoardType) {
        signalProcessor.setExpansionBoardType(expansionBoardType);
    }

    //========================================================
    //  IMPLEMENTATIONS OF OnProcessingListener INTERFACE
    //========================================================

    /**
     * Passes processed data to the recording saver.
     *
     * @see SignalProcessor.OnProcessingListener#onDataProcessed(SignalData)
     */
    @Override public void onDataProcessed(@NonNull SignalData signalData) {
        record(signalData);
    }

    //========================================================
    //  CURRENT INPUT SOURCE
    //========================================================

    /**
     * Starts processing active input. If there is no active input Microphone is set as one.
     */
    public void startActiveInputSource() {
        if (created) {
            if (signalSource != null) {
                switch (signalSource.getType()) {
                    case SignalSource.Type.MICROPHONE:
                        turnOnMicrophone();
                        break;
                    case SignalSource.Type.USB:
                        turnOnUsb();
                        break;
                    case SignalSource.Type.FILE:
                        break;
                }
            } else {
                turnOnMicrophone();
            }
        }
    }

    /**
     * Stops processing active input.
     */
    public void stopActiveInputSource() {
        if (created) {
            turnOffMicrophone();
            turnOffUsb();
        }
    }

    /**
     * Whether USB is active input source.
     */
    public boolean isUsbActiveInput() {
        return signalSource != null && signalSource.isUsb();
    }

    //========================================================
    //  MICROPHONE
    //========================================================

    /**
     * Starts processing Microphone input. (Default)
     */
    public void startMicrophone() {
        if (created) turnOnMicrophone();
    }

    private void turnOnMicrophone() {
        LOGD(TAG, "turnOnMicrophone()");
        turnOffUsb();
        turnOffPlayback();

        // set current audio input source
        if (audioHelper.getAudioDevice() != null) {
            signalSource = audioHelper.getAudioDevice();
            signalProcessor.setSignalSource(signalSource);
            signalSource.start();
        }

        LOGD(TAG, "Microphone started");
    }

    private void turnOffMicrophone() {
        LOGD(TAG, "turnOffMicrophone()");
        stopRecording();

        if (signalSource != null && signalSource.isMicrophone()) {
            signalSource.stop();
            signalSource = null;
        }
        LOGD(TAG, "Microphone stopped");
    }

    private void startAudioDetection() {
        LOGD(TAG, "startAudioDetection()");
        if (audioHelper == null) {
            audioHelper = new AudioHelper();
            audioHelper.start(getApplicationContext());
            LOGD(TAG, "Audio helper started");
        }
    }

    private void stopAudioDetection() {
        LOGD(TAG, "startAudioDetection()");
        if (audioHelper != null) {
            audioHelper.stop();
            audioHelper = null;
            LOGD(TAG, "Audio helper stopped");
        }
    }

    //========================================================
    //  AM MODULATION
    //========================================================

    /**
     * Whether AM modulation is currently detected.
     */
    public boolean isAmModulationDetected() {
        return JniUtils.isAudioStreamAmModulated();
    }

    //========================================================
    //  USB
    //========================================================

    final OnSpikerBoxHardwareTypeDetectionListener spikerBoxDetectionListener = hardwareType -> {
        LOGD(TAG, "HARDWARE TYPE DETECTED: " + hardwareType);
        if (hardwareType != SpikerBoxHardwareType.UNKNOWN) {
            EventBus.getDefault().post(new SpikerBoxHardwareTypeDetectionEvent(hardwareType));
        }
        updateSignalProcessorBoardType(hardwareType);
    };

    final OnExpansionBoardTypeDetectionListener expansionBoardDetectionListener = expansionBoardType -> {
        LOGD(TAG, "EXPANSION BOARD DETECTED: " + expansionBoardType);
        if (expansionBoardType != ExpansionBoardType.NONE) {
            EventBus.getDefault().post(new ExpansionBoardTypeDetectionEvent(expansionBoardType));
        }
        updateSignalProcessorExpansionBoardType(expansionBoardType);
    };

    final OnUsbSignalSourceDisconnectListener usbSignalSourceDisconnectListener = () -> {
        usbDisconnecting = false;

        EventBus.getDefault().post(new UsbSignalSourceDisconnectEvent());
    };

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
        if (created) {
            usbDisconnecting = true;
            usbHelper.close();
        }
    }

    /**
     * Returns {@code true} if currently active input is USB input and is currently disconnecting.
     */
    public boolean isUsbDeviceDisconnecting() {
        return created && usbDisconnecting;
    }

    /**
     * Returns number of connected serial devices.
     */
    public int getUsbDeviceCount() {
        return created ? usbHelper.getDevicesCount() : 0;
    }

    /**
     * Temporary method that returns USB device under specified {@code index}.
     */
    @Nullable public UsbDevice getDevice(int index) {
        return usbHelper.getDevice(index);
    }

    // Turns on USB input processing
    void turnOnUsb() {
        LOGD(TAG, "turnOnUsb()");
        turnOffMicrophone();
        turnOffPlayback();

        // set current USB input source
        if (usbHelper.getUsbDevice() != null) {
            if (usbHelper.getUsbDevice().getHardwareType() != SpikerBoxHardwareType.UNKNOWN) {
                EventBus.getDefault()
                    .post(new SpikerBoxHardwareTypeDetectionEvent(usbHelper.getUsbDevice().getHardwareType()));
                updateSignalProcessorBoardType(usbHelper.getUsbDevice().getHardwareType());
            } else {
                usbHelper.getUsbDevice().addOnSpikerBoxHardwareTypeDetectionListener(spikerBoxDetectionListener);
            }
            usbHelper.getUsbDevice().addOnExpansionBoardTypeDetectionListener(expansionBoardDetectionListener);
            usbHelper.getUsbDevice().setOnUsbSignalSourceDisconnectListener(usbSignalSourceDisconnectListener);
            signalSource = usbHelper.getUsbDevice();
            signalProcessor.setSignalSource(signalSource);
        }

        LOGD(TAG, "USB communication started");
    }

    // Turns off USB input processing
    void turnOffUsb() {
        LOGD(TAG, "turnOffUsb()");
        stopRecording();

        if (signalSource != null && signalSource.isUsb()) {
            AbstractUsbSignalSource usbSignalSource = (AbstractUsbSignalSource) signalSource;
            usbSignalSource.removeOnSpikerBoxHardwareTypeDetectionListener(spikerBoxDetectionListener);
            usbSignalSource.removeOnExpansionBoardTypeDetectionListener(expansionBoardDetectionListener);
            if (!usbSignalSource.isDisconnecting()) signalSource.stop();
            signalSource = null;
        }
        LOGD(TAG, "USB communication ended");
    }

    // Starts listening attaching/detaching of USB devices
    private void startUsbDetection() {
        LOGD(TAG, "startUsbDetection()");
        if (usbHelper == null) {
            usbHelper = new UsbHelper(getApplicationContext(), new UsbHelper.UsbListener() {
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

    //========================================================
    //  RECORDING PLAYBACK
    //========================================================

    /**
     * Triggers loading and playback of the file at specified {@code filePath}. If {@code autoPlay} is {@code true} file
     * starts playing as soon as first samples are loaded, if it's {@code false} file is initially paused.
     */
    public void startPlayback(@NonNull String filePath, boolean autoPlay, int position) {
        if (created) startPlaybackSource(filePath, autoPlay, position);
    }

    /**
     * Plays or pauses the playback depending on the {@code play} parameter.
     *
     * @param play Determines whether playback needs to be continued or paused.
     */
    public void togglePlayback(boolean play) {
        if (created && isPlaybackMode()) {
            if (play) {
                ((PlaybackSignalSource) signalSource).resumePlayback();
            } else {
                ((PlaybackSignalSource) signalSource).pausePlayback();
            }
        }
    }

    /**
     * Stops the playback.
     */
    public void stopPlayback() {
        if (created) {
            if (isPlaybackMode()) ((PlaybackSignalSource) signalSource).pausePlayback();
            turnOffPlayback();
        }
    }

    /**
     * Marks the start of the playback seek. Prepares the playback thread for the seek sequence. The playback
     * controller needs to call this method before starting the seek sequence. It should be called even if the seek
     * sequence is not really a sequence but just a simple "jump" to a specific playback point in time.
     */
    public void startPlaybackSeek() {
        if (created && isPlaybackMode()) {
            ((PlaybackSignalSource) signalSource).seek(true);
            signalProcessor.setSignalSeeking(true);
        }
    }

    /**
     * Rewinds or forwards the playback to the specified sample {@code position}.
     */
    public void seekPlayback(int position) {
        if (created && isPlaybackMode()) {
            // let's pause the threshold while seeking
            JniUtils.pauseThreshold();

            final PlaybackSignalSource source = (PlaybackSignalSource) signalSource;
            source.seek(AudioUtils.getByteCount(position, source.getBitsPerSample()));
        }
    }

    /**
     * Marks the end of the playback seek. Informs the playback thread to stop the seek sequence. The playback
     * controller needs to call this method after finishing the seek sequence. It should be called even if the seek
     * sequence is not really a sequence but just a simple "jump" to a specific playback point in time.
     */
    public void stopPlaybackSeek() {
        if (created && isPlaybackMode()) {
            ((PlaybackSignalSource) signalSource).seek(false);
            signalProcessor.setSignalSeeking(false);
        }
    }

    /**
     * Returns number of playback samples.
     */
    public long getPlaybackLength() {
        if (isPlaybackMode()) {
            final PlaybackSignalSource source = (PlaybackSignalSource) signalSource;
            return AudioUtils.getSampleCount(source.getLength(), source.getBitsPerSample());
        }

        return 0;
    }

    /**
     * Whether we are currently in the playback mode.
     */
    public boolean isPlaybackMode() {
        return signalSource != null && signalSource.isFile();
    }

    /**
     * Whether playback is currently playing.
     */
    public boolean isAudioPlaying() {
        return isPlaybackMode() && ((PlaybackSignalSource) signalSource).isPlaying();
    }

    /**
     * Whether playback is currently paused.
     */
    public boolean isAudioPaused() {
        return isPlaybackMode() && !((PlaybackSignalSource) signalSource).isPlaying();
    }

    /**
     * Whether playback is currenly in the seek mode.
     */
    public boolean isAudioSeeking() {
        return isPlaybackMode() && ((PlaybackSignalSource) signalSource).isSeeking();
    }

    private void turnOnPlayback() {
        LOGD(TAG, "turnOnPlayback()");

        if (signalSource != null) signalSource.start();
        LOGD(TAG, "Playback started");
    }

    private void turnOffPlayback() {
        LOGD(TAG,
            "turnOffPlayback() - playbackSampleSource " + (signalSource != null ? "not null (stopping)" : "null"));

        // remove current USB input source
        if (signalSource != null && signalSource.isFile()) {
            signalSource.stop();
            signalSource = null;
        }

        // post event that audio playback has stopped
        EventBus.getDefault().post(new AudioPlaybackStoppedEvent(true));
    }

    private void startPlaybackSource(@NonNull final String filePath, boolean autoPlay, int position) {
        if (ApacheCommonsLang3Utils.isNotBlank(filePath)) {
            turnOffMicrophone();
            turnOffUsb();

            turnOffPlayback();
            if (signalSource == null) {
                signalSource = new PlaybackSignalSource(filePath, autoPlay, position);
                signalProcessor.setSignalSource(signalSource);
                ((PlaybackSignalSource) signalSource).setPlaybackListener(new PlaybackSignalSource.PlaybackListener() {

                    final AudioPlaybackProgressEvent progressEvent = new AudioPlaybackProgressEvent();

                    @Override public void onStart(long length, int sampleRate, int channelCount, int bitsPerSample) {
                        // post event that audio playback has started, but post a sticky event
                        // because the view might sill not be initialized
                        EventBus.getDefault()
                            .postSticky(new AudioPlaybackStartedEvent(AudioUtils.getSampleCount(length, bitsPerSample),
                                sampleRate, channelCount, bitsPerSample));
                    }

                    @Override public void onResume(int sampleRate, int channelCount, int bitsPerSample) {
                        // post event that audio playback has started
                        EventBus.getDefault()
                            .post(new AudioPlaybackStartedEvent(-1, sampleRate, channelCount, bitsPerSample));
                    }

                    @Override
                    public void onProgress(long progress, int sampleRate, int channelCount, int bitsPerSample) {
                        progressEvent.setProgress(AudioUtils.getSampleCount(progress, bitsPerSample));
                        progressEvent.setSampleRate(sampleRate);
                        progressEvent.setChannelCount(channelCount);
                        progressEvent.setBitsPerSample(bitsPerSample);
                        EventBus.getDefault().post(progressEvent);
                    }

                    @Override public void onPause() {
                        // post event that audio playback has started
                        EventBus.getDefault().post(new AudioPlaybackStoppedEvent(false));
                    }

                    @Override public void onStop() {
                        // post event that audio playback has started
                        EventBus.getDefault().post(new AudioPlaybackStoppedEvent(true));
                    }
                });
                turnOnPlayback(); // this will stop the microphone and in progress recording if any
            }
        }
    }

    //========================================================
    //  RECORDING AND LIVE PLAYBACK
    //========================================================

    /**
     * Starts recording from the active input source.
     */
    public void startRecording() {
        LOGD(TAG, "startRecording()");
        try {
            if (!isRecording()) {
                recorder.startRecording(signalProcessor.getSampleRate(), signalProcessor.getVisibleChannelCount());
            }

            // post that recording of audio has started
            EventBus.getDefault().post(new AudioRecordingStartedEvent());
        } catch (IllegalStateException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            ViewUtils.toast(getApplicationContext(),
                "No SD Card is available. Recording is disabled");
            stopRecording();
        } catch (IOException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
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
        if (isRecording()) recorder.stopRecording();

        // post that recording of audio has stopped
        EventBus.getDefault().post(new AudioRecordingStoppedEvent());
    }

    /**
     * Whether active input source is being recorded or not.
     *
     * @return boolean {@code True} if active input is being recorded, {@code false} otherwise.
     */
    public boolean isRecording() {
        return recorder != null && recorder.isRecording();
    }

    /**
     * Starts/stops playing back active input source on speakers.
     */
    public void setMuteSpeakers(boolean mute) {
        LOGD(TAG, "setMuteSpeakers(" + mute + ")");
        if (recorder != null) {
            if (mute) {
                if (recorder.isPlaying()) recorder.stopPlaying();
            } else {
                if (!recorder.isPlaying()) {
                    recorder.startPlaying(signalProcessor.getSampleRate(), signalProcessor.getChannelCount(),
                        signalProcessor.getBitsPerSample());
                }
            }
        }
    }

    public boolean isMuteSpeakers() {
        return recorder != null && !recorder.isPlaying();
    }

    private void turnOnRecorder() {
        if (recorder == null) recorder = new Recorder();
    }

    private void turnOffRecorder() {
        if (recorder != null) {
            recorder.requestStop();
            recorder = null;
        }
    }

    // Pass audio and events to the active Recorder instance
    private void record(@NonNull SignalData signalData) {
        try {
            if (isRecording()) {
                recorder.write(signalData);

                // recorder can be set to null if stopRecording() is called between this and previous line
                // post current recording progress
                EventBus.getDefault()
                    .post(new AudioRecordingProgressEvent(
                        AudioUtils.getSampleCount(recorder.getAudioLength(), signalProcessor.getBitsPerSample()),
                        signalProcessor.getSampleRate(), signalProcessor.getVisibleChannelCount(),
                        signalProcessor.getBitsPerSample()));
            }
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            LOGW(TAG, "Ignoring bytes received while not synced: " + e.getMessage());
        }
    }
}
