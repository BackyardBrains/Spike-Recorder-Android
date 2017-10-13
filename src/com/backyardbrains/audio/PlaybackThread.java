package com.backyardbrains.audio;

import android.media.AudioTrack;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.BufferUtils;
import com.crashlytics.android.Crashlytics;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.LOGE;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
class PlaybackThread {

    private static final String TAG = makeLogTag(PlaybackThread.class);

    // Service to which we push audio data
    private final ReceivesAudio service;
    // Triggers audio playback events
    private PlaybackListener listener;
    // Path to the audio file
    private final String filePath;
    // Whether file should start playing right away
    private boolean autoPlay;
    // Size of buffer (chunk) for the audio file reading
    private final int bufferSize;
    // Size of buffer (chunk) to read when seeking (6 seconds)
    private final int seekBufferSize;

    // Audio playback thread
    private Thread thread;
    // Random access file stream that holds audio file that's being played
    private BYBAudioFile raf;
    // True if audio is currently being played, false if it's paused or stopped
    private boolean playing;
    // Whether audio is currently being sought.
    private boolean seeking;
    // Flag that indicates whether thread should be running
    private boolean done;
    // Position of the playback head
    private long progress;
    // Length of the audio file in bytes
    private long duration;

    /**
     * Listener for handling different audio file playback events.
     */
    interface PlaybackListener {

        /**
         * Triggered on playback start.
         *
         * @param length Length of the playback in bytes.
         */
        void onStart(long length);

        /**
         * Triggered when playback resumes after pause.
         */
        void onResume();

        /**
         * Triggered constantly during playback progress.
         *
         * @param progress Current byte being played
         */
        void onProgress(long progress);

        /**
         * Triggered when playback pauses.
         */
        void onPause();

        /**
         * Triggered on playback stop.
         */
        void onStop();
    }

    /**
     * Class constructor.
     *
     * @param service the service that implements the {@link ReceivesAudio}
     * @see AudioService#turnOnPlaybackThread()
     */
    PlaybackThread(@NonNull ReceivesAudio service, @NonNull String filePath, boolean autoPlay,
        @Nullable final PlaybackListener listener) {
        this.service = service;
        this.filePath = filePath;
        this.autoPlay = autoPlay;
        this.listener = listener;

        bufferSize = AudioUtils.OUT_BUFFER_SIZE;
        seekBufferSize = AudioUtils.SAMPLE_RATE * 6 * 2;
    }

    /**
     * Whether playback is in progress.
     */
    boolean isPlaying() {
        return playing;
    }

    /**
     * Whether playback is being sought.
     */
    boolean isSeeking() {
        return seeking;
    }

    /**
     * Returns position of the current byte being played.
     */
    long getProgress() {
        return progress;
    }

    /**
     * Returns length of playback in bytes.
     */
    long getLength() {
        return duration;
    }

