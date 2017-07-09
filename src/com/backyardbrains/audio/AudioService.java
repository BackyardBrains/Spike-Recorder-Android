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
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.events.AudioPlaybackProgressEvent;
import com.backyardbrains.events.AudioPlaybackStartedEvent;
import com.backyardbrains.events.AudioPlaybackStoppedEvent;
import com.backyardbrains.events.AudioRecordingProgressEvent;
import com.backyardbrains.events.AudioRecordingStartedEvent;
import com.backyardbrains.events.AudioRecordingStoppedEvent;
import com.backyardbrains.utils.ApacheCommonsLang3Utils;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.ViewUtils;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
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

    private static final int RING_BUFFER_NUM_SAMPLES = AudioUtils.SAMPLE_RATE * 6; // 6 seconds

    private final IBinder mBinder = new AudioServiceBinder();

    private RingBuffer audioBuffer;
    private MicListener micThread;
    private PlaybackThread playbackThread;
    private long lastBytePosition;
    private RecordingSaver recordingSaver;

    private ThresholdHelper averager;
    private boolean useAverager;

    private boolean created;

    /**
     * Provides a reference to {@link AudioService} to all bound clients.
     */
    public class AudioServiceBinder extends Binder {
        public AudioService getService() {
            return AudioService.this;
        }
    }

    //=================================================
    //  PUBLIC METHODS
    //=================================================

    public boolean isPlaybackMode() {
        return playbackThread != null;
    }

    public boolean isAudioPlaying() {
        return isPlaybackMode() && playbackThread.isPlaying();
    }

    public boolean isAudioSeeking() {
        return isPlaybackMode() && playbackThread.isSeeking();
    }

    //=================================================
    //  RING BUFFER
    //=================================================

    /**
     * return a byte array with in the appropriate order representing the last
     * 1.5 seconds of audio or so
     *
     * @return a ordinate-corrected version of the audio buffer
     */
    public short[] getAudioBuffer() {
        return audioBuffer != null ? audioBuffer.getArray() : new short[0];
    }

    public short[] getAverageBuffer() {
        if (averager != null) {
            return averager.getAveragedSamples();
        } else {
            return new short[0];
        }
    }

    // Adds specified audio data to ring buffer and saves position of the last added byte (progress)
    private void addToBuffer(ByteBuffer audioInfo, long lastBytePosition) {
        // add audio data to buffer
        addToBuffer(audioInfo);
        // last played byte position
        this.lastBytePosition = lastBytePosition;
    }

    // Adds specified audio data to ring buffer
    private void addToBuffer(ByteBuffer audioInfo) {
        // add audio data to buffer
        if (!useAverager) {
            audioBuffer.add(audioInfo);
        } else {
            averager.push(audioInfo);
        }
    }

    // Adds specified audio data to ring buffer
    private void addToBuffer(ShortBuffer audioInfo) {
        if (!useAverager) {
            audioBuffer.add(audioInfo);
        } else {
            averager.push(audioInfo);
        }
    }

    // Clears the ring buffer and resets last read byte position (progress)
    private void clearBuffer() {
        audioBuffer.clear();
        lastBytePosition = 0;
    }

    //=================================================
    //  THRESHOLD
    //=================================================

    @Nullable public Handler getTriggerHandler() {
        if (averager != null) {
            return averager.getHandler();
        } else {
            return null;
        }
    }

    public void setUseAverager(boolean bUse) {
        LOGD(TAG, "setUseAverager: " + (bUse ? "TRUE" : "FALSE"));
        useAverager = bUse;
    }

    public void setThresholdAveragedSampleCount(int averagedSampleCount) {
        averager.setMaxsize(averagedSampleCount);
    }

    public int getThresholdAveragedSampleCount() {
        return averager != null ? averager.getMaxsize() : ThresholdHelper.DEFAULT_SIZE;
    }

    //=================================================
    //  LIFECYCLE OVERRIDES
    //=================================================

    @Override public void onCreate() {
        super.onCreate();
        LOGD(TAG, "onCreate()");
        audioBuffer = new RingBuffer(RING_BUFFER_NUM_SAMPLES);
        averager = new ThresholdHelper();
        turnOnMicThread();

        created = true;
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override public void onDestroy() {
        created = false;

        LOGD(TAG, "onDestroy()");
        turnOffMicThread();
        turnOffPlaybackThread();
        averager.close();
        averager = null;
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
    //  IMPLEMENTATIONS OF ReceivesAudio INTERFACE
    //=================================================

    /**
     * Adds received audio to the ring buffer. If we're recording, it also passes it to the recording saver.
     *
     * @see com.backyardbrains.audio.ReceivesAudio#receiveAudio(ByteBuffer)
     */
    @Override public void receiveAudio(ByteBuffer audioInfo) {
        // add audio to ring buffer
        addToBuffer(audioInfo);
        // pass audio data to RecordingSaver
        if (recordingSaver != null) recordAudio(audioInfo);
    }

    @Override public void receiveAudio(ByteBuffer audioInfo, long lastBytePosition) {
        // add audio to ring buffer
        addToBuffer(audioInfo, lastBytePosition);
    }

    @Override public void receiveAudio(ShortBuffer audioInfo) {
        // add audio to ring buffer
        addToBuffer(audioInfo);
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
            clearBuffer();

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
            clearBuffer();
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
        if (isPlaybackMode()) return AudioUtils.getSampleCount(lastBytePosition);

        return 0;
    }

    public long getPlaybackLength() {
        if (isPlaybackMode()) return AudioUtils.getSampleCount(playbackThread.getLength());

        return 0;
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
            clearBuffer();
        }

        // post event that audio playback has started
        EventBus.getDefault().post(new AudioPlaybackStoppedEvent(true));
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
                    clearBuffer();
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
        LOGW(TAG, "start recording");
        if (recordingSaver != null) return false;

        try {
            turnOnMicThread();
            recordingSaver = new RecordingSaver();

            // post that recording of audio has started
            EventBus.getDefault().post(new AudioRecordingStartedEvent());
        } catch (IllegalStateException e) {
            ViewUtils.toast(getApplicationContext(), "No SD Card is available. Recording is disabled");
            stopRecording();
        }
        return true;
    }

    public boolean stopRecording() {
        if (recordingSaver != null) {
            LOGW(TAG, "stop recording");
            recordingSaver.stopRecording();
            recordingSaver = null;

            // post that recording of audio has started
            EventBus.getDefault().post(new AudioRecordingStoppedEvent());

            return true;
        }

        return false;
    }

    public boolean isRecording() {
        return (recordingSaver != null);
    }
}
