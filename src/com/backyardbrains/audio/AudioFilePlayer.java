package com.backyardbrains.audio;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.audio.PlaybackThread1.PlaybackState;
import java.io.File;

import static com.backyardbrains.utls.LogUtils.LOGD;
import static com.backyardbrains.utls.LogUtils.LOGE;
import static com.backyardbrains.utls.LogUtils.makeLogTag;

class AudioFilePlayer implements PlaybackThread1.PlaybackListener {

    private static final String TAG = makeLogTag(AudioFilePlayer.class);

    private final Context context;
    private final ReceivesAudio audioReceiver;
    private final PlayerListener listener;

    // Manages audio file playback
    private PlaybackThread1 filePlayback;
    // Manages audio file loading
    private RecordingReader1 fileLoader;

    // Whether file has been loaded
    private boolean fileLoaded;
    // Whether file can be played or not
    private boolean shouldPlay;
    // Current playback progress
    private long progress = 0;

    /**
     * Listener for handling audio playback events.
     */
    interface PlayerListener {

        /**
         * Triggered on playback start.
         */
        void onPlaybackStart(int length);

        /**
         * Triggered when playback resumes after pause.
         */
        void onResume();

        /**
         * Triggered 30 times per sec during playback progress.
         */
        void onProgress(long progress);

        /**
         * Triggered when playback pauses.
         */
        void onPause();

        /**
         * Triggered on playback completion.
         */
        void onPlaybackStop();
    }

    AudioFilePlayer(@NonNull Context context, @Nullable ReceivesAudio audioReceiver,
        @Nullable PlayerListener listener) {
        this.context = context.getApplicationContext();
        this.audioReceiver = audioReceiver;
        this.listener = listener;
    }

    /**
     * Returns array of audio file samples.
     */
    public short[] getBuffer() {
        if (fileLoader != null) {
            return fileLoader.getDataShorts();
        } else {
            return new short[1];
        }
    }

    /**
     * Loads audio file with specified {@code filePath}.
     */
    void load(@NonNull String filePath) {
        final File file = new File(filePath);
        if (file.exists()) {
            fileLoader = null;
            fileLoader = new RecordingReader1(file, new RecordingReader1.AudioFileReadListener() {
                @Override public void audioFileRead() {
                    LOGD(TAG, "AudioFileRead");

                    fileLoaded = true;
                    if (shouldPlay) {
                        play();
                        shouldPlay = false;
                    }
                }
            });

            fileLoaded = false;
            shouldPlay = false;
        } else {
            LOGE(TAG, "Cant load file: it doesn't exist!!");
        }
    }

    /**
     * Returns current playback progress.
     */
    long getProgress() {
        return progress;
    }

    /**
     * Returns {@code true} if file is currently playing, {@code false} otherwise.
     */
    boolean isPlaying() {
        return filePlayback != null && filePlayback.isPlaying();
    }

    /**
     * Starts playing file.
     */
    void play() {
        if (fileLoaded) {
            turnOnPlaybackThread();
        } else {
            shouldPlay = true;
        }
    }

    /**
     * Pauses file playback.
     */
    void pause() {
        if (fileLoaded) {
            if (filePlayback != null) filePlayback.pause();
        } else {
            shouldPlay = false;
        }
    }

    /**
     * Stops file playback.
     */
    void stop() {
        turnOffPlaybackThread();

        if (fileLoader != null) {
            fileLoader.close();
            fileLoader = null;
        }
    }

    /**
     * Seeks playback to specified position.
     */
    void seek(int position) {
        if (fileLoaded && filePlayback != null) filePlayback.seek(position);
    }

    /**
     * Rewinds audio file.
     */
    private void rewind() {
        if (filePlayback != null) {
            filePlayback.rewind();

            turnOffPlaybackThread();
        }
    }

    // Actually starts the playback.
    private void turnOnPlaybackThread() {
        LOGD(TAG, "turnOnPlaybackThread()");
        if (filePlayback != null) {
            filePlayback.play();
        } else if (fileLoader != null && audioReceiver != null && fileLoaded) {
            filePlayback = null;
            filePlayback = new PlaybackThread1(audioReceiver, fileLoader.getDataShorts(), this);
            filePlayback.play();
        } else {
            String m = "TurnOnPlaybackThread failed! ";
            if (fileLoader == null) {
                m += "Reader is Null ";
            }
            if (audioReceiver == null) {
                m += "audioReceiver is null";
            }
            if (!fileLoaded) {
                m += "file not loaded";
            }
            LOGD(TAG, m);
        }
    }

    // Stops playback and releases resources.
    private void turnOffPlaybackThread() {
        LOGD(TAG, "turnOffPlaybackThread()");
        if (filePlayback != null) {
            filePlayback.stop();
            filePlayback = null;
        }
    }

    //==============================================
    //  PlaybackListener INTERFACE IMPLEMENTATION
    //==============================================

    @Override public void onProgress(int progress) {
        this.progress = (0x7FFFFFFF & progress) + ((progress & 0x80000000) >> 31) * 0x80000000;
        if (listener != null) listener.onProgress(this.progress);
    }

    @Override public void onCompletion() {
        LOGD(TAG, "onCompletion");

        pause();
        rewind();

        //if (listener != null) listener.onPlaybackStop();
        // FIXME: 4/11/2017 legacy code, should be removed
        if (context != null) {
            Intent i = new Intent();
            i.setAction("BYBAudioFilePlaybackEnded");
            context.sendBroadcast(i);
        }
    }

    @Override public void onPlaybackStateChange(@PlaybackState int state) {
        final String s;
        if (listener != null) {
            switch (state) {
                case PlaybackState.STATE_STARTED:
                    s = "started";
                    listener.onPlaybackStart(filePlayback.getLength());
                    break;
                case PlaybackState.STATE_PAUSED:
                    s = "paused";
                    listener.onPause();
                    break;
                case PlaybackState.STATE_RESUMED:
                    s = "resumed";
                    listener.onResume();
                    break;
                case PlaybackState.STATE_STOPPED:
                    s = "stopped";
                    listener.onPlaybackStop();
                    break;
                default:
                    s = "unknown";
                    break;
            }
        } else {
            s = "no listener";
        }
        LOGD(TAG, "onPlaybackStateChange - " + s);

        // FIXME: 4/11/2017 legacy code, should be removed
        if (context != null) {
            Intent i = new Intent();
            i.setAction("BYBUpdateUI");
            context.sendBroadcast(i);
        }
    }
}
