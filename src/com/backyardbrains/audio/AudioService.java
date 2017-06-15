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

    private MicListener micThread;
    private PlaybackThread playbackThread;
    private long lastBytePosition;
    private RingBuffer audioBuffer;
    private RecordingSaver mRecordingSaverInstance;

    private ThresholdHelper averager;
    private boolean bUseAverager = false;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- GETTERS SETTERS
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Provides a reference to {@link AudioService} to all bound clients.
     */
    public class AudioServiceBinder extends Binder {
        public AudioService getService() {
            return AudioService.this;
        }
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

    /**
     * return a byte array with in the appropriate order representing the last
     * 1.5 seconds of audio or so
     *
     * @return a ordinate-corrected version of the audio buffer
     */
    public short[] getAudioBuffer() {
        return audioBuffer.getArray();
    }

    public short[] getAverageBuffer() {
        if (averager != null) {
            return averager.getAveragedSamples();
        } else {
            return new short[0];
        }
    }

    @Nullable public Handler getTriggerHandler() {
        if (averager != null) {
            return averager.getHandler();
        } else {
            return null;
        }
    }

    public void setUseAverager(boolean bUse) {
        LOGD(TAG, "setUseAverager: " + (bUse ? "TRUE" : "FALSE"));
        bUseAverager = bUse;
    }

    public void setThresholdAveragedSampleCount(int averagedSampleCount) {
        averager.setMaxsize(averagedSampleCount);
    }

    public long getPlaybackProgress() {
        if (isPlaybackMode()) return AudioUtils.getSampleCount(lastBytePosition);

        return 0;
    }

    public long getPlaybackLength() {
        if (isPlaybackMode()) return AudioUtils.getSampleCount(playbackThread.getLength());

        return 0;
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
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override public void onDestroy() {
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
    //  MICROPHONE
    //=================================================

    /**
     * Starts processing default input (Microphone).
     */
    public void startMicrophone() {
        turnOnMicThread();
    }

    /**
     * Stops processing default input (Microphone).
     */
    public void stopMicrophone() {
        turnOffMicThread();
    }

    private void turnOnMicThread() {
        LOGD(TAG, "turnOnMicThread()");
        turnOffPlaybackThread();
        if (micThread == null) {
            micThread = null;
            micThread = new MicListener(this);

            // we should clear buffer
            audioBuffer.clear();

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
            audioBuffer.clear();
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
        startPlaybackThread(filePath, autoPlay);
    }

    public void togglePlayback(boolean play) {
        if (playbackThread != null) {
            if (play) {
                playbackThread.play();
            } else {
                playbackThread.pause();
            }
        }
    }

    public void stopPlayback() {
        turnOffPlaybackThread();
    }

    public void startPlaybackSeek() {
        if (playbackThread != null) playbackThread.seek(true);
    }

    public void seekPlayback(int position) {
        if (playbackThread != null) playbackThread.seek(AudioUtils.getByteCount(position));
    }

    public void stopPlaybackSeek() {
        if (playbackThread != null) playbackThread.seek(false);
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
            audioBuffer.clear();
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
                    audioBuffer.clear();
                    // post event that audio playback has started
                    EventBus.getDefault().post(new AudioPlaybackStoppedEvent(true));
                }
            });
            turnOnPlaybackThread(); // this will stop the microphone and in progress recording if any
        }
    }

    //=================================================
    //  IMPLEMENTATIONS OF ReceivesAudio INTERFACE
    //=================================================

    /**
     * On receiving audio, add it to the RingBuffer. If we're recording, also
     * dispatch it to the RecordingSaver instance.
     *
     * @see com.backyardbrains.audio.ReceivesAudio#receiveAudio(ByteBuffer)
     */
    @Override public void receiveAudio(ByteBuffer audioInfo) {
        if (!bUseAverager) {
            audioBuffer.add(audioInfo);
        } else {
            averager.push(audioInfo);
        }
        if (mRecordingSaverInstance != null) recordAudio(audioInfo);
    }

    @Override public void receiveAudio(ByteBuffer audioInfo, long lastBytePosition) {
        if (!bUseAverager) {
            audioBuffer.add(audioInfo);
        } else {
            averager.push(audioInfo);
        }
        this.lastBytePosition = lastBytePosition;
        if (mRecordingSaverInstance != null) recordAudio(audioInfo);
    }

    @Override public void receiveAudio(ShortBuffer audioInfo) {
        if (!bUseAverager) {
            audioBuffer.add(audioInfo);
        } else {
            averager.push(audioInfo);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- RECORD AUDIO
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * dispatch audio to the active RecordingSaver instance
     */
    private void recordAudio(ByteBuffer audioInfo) {
        audioInfo.clear();
        try {
            mRecordingSaverInstance.receiveAudio(audioInfo);
        } catch (IllegalStateException e) {
            LOGW(TAG, "Ignoring bytes received while not synced: " + e.getMessage());
        }
    }

    public boolean startRecording() {
        LOGW(TAG, "start recording");
        if (mRecordingSaverInstance != null) return false;

        try {
            turnOnMicThread();
            mRecordingSaverInstance = new RecordingSaver(this.getApplicationContext(), "BYB_");// theTime.toString());

            // post that recording of audio has started
            EventBus.getDefault().post(new AudioRecordingStartedEvent());
        } catch (IllegalStateException e) {
            ViewUtils.toast(getApplicationContext(), "No SD Card is available. Recording is disabled");
            stopRecording();
        }
        return true;
    }

    public boolean stopRecording() {
        if (mRecordingSaverInstance != null) {
            LOGW(TAG, "stop recording");
            mRecordingSaverInstance.finishRecording();
            mRecordingSaverInstance = null;

            // post that recording of audio has started
            EventBus.getDefault().post(new AudioRecordingStoppedEvent());

            return true;
        }

        return false;
    }

    public boolean isRecording() {
        return (mRecordingSaverInstance != null);
    }
}
