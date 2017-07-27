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
import com.backyardbrains.data.DataManager;
import com.backyardbrains.data.DataProcessor;
import com.backyardbrains.events.AudioPlaybackProgressEvent;
import com.backyardbrains.events.AudioPlaybackStartedEvent;
import com.backyardbrains.events.AudioPlaybackStoppedEvent;
import com.backyardbrains.events.AudioRecordingProgressEvent;
import com.backyardbrains.events.AudioRecordingStartedEvent;
import com.backyardbrains.events.AudioRecordingStoppedEvent;
import com.backyardbrains.events.UsbDeviceConnectionEvent;
import com.backyardbrains.events.UsbPermissionEvent;
import com.backyardbrains.utils.ApacheCommonsLang3Utils;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.ViewUtils;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import org.greenrobot.eventbus.EventBus;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.LOGW;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * Manages a thread which monitors default audio input and pushes raw audio data to bound activities.
 *
 * @author Nathan Dotz <nate@backyardbrains.com>
 * @version 1
 */

public class AudioService extends Service implements ReceivesAudio {

    private static final String TAG = makeLogTag(AudioService.class);

    private final IBinder mBinder = new ServiceBinder();

    // Reference to the data manager that stores and processes the data
    private DataManager dataManager;
    // Reference to the data processor that will additionally process the data
    private WeakReference<DataProcessor> dataProcessorRef;

    // Reference to the microphone data source
    private MicListener micThread;
    // Reference to the playback data source
    private PlaybackThread playbackThread;
    //
    private UsbHelper usbHelper;
    // Reference to the audio recorder
    private RecordingSaver recordingSaver;

    private boolean created;

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

        dataManager = DataManager.get();
        // we need to listen for USB attach/detach
        turnOnUsb();

        created = true;
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override public void onDestroy() {
        created = false;

        LOGD(TAG, "onDestroy()");
        turnOffMicThread();
        turnOffUsb();
        turnOffPlaybackThread();

        dataManager = null;

        super.onDestroy();
    }

    //=================================================
    //  BIND
    //=================================================

    /**
     * return a binding pointer for GL threads to reference this object
     *
     * @return binding reference to this object
     * @see android.app.Service#onBind(android.content.Intent)
     */
    @Override public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    @Override public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    //=================================================
    //  DATA PROCESSING
    //=================================================

    // Returns the activity reference and if reference is lost, logs the calling method.
    @Nullable @SuppressWarnings("WeakerAccess") DataProcessor getProcessor(@NonNull String methodName) {
        // case if data processor is not set at all
        if (dataProcessorRef == null) return null;

        final DataProcessor processor = dataProcessorRef.get();
        if (processor == null) LOGD(TAG, "Service doesn't have DataProcessor reference, ignoring (" + methodName + ")");

        return processor;
    }

    /**
     * Sets the data processor that will be used to additionally process incoming data.
     */
    public void setDataProcessor(@NonNull DataProcessor dataProcessor) {
        dataProcessorRef = new WeakReference<>(dataProcessor);
    }

    /**
     * Clears data processor.
     */
    public void clearDataProcessor() {
        dataProcessorRef = null;
    }

    //=================================================
    //  IMPLEMENTATIONS OF ReceivesAudio INTERFACE
    //=================================================

    /**
     * Adds received audio to the ring buffer. If we're recording, it also passes it to the recording saver.
     *
     * @see com.backyardbrains.audio.ReceivesAudio#receiveAudio(ByteBuffer)
     */
    @Override public void receiveAudio(@NonNull ByteBuffer audioData) {
        if (dataManager != null) {
            if (getProcessor("receiveAudio(ByteBuffer)") != null) {
                // additionally process data if processor is provided before passing it to data manager
                //noinspection ConstantConditions
                dataManager.addToBuffer(getProcessor("receiveAudio(ByteBuffer)").processData(audioData));
            } else {
                // pass data to data manager
                dataManager.addToBuffer(audioData);
            }
        }
        // pass data to RecordingSaver
        if (recordingSaver != null) recordAudio(audioData);
    }

