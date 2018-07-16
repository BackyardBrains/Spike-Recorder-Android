package com.backyardbrains.audio;

import android.media.AudioTrack;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import com.backyardbrains.data.processing.SamplesWithEvents;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.Benchmark;
import com.backyardbrains.utils.BufferUtils;
import com.backyardbrains.utils.EventUtils;
import com.crashlytics.android.Crashlytics;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.LOGE;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class PlaybackSampleSource extends AbstractAudioSampleSource {

    @SuppressWarnings("WeakerAccess") static final String TAG = makeLogTag(PlaybackSampleSource.class);

    // Path to the audio file
    private final String filePath;
    // Whether file should start playing right away
    private final boolean autoPlay;

    // Audio playback thread
    private PlaybackThread playbackThread;

    // Used for converting read bytes to samples
    @SuppressWarnings("WeakerAccess") ByteBuffer byteBuffer;
    // Updated during playback and returned to processing thread on every cycle
    @SuppressWarnings("WeakerAccess") SamplesWithEvents samplesWithEvents;
    // Used for passing samples to samplesWithEvents
    @SuppressWarnings("WeakerAccess") short[] samples;
    // Used for passing event indices to samplesWithEvents
    private int[] eventIndices = new int[EventUtils.MAX_EVENT_COUNT];
    // Used for passing event names to samplesWithEvents
    private String[] eventNames = new String[EventUtils.MAX_EVENT_COUNT];

    // Collection of events within currently processed data batch
    @SuppressWarnings("WeakerAccess") SparseArray<String> eventsInCurrentBatch =
        new SparseArray<>(EventUtils.MAX_EVENT_COUNT);

    /**
     * Thread used for playing the audio file.
     */
    protected class PlaybackThread extends Thread {

        // Path to the audio file
        private final String filePath;
        // Whether file should start playing right away
        private final boolean autoPlay;
        // Size of buffer (chunk) to read when seeking (6 seconds)
        private int bufferSize;
        // Buffer that holds audio data while seeking
        private byte[] buffer;
        // Holds all events saved for the played file
        private SparseArray<String> allEvents;

        // Random access file stream that holds audio file that's being played
        private BYBAudioFile raf;
        // True if audio is currently being played, false if it's paused or stopped
        private AtomicBoolean playing = new AtomicBoolean(false);
        // Whether audio is currently being sought.
        private AtomicBoolean seeking = new AtomicBoolean(false);
        // Flag that indicates whether thread should be running
        private AtomicBoolean working = new AtomicBoolean(true);
        // Position of the playback head
        private AtomicLong progress = new AtomicLong();
        // Length of the audio file in bytes
        private AtomicLong duration = new AtomicLong();

        PlaybackThread(@NonNull String filePath, boolean autoPlay) {
            this.filePath = filePath;
            this.autoPlay = autoPlay;
        }

        @Override public void run() {
            try {
                raf = newRandomAccessFile();
                if (raf == null) return;

                LOGD(TAG, "RandomAccessFile created");

                allEvents = EventUtils.parseEvents(filePath, raf.sampleRate());

                duration.set(raf.length());
                LOGD(TAG, "Audio file byte count is: " + duration.get());

                setSampleRate(raf.sampleRate());
                LOGD(TAG, "Audio file sample rate is is: " + getSampleRate());

                // setup audio track
                final AudioTrack track = AudioUtils.createAudioTrack(raf.sampleRate());
                track.play();
                LOGD(TAG, "AudioTrack created");

                if (autoPlay) playing.set(true);

                // number of bytes that should be read during playback
                int bytesToReadWhilePlaying = AudioUtils.getOutBufferSize(raf.sampleRate());
                // number of bytes actually read during single read
                int bytesRead;

                // set size of the buffer for seeking we need full buffer of 6 seconds (in bytes)
                bufferSize = (int) (raf.sampleRate() * Math.min(6, raf.length()) * 2);
                byteBuffer = ByteBuffer.allocate(bufferSize).order(ByteOrder.LITTLE_ENDIAN);
                buffer = new byte[bufferSize];
                samples = new short[(int) (bufferSize * .5)];
                samplesWithEvents = new SamplesWithEvents(samples.length);

                LOGD(TAG, "Playback started");

                // inform any interested parties that playback has started
                if (playbackListener != null) playbackListener.onStart(duration.get(), raf.sampleRate());

                while (working.get() && raf != null) {
                    if (playing.get()) {
                        // if we are playing after seek we need to fix it because of the different buffer sizes
                        // when playing and seeking
                        if (Math.abs(raf.getFilePointer() - progress.get()) > bytesToReadWhilePlaying) {
                            raf.seek(progress.get());
                        }

                        // index of the sample from which we check the events
                        long startSampleIndex = AudioUtils.getSampleCount(raf.getFilePointer());

                        if ((bytesRead = raf.read(buffer, 0, bytesToReadWhilePlaying)) < 0) {
                            // audio playback reached end

                            // reset input stream
                            rewind();

                            writeToBuffer(new byte[bufferSize], 0, bufferSize, 0);

                            LOGD(TAG, "Playback completed");

                            if (playbackListener != null) playbackListener.onStop();

                            continue;
                        }

                        // save progress
                        progress.set(raf.getFilePointer());

                        // index of the sample up to which we check the events
                        long endSampleIndex = AudioUtils.getSampleCount(progress.get());

                        // check if there are any events in the currently read buffer
                        int len = allEvents.size();
                        eventsInCurrentBatch.clear();
                        for (int i = 0; i < len; i++) {
                            int sampleIndex = allEvents.keyAt(i);
                            if (startSampleIndex <= sampleIndex && sampleIndex < endSampleIndex) {
                                eventsInCurrentBatch.put((int) (sampleIndex - startSampleIndex), allEvents.valueAt(i));
                            }
                        }

                        // write data to buffer
                        writeToBuffer(buffer, 0, bytesRead, progress.get());

                        // trigger progress listener
                        if (playbackListener != null) playbackListener.onProgress(progress.get(), raf.sampleRate());

                        // play audio data if we're not seeking
                        track.write(buffer, 0, bytesRead);
                    } else if (seeking.get()) {
                        seekToPosition();
                    }
                }

                // release resources
                if (raf != null) closeRaf();
                track.release();

                LOGD(TAG, "AudioTrack released");
            } catch (IOException e) {
                LOGE(TAG, e instanceof FileNotFoundException ? "Error loading file"
                    : "Error reading random access file stream", e);
                Crashlytics.logException(e);

                onInputStop(); //stop();
            }
        }

        long getProgress() {
            return progress.get();
        }

        public void setProgress(int progress) {
            this.progress.set(progress);
        }

        long getDuration() {
            return duration.get();
        }

        void seek(boolean start) {
            seeking.set(start);
        }

        boolean isSeeking() {
            return seeking.get();
        }

        void playback(boolean start) {
            playing.set(start);
        }

        boolean isPlaying() {
            return playing.get();
        }

        void stopWorking() {
            seeking.set(false);
            playing.set(false);
            working.set(false);
        }

        // This represents a single seek loop.
        synchronized void seekToPosition() throws IOException {
            // if we don't have file we can't seek
            if (raf == null) return;

            final long zerosPrependCount = progress.get() - bufferSize;
            final long seekPosition = Math.max(0, zerosPrependCount);
            raf.seek(seekPosition);

            // index of the sample from which we check the events
            long startSampleIndex = AudioUtils.getSampleCount(raf.getFilePointer());

            if (raf.read(buffer) > 0) {
                int sampleIndexPrepend = 0;
                if (zerosPrependCount < 0) {
                    final int zerosPrependCountAbs = (int) Math.abs(zerosPrependCount);
                    BufferUtils.shiftRight(buffer, zerosPrependCountAbs);

                    sampleIndexPrepend = (int) (zerosPrependCountAbs * .5);
                }

                // index of the sample up to which we check the events
                long endSampleIndex = AudioUtils.getSampleCount(raf.getFilePointer());

                // check if there are any events in the currently read buffer
                int len = allEvents.size();
                eventsInCurrentBatch.clear();
                for (int i = 0; i < len; i++) {
                    int sampleIndex = allEvents.keyAt(i) + sampleIndexPrepend;
                    if (startSampleIndex <= sampleIndex && sampleIndex < endSampleIndex) {
                        eventsInCurrentBatch.put((int) (sampleIndex - startSampleIndex), allEvents.valueAt(i));
                    }
                }

                // update buffer capacity when switching from playing to seeking
                setProcessingBufferSize(bufferSize);
                // write data to buffer
                writeToBuffer(buffer, 0, buffer.length, Math.min(0, zerosPrependCount) + raf.getFilePointer());
            }
        }

        // Rewinds audio file.
        private void rewind() throws IOException {
            if (seeking.get()) return; // we can't rewind while seeking

            // set playing flag
            playing.set(false);
            // seek to file start
            if (raf != null) raf.seek(0);
            // update progress to 0 and trigger listener
            progress.set(0);
            if (playbackListener != null) {
                playbackListener.onProgress(progress.get(), raf != null ? raf.sampleRate() : 0);
            }

            LOGD(TAG, "Audio file rewind");
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
                onInputStop(); //stop();
                LOGE(TAG, "Cant load file " + filePath + ", it doesn't exist!!");
            }

            return null;
        }
    }

    /**
     * Listener for handling different audio file playback events.
     */
    interface PlaybackListener {

        /**
         * Triggered on playback start.
         *
         * @param length Length of the playback in bytes.
         * @param sampleRate Sample rate of the played file.
         */
        void onStart(long length, int sampleRate);

        /**
         * Triggered when playback resumes after pause.
         *
         * @param sampleRate Sample rate of the played file.
         */
        void onResume(int sampleRate);

        /**
         * Triggered constantly during playback progress.
         *
         * @param progress Current byte being played
         * @param sampleRate Sample rate of the played file.
         */
        void onProgress(long progress, int sampleRate);

        /**
         * Triggered when playback pauses.
         */
        void onPause();

        /**
         * Triggered on playback stop.
         */
        void onStop();
    }

    @SuppressWarnings("WeakerAccess") PlaybackListener playbackListener;

    PlaybackSampleSource(@NonNull String filePath, boolean autoPlay, @Nullable OnSamplesReceivedListener listener) {
        super(listener);
        this.filePath = filePath;
        this.autoPlay = autoPlay;
    }

    /**
     * Whether playback is in progress.
     */
    boolean isPlaying() {
        return playbackThread != null && playbackThread.isPlaying();
    }

    /**
     * Whether playback is being sought.
     */
    boolean isSeeking() {
        return playbackThread != null && playbackThread.isSeeking();
    }

    /**
     * Returns length of playback in bytes.
     */
    long getLength() {
        return playbackThread != null ? playbackThread.getDuration() : 0;
    }

    /**
     * Pauses playback.
     */
    void pausePlayback() {
        if (playbackThread != null) {
            if (playbackThread.isSeeking()) return;

            playbackThread.playback(false);

            LOGD(TAG, "Playback paused");

            if (playbackListener != null) playbackListener.onPause();
        }
    }

    /**
     * Resumes playback.
     */
    void resumePlayback() {
        if (playbackThread != null) {
            if (playbackThread.isSeeking()) return;

            playbackThread.playback(true);

            LOGD(TAG, "Playback resumed");

            if (playbackListener != null) playbackListener.onResume(getSampleRate());
        }
    }

    /**
     * Seeks audio file to specified byte position.
     */
    void seek(int position) {
        if (playbackThread != null) {
            playbackThread.setProgress(position);

            try {
                playbackThread.seekToPosition();
            } catch (IOException e) {
                Crashlytics.logException(e);
                LOGE(TAG, "Error reading random access file stream", e);
            }
        }
    }

    /**
     * Needs to be called on seek start and seek end so that thread is aware that seeking is in progress. {@code start}
     * should be {@code true} when method is called on seek start, false otherwise.
     */
    void seek(boolean start) {
        if (playbackThread != null) {
            if (start && playbackThread.isPlaying()) pausePlayback();

            playbackThread.seek(start);

            try {
                playbackThread.seekToPosition();
            } catch (IOException e) {
                Crashlytics.logException(e);
                LOGE(TAG, "Error reading random access file stream", e);
            }
        }
    }

    /**
     * Registers a callback to be invoked when different audio file playback events occur.
     *
     * @param listener The callback that will be run. This value may be {@code null}.
     */
    void setOnPlaybackListener(@Nullable PlaybackListener listener) {
        this.playbackListener = listener;
    }

    @Override protected void onInputStart() {
        if (playbackThread == null) {
            // Start playback in a thread
            playbackThread = new PlaybackThread(filePath, autoPlay);
            playbackThread.start();
        }
    }

    @Override protected void onInputStop() {
        if (playbackThread != null) {
            playbackThread.stopWorking();
            playbackThread = null;

            LOGD(TAG, "Playback stopped");

            if (playbackListener != null) playbackListener.onStop();
        }
    }

    private final Benchmark benchmark = new Benchmark("PLAYBACK_TEST").warmUp(200)
        .sessions(10)
        .measuresPerSession(200)
        .logBySession(false)
        .logToFile(false)
        .listener(new Benchmark.OnBenchmarkListener() {
            @Override public void onEnd() {
                //EventBus.getDefault().post(new ShowToastEvent("PRESS BACK BUTTON!!!!"));
            }
        });

    @NonNull @Override protected SamplesWithEvents processIncomingData(byte[] data, int length, long lastByteIndex) {
        //benchmark.start();

        int sampleCount = (int) (length * .5);
        byteBuffer.put(data, 0, length);
        byteBuffer.clear();
        byteBuffer.asShortBuffer().get(samples, 0, sampleCount);

        int eventCount = eventsInCurrentBatch.size();
        for (int i = 0; i < eventCount; i++) {
            eventIndices[i] = eventsInCurrentBatch.keyAt(i);
            eventNames[i] = eventsInCurrentBatch.valueAt(i);
        }

        samplesWithEvents.setAll(samples, sampleCount, eventIndices, eventNames, eventCount,
            AudioUtils.getSampleCount(lastByteIndex));

        //benchmark.end();

        return samplesWithEvents;
    }
}
