package com.backyardbrains.audio;

import android.media.AudioTrack;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.utls.AudioUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ShortBuffer;

import static com.backyardbrains.utls.LogUtils.LOGD;
import static com.backyardbrains.utls.LogUtils.LOGV;
import static com.backyardbrains.utls.LogUtils.makeLogTag;

class PlaybackThread1 {

    private static final String TAG = makeLogTag(PlaybackThread1.class);

    private final ReceivesAudio service;
    private final ShortBuffer samples;
    private final PlaybackListener listener;
    private final int samplesCount;
    private final int bufferSize;

    // Audio playback thread
    private Thread thread;
    // Number of already played samples
    private int totalWritten;
    // Thread control flag so we know when to stop execution
    private boolean shouldContinue;
    // True if audio is currently being played, false if it's paused or stopped
    private boolean playing;
    // Whether audio is currently being sought.
    private boolean seeking = false;

    @Retention(RetentionPolicy.SOURCE) @IntDef({
        PlaybackState.STATE_STARTED, PlaybackState.STATE_PAUSED, PlaybackState.STATE_RESUMED,
        PlaybackState.STATE_STOPPED
    }) @interface PlaybackState {

        /**
         * Playback state that indicates audio file started playing.
         */
        int STATE_STARTED = 0;
        /**
         * Playback state that indicates audio file is paused.
         */
        int STATE_PAUSED = 1;
        /**
         * Playback state that indicates audio file is resumed.
         */
        int STATE_RESUMED = 2;
        /**
         * Playback state that indicates audio file playback stopped.
         */
        int STATE_STOPPED = 3;
    }

    /**
     * Listener for handling different audio file playback events.
     */
    interface PlaybackListener {

        /**
         * Triggered while audio file is playing. It's triggered 30 times per second.
         */
        void onProgress(int progress);

        /**
         * Triggered when audio file playback finishes.
         */
        void onCompletion();

        /**
         * Triggered every time audio file playback changes status. Status is changed on play, pause and completion.
         */
        void onPlaybackStateChange(@PlaybackState int state);
    }

    /**
     * Class constructor.
     */
    PlaybackThread1(@NonNull ReceivesAudio service, short[] samples, @Nullable final PlaybackListener listener) {
        this.service = service;
        this.samples = ShortBuffer.wrap(samples);
        this.listener = listener;
        samplesCount = samples.length;
        bufferSize = AudioUtils.OUT_BUFFER_SIZE;
    }

    /**
     * Whether playback is in progress.
     */
    boolean isPlaying() {
        return playing;
    }

    /**
     * Returns length of the audio file as a number of samples.
     */
    int getLength() {
        return samplesCount;
    }

    /**
     * Starts/resumes playback.
     */
    void play() {
        if (thread != null) {
            playing = true;

            onPlaybackStateChange(PlaybackState.STATE_RESUMED);
        } else {
            // Start streaming in a thread
            shouldContinue = true;
            thread = new Thread(new Runnable() {
                @Override public void run() {
                    start();
                }
            });
            thread.start();
        }
    }

    /**
     * Pauses playback.
     */
    void pause() {
        if (thread == null) return;

        playing = false;

        onPlaybackStateChange(PlaybackState.STATE_PAUSED);
    }

    /**
     * Stops playback.
     */
    void stop() {
        if (thread == null) return;
        thread = null;

        playing = false;
        shouldContinue = false;

        onPlaybackStateChange(PlaybackState.STATE_STOPPED);
    }

    /**
     * Seeks audio file to specified position.
     */
    void seek(int position) {
        if (thread == null) return;

        if (samples.capacity() > position) {
            final boolean playingBeforeSeek = playing;
            seeking = true;
            playing = false;

            LOGD(TAG, "Samples position before: " + samples.position());

            final short[] buffer = new short[position];
            samples.clear();
            samples.get(buffer);
            totalWritten = samples.position();

            LOGD(TAG, "Samples position after: " + samples.position());

            playing = playingBeforeSeek;
            seeking = false;
        }
    }

    /**
     * Rewinds audio file.
     */
    void rewind() {
        samples.rewind();
    }

    // Triggers PlaybackListener.onPlaybackStateChange() listener
    private void onPlaybackStateChange(int state) {
        if (listener != null) listener.onPlaybackStateChange(state);
    }

    // Starts playing audio file
    private void start() {
        // setup audio track
        final AudioTrack track = AudioUtils.createAudioTrack();
        track.play();

        playing = true;

        onPlaybackStateChange(PlaybackState.STATE_STARTED);

        LOGV(TAG, "Audio streaming started");

        // rewind just in case
        samples.rewind();

        final short[] buffer = new short[bufferSize];
        final int limit = samplesCount;
        totalWritten = 0;
        while (samples.position() < limit && shouldContinue) {
            if (playing || seeking) {
                int numSamplesLeft = limit - samples.position();
                int samplesToWrite;
                if (numSamplesLeft >= buffer.length) {
                    samples.get(buffer);
                    samplesToWrite = buffer.length;
                } else {
                    System.arraycopy(new short[buffer.length - numSamplesLeft], 0, buffer, numSamplesLeft,
                        buffer.length - numSamplesLeft);
                    samples.get(buffer, 0, numSamplesLeft);
                    samplesToWrite = numSamplesLeft;
                }
                totalWritten += samplesToWrite;
                synchronized (service) {
                    service.receiveAudio(ShortBuffer.wrap(buffer));
                }
                // trigger progress listener
                if (listener != null) listener.onProgress(totalWritten);
                // play track if we're not seeking
                if (!seeking) track.write(buffer, 0, samplesToWrite);
            }
        }

        // release audio track
        track.release();

        if (listener != null) listener.onCompletion();
        onPlaybackStateChange(PlaybackState.STATE_STOPPED);

        LOGV(TAG, "Audio streaming finished. Samples written: " + totalWritten);
    }
}