    /**
     * Adds received audio and position of the last read byte to the ring buffer.
     *
     * @see com.backyardbrains.audio.ReceivesAudio#receiveAudio(ByteBuffer, long)
     */
    @Override public void receiveAudio(@NonNull ByteBuffer audioData, long lastBytePosition) {
        if (dataManager != null) {
            if (getProcessor("receiveAudio(ByteBuffer, long)") != null) {
                // additionally process data if processor is provided before passing it to data manager
                //noinspection ConstantConditions
                dataManager.addToBuffer(getProcessor("receiveAudio(ByteBuffer, long)").processData(audioData));
            } else {
                // pass data to data manager
                dataManager.addToBuffer(audioData, lastBytePosition);
            }
        }
    }

    //=================================================
    //  MICROPHONE
    //=================================================

    /**
     * Starts processing default input (Microphone).
     */
    public void startMicrophone() {
        if (created) turnOnMicThread();
    }

    /**
     * Stops processing default input (Microphone).
     */
    public void stopMicrophone() {
        if (created) turnOffMicThread();
    }

    private void turnOnMicThread() {
        LOGD(TAG, "turnOnMicThread()");
        turnOffPlaybackThread();
        if (micThread == null) {
            micThread = null;
            micThread = new MicListener(this);

            // we should clear buffer
            dataManager.clearBuffer();

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
            dataManager.clearBuffer();
        }
    }

    //=================================================
    //  USB
    //=================================================

    /**
     * Tries to connect to the USB device with the specified {@code deviceName}.
     */
    public void connectToUsbDevice(@NonNull String deviceName) {
        if (created) usbHelper.connect(getApplicationContext(), deviceName);
    }

