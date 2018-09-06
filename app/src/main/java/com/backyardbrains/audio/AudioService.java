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
import com.backyardbrains.data.processing.SampleSource;
import com.backyardbrains.data.processing.SamplesWithEvents;
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
import com.backyardbrains.usb.UsbHelper;
import com.backyardbrains.utils.ApacheCommonsLang3Utils;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.Benchmark;
import com.backyardbrains.utils.JniUtils;
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
 */
public class AudioService extends Service implements SampleSource.SampleSourceListener {

    static final String TAG = makeLogTag(AudioService.class);

    private static final Filters FILTERS = new Filters();

    private final IBinder binder = new ServiceBinder();

    // Reference to the data manager that stores and processes the data
    ProcessingBuffer processingBuffer;
    // Reference to the sample processor that will additionally process the samples
    private WeakReference<SampleProcessor> sampleProcessorRef;

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
    // Whether incoming samples should be averaged using a threshold
    private boolean averageSamples;

    // Reference to currently active sample source
    private AbstractSampleSource sampleSource;

    /**
     * Provides a reference to {@link AudioService} to all bound clients.
     */
    public class ServiceBinder extends Binder {
        public AudioService getService() {
            return AudioService.this;
        }
    }

    //========================================================
    //  LIFECYCLE OVERRIDES
    //========================================================