    /**
     * Starts/resumes playback.
     */
    void play() {
        if (seeking) return;

        if (thread != null) {
            playing = true;

            LOGD(TAG, "Playback resumed");

            if (listener != null) listener.onResume();
        } else {
            // Start streaming in a thread
            done = false;
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
        if (seeking) return;

        playing = false;

        LOGD(TAG, "Playback paused");

        if (listener != null) listener.onPause();
    }

    /**
     * Stops playback and cleans up {@link InputStream} before exiting thread.
     */
    void stop() {
        thread = null;

        playing = false;
        done = true;

        LOGD(TAG, "Playback stopped");

        if (listener != null) listener.onStop();
    }

    /**
     * Seeks audio file to specified byte position.
     */
    void seek(int position) {
        if (thread == null) return;

        progress = position;

        try {
            seekToPosition(new byte[seekBufferSize]);
        } catch (IOException e) {
            Crashlytics.logException(e);
            LOGE(TAG, "Error reading random access file stream", e);
        }
    }

    /**
     * Needs to be called on seek start and seek end so that thread is aware that seeking is in progress. {@code start}
     * should be {@code true} when method is called on seek start, false otherwise.
     */
    void seek(boolean start) {
        if (thread == null) return;

        if (start && playing) pause();

        seeking = start;

        try {
            seekToPosition(new byte[seekBufferSize]);
        } catch (IOException e) {
            Crashlytics.logException(e);
            LOGE(TAG, "Error reading random access file stream", e);
        }
    }

    // Reads the audio file chunk by chunk while control flag is
    private void start() {
        try {
            raf = newRandomAccessFile();
            if (raf == null) return;

            LOGD(TAG, "RandomAccessFile created");

            duration = (int) raf.length();
            LOGD(TAG, "Audio file byte count is: " + duration);

            // setup audio track
            final AudioTrack track = AudioUtils.createAudioTrack();
            track.play();
            LOGD(TAG, "AudioTrack created");

            if (autoPlay) playing = true;

            LOGD(TAG, "Playback started");

            if (listener != null) listener.onStart(duration);

            final byte[] buffer = new byte[bufferSize];
            while (!done && raf != null) {
                if (playing) {
                    // if we are playing after seek we need to fix it because of the different buffer sizes
                    // when playing and seeking
                    if (Math.abs(raf.getFilePointer() - progress) > bufferSize) raf.seek(progress);

                    if (raf.read(buffer) < 0) { // audio playback reached end
                        // reset input stream
                        rewind();

                        LOGD(TAG, "Playback completed");

                        if (listener != null) listener.onStop();

                        continue;
                    }

                    // save progress
                    progress = (int) raf.getFilePointer();

                    synchronized (service) {
                        service.receiveAudio(ByteBuffer.wrap(buffer), progress);
                    }

                    // trigger progress listener
                    if (listener != null) listener.onProgress(progress);

                    // play audio data if we're not seeking
                    track.write(buffer, 0, buffer.length);
                } else if (seeking) {
                    seekToPosition(new byte[seekBufferSize]);
                }
            }

            // release resources
            if (raf != null) closeRaf();
            track.release();

            LOGD(TAG, "AudioTrack released");
        } catch (IOException e) {
            LOGE(TAG,
                e instanceof FileNotFoundException ? "Error loading file" : "Error reading random access file stream",
                e);
            Crashlytics.logException(e);

            stop();
        }
    }

    // Rewinds audio file.
    private void rewind() throws IOException {
        if (thread == null) return;
        if (seeking) return; // we can't rewind while seeking

        // set playing flag
        playing = false;
        // seek to file start
        if (raf != null) raf.seek(0);
        // update progress to 0 and trigger listener
        progress = 0;
        if (listener != null) listener.onProgress(progress);

        LOGD(TAG, "Audio file rewind");
    }

    // This represents a single seek loop.
    private synchronized void seekToPosition(byte[] seekBuffer) throws IOException {
        // if we don't have file we can't seek
        if (raf == null) return;

        final long zerosPrependCount = progress - seekBuffer.length;
        final long seekPosition = Math.max(0, zerosPrependCount);
        raf.seek(seekPosition);
        int readBytesCount = raf.read(seekBuffer);
        if (readBytesCount > 0) {
            if (zerosPrependCount < 0) {
                final int zerosPrependCountAbs = (int) Math.abs(zerosPrependCount);
                seekBuffer = BufferUtils.shift(seekBuffer, zerosPrependCountAbs);
            }
            service.receiveAudio(ByteBuffer.wrap(seekBuffer), progress);
        }
    }

    // Closes InputStream
    private void closeRaf() {
        try {
            raf.close();
        } catch (IOException e) {
            LOGE(TAG, "IOException while stopping random access file: " + e.toString());
            Crashlytics.logException(e);
        } finally {
            raf = null;
        }
        LOGD(TAG, "RandomAccessFile closed");
    }

    // Convenience function for creating new {@link BYBAudioFile} object from the audio file.
    @Nullable private BYBAudioFile newRandomAccessFile() throws IOException {
        final File file = new File(filePath);
        if (file.exists()) {
            return new WavAudioFile(file);
        } else {
            stop();
            LOGE(TAG, "Cant load file " + filePath + ", it doesn't exist!!");
        }

        return null;
    }
}