    /**
     * Disconnects from the currently connected USB device.
     */
    public void disconnectFromUsbDevice() {
        if (created) usbHelper.disconnect();
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

    private void turnOnUsb() {
        LOGD(TAG, "turnOnUsb()");
        if (usbHelper == null) {
            usbHelper = new UsbHelper(getApplicationContext(), this, new UsbHelper.UsbListener() {
                @Override public void onDeviceAttached() {
                    EventBus.getDefault().post(new UsbDeviceConnectionEvent(true));
                }

                @Override public void onDeviceDetached() {
                    EventBus.getDefault().post(new UsbDeviceConnectionEvent(false));
                }

                @Override public void onDataTransferStart() {
                    EventBus.getDefault().post(new UsbPermissionEvent(true));
                }

                @Override public void onPermissionDenied() {
                    EventBus.getDefault().post(new UsbPermissionEvent(false));
                }
            });

            // we should clear buffer
            dataManager.clearBuffer();

            usbHelper.start(getApplicationContext());
            LOGD(TAG, "USB helper started");
        }
    }

    private void turnOffUsb() {
        LOGD(TAG, "turnOffUsb()");
        if (usbHelper != null) {
            usbHelper.disconnect();
            usbHelper.stop(getApplicationContext());
            usbHelper = null;
            LOGD(TAG, "USB helper stopped");

            // we should clear buffer so that next buffer user doesn't have any residue
            dataManager.clearBuffer();
        }
    }

    //=================================================
    //  AUDIO PLAYBACK
    //=================================================

    /**
     * Triggers loading and playback of the file at specified {@code filePath}. If {@code autoPlay} is {@code true} file
     * starts playing as soon as first samples are loaded, if it's {@code false} file is initially paused.
     */
    public void startPlayback(@NonNull String filePath, boolean autoPlay) {
        if (created) startPlaybackThread(filePath, autoPlay);
    }

    public void togglePlayback(boolean play) {
        if (created && playbackThread != null) {
            if (play) {
                playbackThread.play();
            } else {
                playbackThread.pause();
            }
        }
    }

    public void stopPlayback() {
        if (created) turnOffPlaybackThread();
    }

    public void startPlaybackSeek() {
        if (created && playbackThread != null) playbackThread.seek(true);
    }

    public void seekPlayback(int position) {
        if (created && playbackThread != null) playbackThread.seek(AudioUtils.getByteCount(position));
    }

    public void stopPlaybackSeek() {
        if (created && playbackThread != null) playbackThread.seek(false);
    }

    public long getPlaybackProgress() {
        if (isPlaybackMode()) return AudioUtils.getSampleCount(dataManager.getLastBytePosition());

        return 0;
    }

    public long getPlaybackLength() {
        if (isPlaybackMode()) return AudioUtils.getSampleCount(playbackThread.getLength());

        return 0;
    }

    public boolean isPlaybackMode() {
        return playbackThread != null;
    }

    public boolean isAudioPlaying() {
        return isPlaybackMode() && playbackThread.isPlaying();
    }

    public boolean isAudioSeeking() {
        return isPlaybackMode() && playbackThread.isSeeking();
    }

    private void turnOnPlaybackThread() {
        LOGD(TAG, "turnOnPlaybackThread()");
        if (playbackThread != null) {
            turnOffMicThread();

            playbackThread.play();
        }
    }

    private void turnOffPlaybackThread() {
        LOGD(TAG,
            "turnOffPlaybackThread() - playbackThread " + (playbackThread != null ? "not null (stopping)" : "null"));

        if (playbackThread != null) {
            playbackThread.stop();
            playbackThread = null;

            // we should clear buffer so that next buffer user doesn't have any residue
            dataManager.clearBuffer();

            // post event that audio playback has stopped
            EventBus.getDefault().post(new AudioPlaybackStoppedEvent(true));
        }
    }

    private void startPlaybackThread(@NonNull String filePath, boolean autoPlay) {
        if (ApacheCommonsLang3Utils.isNotBlank(filePath)) {
            turnOffPlaybackThread();
            playbackThread = new PlaybackThread(this, filePath, autoPlay, new PlaybackThread.PlaybackListener() {
                @Override public void onStart(long length) {
                    // post event that audio playback has started, but post a sticky event
                    // because the view might sill not be initialized
                    EventBus.getDefault().postSticky(new AudioPlaybackStartedEvent(AudioUtils.getSampleCount(length)));
                }

                @Override public void onResume() {
                    // post event that audio playback has started
                    EventBus.getDefault().post(new AudioPlaybackStartedEvent(-1));
                }

                @Override public void onProgress(long progress) {
                    EventBus.getDefault().post(new AudioPlaybackProgressEvent(AudioUtils.getSampleCount(progress)));
                }

                @Override public void onPause() {
                    // post event that audio playback has started
                    EventBus.getDefault().post(new AudioPlaybackStoppedEvent(false));
                }

                @Override public void onStop() {
                    // we should clear buffer
                    dataManager.clearBuffer();
                    // post event that audio playback has started
                    EventBus.getDefault().post(new AudioPlaybackStoppedEvent(true));
                }
            });
            turnOnPlaybackThread(); // this will stop the microphone and in progress recording if any
        }
    }

    //=================================================
    //  AUDIO RECORDING
    //=================================================

    /**
     * dispatch audio to the active RecordingSaver instance
     */
    private void recordAudio(ByteBuffer audioInfo) {
        audioInfo.clear();
        try {
            recordingSaver.writeAudio(audioInfo);

            // post current recording progress
            EventBus.getDefault()
                .post(new AudioRecordingProgressEvent(AudioUtils.getSampleCount(recordingSaver.getAudioLength())));
        } catch (IllegalStateException e) {
            LOGW(TAG, "Ignoring bytes received while not synced: " + e.getMessage());
        }
    }

    public boolean startRecording() {
        LOGD(TAG, "startRecording()");
        if (recordingSaver != null) return false;

        try {
            turnOnMicThread();
            recordingSaver = new RecordingSaver();

            // post that recording of audio has started
            EventBus.getDefault().post(new AudioRecordingStartedEvent());
        } catch (IllegalStateException e) {
            ViewUtils.toast(getApplicationContext(), "No SD Card is available. Recording is disabled");
            stopRecording();
        } catch (IOException e) {
            ViewUtils.toast(getApplicationContext(),
                "Error occurred while trying to initiate recording. Please try again.");
            stopRecording();
        }

        return true;
    }

    public boolean stopRecording() {
        LOGD(TAG, "stopRecording()");
        if (recordingSaver == null) return false;

        try {
            recordingSaver.stopRecording();
            recordingSaver = null;

            // post that recording of audio has started
            EventBus.getDefault().post(new AudioRecordingStoppedEvent());
        } catch (IllegalStateException e) {
            ViewUtils.toast(getApplicationContext(),
                "Error occurred while trying to stop recording. Please check if your file recorded correctly.");

            return false;
        }

        return true;
    }

    public boolean isRecording() {
        return (recordingSaver != null);
    }
}