    @Override public void onCreate() {
        super.onCreate();
        LOGD(TAG, "onCreate()");

        processingBuffer = ProcessingBuffer.get();
        // we need to listen for USB attach/detach
        startUsbDetection();

        created = true;
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override public void onDestroy() {
        created = false;

        LOGD(TAG, "onDestroy()");
        stopUsbDetection();
        turnOffMicrophone();
        turnOffPlayback();

        processingBuffer.clearBuffer();
        processingBuffer = null;

        super.onDestroy();
    }

    //========================================================
    //  BIND
    //========================================================

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

    //========================================================
    //  DATA PROCESSING
    //========================================================

    /**
     * Sets the maximum time of incoming data to be processed at any given moment in seconds.
     */
    public void setMaxProcessingTimeInSeconds(double maxSeconds) {
        LOGD(TAG, "setMaxProcessingTimeInSeconds(" + maxSeconds + ")");
        if (maxSeconds <= 0) return; // max time needs to be positive

        final int bufferSize = (int) (maxSeconds * sampleRate);
        if (processingBuffer != null) processingBuffer.setSize(bufferSize);
        if (sampleSource != null) sampleSource.setBufferSize(bufferSize);

        this.maxTime = maxSeconds;
    }

    /**
     * Sets whether incoming samples should be average by a threshold value.
     */
    public void setAverageSamples(boolean averageSamples) {
        if (averageSamples == this.averageSamples) return;

        this.averageSamples = averageSamples;
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

    //========================================================
    //  FILTERS
    //========================================================

    /**
     * Returns filter that is additionally applied when processing incoming data.
     */
    public Filter getFilter() {
        if (sampleSource != null) return sampleSource.getFilter();
        return FILTERS.getFilter();
    }

    /**
     * Sets predefined filters to be applied when processing incoming data.
     */
    public void setFilter(@Nullable Filter filter) {
        if (sampleSource != null) sampleSource.setFilter(filter);
        if (filter != null) {
            JniUtils.setFilters((float) filter.getLowCutOffFrequency(), (float) filter.getHighCutOffFrequency());
        } else {
            JniUtils.setFilters(-1f, -1f);
        }
        FILTERS.setFilter(filter);
    }

    //========================================================
    //  IMPLEMENTATIONS OF OnSamplesReceiveListener INTERFACE
    //========================================================

    private final Benchmark benchmark = new Benchmark("AUDIO_DATA_PROCESSING").warmUp(200)
        .sessions(10)
        .measuresPerSession(200)
        .logBySession(false)
        .listener(new Benchmark.OnBenchmarkListener() {
            @Override public void onEnd() {
                //EventBus.getDefault().post(new ShowToastEvent("PRESS BACK BUTTON!!!!"));
            }
        });

    /**
     * Adds received samples and events to the ring buffer. If we're recording, it also passes it to the recording
     * saver.
     *
     * @see SampleSource.SampleSourceListener#onSamplesReceived(SamplesWithEvents)
     */
    @Override public void onSamplesReceived(@NonNull SamplesWithEvents samplesWithEvents) {
        //benchmark.start();
        passToDataManager(samplesWithEvents);
        //benchmark.end();
    }

    /**
     * Sets sample rate and updates filters.
     *
     * @see SampleSource.SampleSourceListener#onSampleRateDetected(int)
     */
    @Override public void onSampleRateDetected(int sampleRate) {
        setSampleRate(sampleRate);
    }

    private final Benchmark benchmarkT = new Benchmark("THRESHOLD_JAVA").warmUp(50)
        .sessions(10)
        .measuresPerSession(50)
        .logBySession(false)
        .listener(new Benchmark.OnBenchmarkListener() {
            @Override public void onEnd() {
                //EventBus.getDefault().post(new ShowToastEvent("PRESS BACK BUTTON!!!!"));
            }
        });

    // Passes data to data manager so it can be consumed by renderer
    private void passToDataManager(@NonNull SamplesWithEvents samplesWithEvents) {
        if (averageSamples) {
            //benchmarkT.start();
            JniUtils.processThreshold(samplesWithEvents, samplesWithEvents.samples, samplesWithEvents.sampleCount);
            //benchmarkT.end();
        }
        // pass data to data manager
        if (processingBuffer != null) processingBuffer.addToBuffer(samplesWithEvents);

        // pass data to RecordingSaver
        passToRecorder(samplesWithEvents);
    }

    // Passes data to audio recorder
    private void passToRecorder(@NonNull SamplesWithEvents samplesWithEvents) {
        if (recordingSaver != null) record(samplesWithEvents);
    }

    //========================================================
    //  CURRENT INPUT SOURCE
    //========================================================

    /**
     * Starts processing active input. If there is no active input Microphone is set as one.
     */
    public void startActiveInputSource() {
        if (created) {
            if (sampleSource != null) {
                switch (sampleSource.getType()) {
                    case SampleSource.Type.MICROPHONE:
                        turnOnMicrophone();
                        break;
                    case SampleSource.Type.USB:
                        turnOnUsb();
                        break;
                    case SampleSource.Type.FILE:
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
        return sampleSource != null && sampleSource.isUsb();
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

        if (sampleSource == null) {
            sampleSource = new MicrophoneSampleSource(this);
            sampleSource.setBufferSize(processingBuffer.getSize());
            sampleSource.start();
            LOGD(TAG, "Microphone started");

            // clear the buffer before on start
            processingBuffer.clearBuffer();
        }
    }

    private void turnOffMicrophone() {
        LOGD(TAG, "turnOffMicrophone()");
        stopRecording();

        if (sampleSource != null && sampleSource.isMicrophone()) {
            sampleSource.stop();
            sampleSource = null;
        }
        LOGD(TAG, "Microphone stopped");
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
        return created && sampleSource != null && sampleSource.isUsb()
            && ((AbstractUsbSampleSource) sampleSource).getHardwareType() == hardwareType;
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
            } else {
                usbHelper.getUsbDevice()
                    .setOnSpikerBoxHardwareTypeDetectionListener(
                        new AbstractUsbSampleSource.OnSpikerBoxHardwareTypeDetectionListener() {
                            @Override public void onHardwareTypeDetected(int hardwareType) {
                                LOGD(TAG, "HARDWARE TYPE DETECTED: " + hardwareType);
                                EventBus.getDefault().post(new SpikerBoxHardwareTypeDetectionEvent(hardwareType));
                            }
                        });
            }
            sampleSource = usbHelper.getUsbDevice();
            sampleSource.setBufferSize(processingBuffer.getSize());

            // clear the buffer before on start
            processingBuffer.clearBuffer();
        }

        // resume communication with USB
        usbHelper.resume();
        LOGD(TAG, "USB communication started");
    }

    // Turns off USB input processing
    void turnOffUsb() {
        LOGD(TAG, "turnOffUsb()");
        stopRecording();

        // pause communication with USB
        usbHelper.pause();
        if (sampleSource != null && sampleSource.isUsb()) {
            sampleSource.stop();
            sampleSource = null;
        }
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

    //========================================================
    //  PLAYBACK
    //========================================================

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
        if (created && isPlaybackMode()) {
            if (play) {
                ((PlaybackSampleSource) sampleSource).resumePlayback();
            } else {
                ((PlaybackSampleSource) sampleSource).pausePlayback();
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
        if (created && isPlaybackMode()) ((PlaybackSampleSource) sampleSource).seek(true);
    }

    /**
     * Rewinds or forwards the playback to the specified sample {@code position}.
     */
    public void seekPlayback(int position) {
        if (created && isPlaybackMode()) {
            ((PlaybackSampleSource) sampleSource).seek(AudioUtils.getByteCount(position));
        }
    }

    /**
     * Marks the end of the playback seek. Informs the playback thread to stop the seek sequence. The playback
     * controller needs to call this method after finishing the seek sequence. It should be called even if the seek
     * sequence is not really a sequence but just a simple "jump" to a specific playback point in time.
     */
    public void stopPlaybackSeek() {
        if (created && isPlaybackMode()) ((PlaybackSampleSource) sampleSource).seek(false);
    }

    /**
     * Returns number of playback samples.
     */
    public long getPlaybackLength() {
        if (isPlaybackMode()) return AudioUtils.getSampleCount(((PlaybackSampleSource) sampleSource).getLength());

        return 0;
    }

    /**
     * Whether we are currently in the playback mode.
     */
    public boolean isPlaybackMode() {
        return sampleSource != null && sampleSource.isFile();
    }

    /**
     * Whether playback is currently playing.
     */
    public boolean isAudioPlaying() {
        return isPlaybackMode() && ((PlaybackSampleSource) sampleSource).isPlaying();
    }

    /**
     * Whether playback is currenly in the seek mode.
     */
    public boolean isAudioSeeking() {
        return isPlaybackMode() && ((PlaybackSampleSource) sampleSource).isSeeking();
    }

    private void turnOnPlayback() {
        LOGD(TAG, "turnOnPlayback()");

        if (sampleSource != null) sampleSource.start();
        LOGD(TAG, "Playback started");
    }

    private void turnOffPlayback() {
        LOGD(TAG,
            "turnOffPlayback() - playbackSampleSource " + (sampleSource != null ? "not null (stopping)" : "null"));

        // remove current USB input source
        if (sampleSource != null && sampleSource.isFile()) {
            sampleSource.stop();
            sampleSource = null;
        }

        // post event that audio playback has stopped
        EventBus.getDefault().post(new AudioPlaybackStoppedEvent(true));
    }

    private void startPlaybackSource(@NonNull final String filePath, boolean autoPlay) {
        if (ApacheCommonsLang3Utils.isNotBlank(filePath)) {
            turnOffMicrophone();
            turnOffUsb();

            turnOffPlayback();
            if (sampleSource == null) {
                sampleSource = new PlaybackSampleSource(filePath, autoPlay, this);
                sampleSource.setBufferSize(processingBuffer.getSize());
                ((PlaybackSampleSource) sampleSource).setPlaybackListener(new PlaybackSampleSource.PlaybackListener() {

                    final AudioPlaybackProgressEvent progressEvent = new AudioPlaybackProgressEvent();

                    @Override public void onStart(long length, int sampleRate) {
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
                        progressEvent.setProgress(AudioUtils.getSampleCount(progress));
                        progressEvent.setSampleRate(sampleRate);
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

                // clear the buffer before on start
                processingBuffer.clearBuffer();
            }
        }
    }

    //========================================================
    //  RECORDING
    //========================================================

    /**
     * Starts recording from the active input source. If there is no active source microphone is turned on and recorded.
     */
    public void startRecording() {
        LOGD(TAG, "startRecording()");
        try {
            if (recordingSaver == null) recordingSaver = new RecordingSaver();
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
        try {
            // set current sample rate to be used when saving WAV file
            if (recordingSaver != null) recordingSaver.requestStop();
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
    private void record(@NonNull SamplesWithEvents samplesWithEvents) {
        try {
            if (recordingSaver != null) recordingSaver.writeAudioWithEvents(samplesWithEvents);

            // recordingSaver can be set to null if stopRecording() is called between this and previous line
            if (recordingSaver != null) {
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
