package com.backyardbrains.dsp.audio;

import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Build;
import android.util.Pair;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.backyardbrains.dsp.AbstractSignalSource;
import com.backyardbrains.dsp.SignalData;
import com.backyardbrains.dsp.SignalProcessor;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.BufferUtils;
import com.backyardbrains.utils.EventUtils;
import com.backyardbrains.utils.JniUtils;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.LOGE;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class PlaybackSignalSource extends AbstractSignalSource {

    @SuppressWarnings("WeakerAccess") static final String TAG =
        makeLogTag(PlaybackSignalSource.class);

    // Lock used when reading samples and events
    @SuppressWarnings("WeakerAccess") static final Object lock = new Object();

    /**
     * Thread used for reading the audio file.
     */
    protected class ReadThread extends Thread {

        // Path to the audio file
        private final String filePath;
        // Whether file should start playing right away
        private final boolean autoPlay;
        // Position of the recording from which playback should start
        private final int position;
        // Size of buffer (chunk) to read when seeking (6 seconds)
        private int bufferSize;
        // Buffer that holds audio data
        private byte[] buffer;

        // Random access file stream that holds audio file that's being played
        private AudioFile raf;

        ReadThread(@NonNull String filePath, boolean autoPlay, int position) {
            this.filePath = filePath;
            this.autoPlay = autoPlay;
            this.position = position;
        }

        @Override public void run() {
            try {
                raf = newRandomAccessFile();
                if (raf == null) return;
                // Android version below Lollipop cannot read 32bit files
                if (raf.bitsPerSample() == 32
                    && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    return;
                }

                LOGD(TAG, "RandomAccessFile created");

                // get all events from file and populate arrays with event indices and event names
                allEvents = EventUtils.parseEvents(filePath, raf.sampleRate());
                int len = allEvents.size();
                eventIndices = new int[len];
                eventNames = new String[len];
                Pair<Integer, String> event;
                for (int i = 0; i < len; i++) {
                    event = allEvents.get(i);
                    eventIndices[i] = event.first;
                    eventNames[i] = event.second;
                }

                duration.set(raf.length());
                LOGD(TAG, "Audio file byte count is: " + duration.get());

                progress.set(position);
                LOGD(TAG, "Audio file will start at position: " + progress.get());

                setSampleRate(raf.sampleRate());
                LOGD(TAG, "Audio file sample rate is: " + raf.sampleRate());
                setChannelCount(raf.channelCount());
                LOGD(TAG, "Audio file channel count is: " + raf.channelCount());
                setBitsPerSample(raf.bitsPerSample());
                LOGD(TAG, "Audio file bits/sample is: " + raf.bitsPerSample());

                // setup audio track
                AudioTrack track = AudioUtils.createAudioTrack(raf.sampleRate(), raf.channelCount(),
                    raf.bitsPerSample());
                track.play();
                LOGD(TAG, "AudioTrack created");

                if (autoPlay) playing.set(true);

                // number of bytes that should be read during playback
                final int bytesToReadWhilePlaying =
                    AudioUtils.getOutBufferSize(raf.sampleRate(), raf.channelCount(),
                        raf.bitsPerSample());
                // ByteBuffer that will be used if encoding is AudioFormat.ENCODING_PCM_FLOAT
                final ByteBuffer pcmFloatByteBuffer =
                    ByteBuffer.allocateDirect(bytesToReadWhilePlaying);
                // number of float that should be rad during playback if encoding is AudioFormat.ENCODING_PCM_FLOAT
                final int floatsToReadWhilePlaying = (int) (bytesToReadWhilePlaying * .25f);
                // buffer that will be used for transferring float if encoding is AudioFormat.ENCODING_PCM_FLOAT
                final float[] pcmFloatBuffer = new float[floatsToReadWhilePlaying];
                // number of bytes actually read during single read
                int read;

                // set size of the buffer for seeking
                bufferSize =
                    (raf.bitsPerSample() * SignalProcessor.getProcessedSamplesPerChannelCount()
                        * raf.channelCount()) / 8;
                buffer = new byte[bufferSize];

                LOGD(TAG, "Processing buffer size is: " + bufferSize);

                LOGD(TAG, "Playback started");

                // inform any interested parties that playback has started
                if (playbackListener != null && position == 0) {
                    playbackListener.onStart(duration.get(), raf.sampleRate(), raf.channelCount(),
                        raf.bitsPerSample());
                }

                while (working.get() && raf != null) {
                    if (playing.get()) {
                        synchronized (lock) {
                            // if we are playing after seek we need to fix position
                            if (Math.abs(raf.getFilePointer() - progress.get())
                                > bytesToReadWhilePlaying) {
                                raf.seek(progress.get());
                            }

                            // index of the sample from which we check the events
                            fromSample.set(
                                AudioUtils.getFrameCount(raf.getFilePointer(), raf.channelCount(),
                                    getBitsPerSample()));

                            // number of samples to prepend
                            samplesToPrepend.set(0);

                            // check if audio playback reached end
                            if ((read = raf.read(buffer, 0, bytesToReadWhilePlaying)) < 0) {
                                // set playing flag
                                playing.set(false);

                                LOGD(TAG, "Playback completed");

                                if (playbackListener != null) playbackListener.onStop();

                                continue;
                            }

                            // save progress
                            progress.set(raf.getFilePointer());

                            // index of the sample up to which we check the events
                            toSample.set(
                                AudioUtils.getFrameCount(progress.get(), raf.channelCount(),
                                    raf.bitsPerSample()));

                            // write data to buffer
                            writeToBuffer(buffer, read);

                            // trigger progress listener
                            if (playbackListener != null) {
                                playbackListener.onProgress(progress.get(), raf.sampleRate(),
                                    raf.channelCount(), raf.bitsPerSample());
                            }

                            if (track.getAudioFormat() == AudioFormat.ENCODING_PCM_FLOAT) {
                                pcmFloatByteBuffer.put(buffer, 0, read);
                                pcmFloatByteBuffer.clear();
                                pcmFloatByteBuffer.asFloatBuffer()
                                    .get(pcmFloatBuffer, 0, floatsToReadWhilePlaying);
                                // play audio data if we're not seeking
                                track.write(pcmFloatBuffer, 0, floatsToReadWhilePlaying,
                                    AudioTrack.WRITE_BLOCKING);
                            } else {
                                track.write(buffer, 0, read);
                            }
                        }
                    }
                }

                // release resources
                if (raf != null) closeRaf();
                track.release();

                LOGD(TAG, "AudioTrack released");
            } catch (IOException e) {
                LOGE(TAG, e instanceof FileNotFoundException ? "Error loading file"
                    : "Error reading random access file stream", e);
                FirebaseCrashlytics.getInstance().recordException(e);

                PlaybackSignalSource.this.stop();
            }
        }

        /**
         * This represents a single seek loop.
         *
         * @throws IOException
         */
        synchronized void seekToPosition() throws IOException {
            // if we don't have file we can't seek
            if (raf == null) return;

            synchronized (lock) {
                final long zerosPrependCount = progress.get() - bufferSize;
                final long seekPosition = Math.max(0, zerosPrependCount);
                // fix seek position so that it's positioned at the begining of the frame
                raf.seek(seekPosition);

                // index of the sample from which we check the events
                fromSample.set(AudioUtils.getFrameCount(raf.getFilePointer(), raf.channelCount(),
                    raf.bitsPerSample()));

                // buffer needs to be initialized
                if (buffer == null) return;

                // number of bytes actually read during single read
                if (raf.read(buffer) > 0) {
                    if (zerosPrependCount < 0) {
                        BufferUtils.shiftRight(buffer, (int) Math.abs(zerosPrependCount));
                    }

                    // number of samples to prepend
                    samplesToPrepend.set(
                        (int) AudioUtils.getFrameCount(zerosPrependCount, raf.channelCount(),
                            raf.bitsPerSample()));

                    // index of the sample up to which we check the events
                    long toByte = raf.getFilePointer();
                    if (bufferSize > toByte) toByte = bufferSize;
                    toSample.set(
                        AudioUtils.getFrameCount(toByte, raf.channelCount(), raf.bitsPerSample()));

                    // write data to buffer
                    writeToBuffer(buffer, bufferSize);
                }
            }
        }

        /**
         * Reads last {@code len} number of bytes from the currently played file and copies them to the specified
         * {@code buffer}. Current position of the file pointer is not changed after this method finishes. If there are
         * less then requested amount of bytes zeros will be prepended to the specified {@code buffer}.
         *
         * @throws IOException
         */
        synchronized void readLast(byte[] buffer, int len) throws IOException {
            synchronized (lock) {
                // save current position before copying the data cause we'll need to move through file
                final long currentPos = raf.getFilePointer();

                // this call will fill the fully buffer with bytes up to current position
                seekToPosition();
                // copy data to the provided buffer
                System.arraycopy(this.buffer, 0, buffer, 0, len);

                // get back to the current position so playback (if playing) can continue normally
                raf.seek(currentPos);
            }
        }

        /**
         * Rewinds audio file.
         */
        synchronized void rewind() {
            synchronized (lock) {
                if (seeking.get()) return; // we can't rewind while seeking

                try {
                    if (raf != null) raf.seek(0);
                } catch (IOException e) {
                    LOGE(TAG, "IOException while rewinding: " + e.toString());
                    FirebaseCrashlytics.getInstance().recordException(e);
                }
                // update progress to 0 and trigger listener
                progress.set(0);
                // update from and to sample to start values
                fromSample.set(0);
                toSample.set(0);
                samplesToPrepend.set(0);

                BufferUtils.emptyBuffer(buffer);
                writeToBuffer(buffer, bufferSize);

                LOGD(TAG, "Audio file rewind");
            }
        }

        // Closes InputStream
        private void closeRaf() {
            try {
                raf.close();
            } catch (IOException e) {
                LOGE(TAG, "IOException while stopping random access file: " + e.toString());
                FirebaseCrashlytics.getInstance().recordException(e);
            } finally {
                raf = null;
            }
            LOGD(TAG, "RandomAccessFile closed");
        }

        // Convenience function for creating new {@link AudioFile} object from the audio file.
        @Nullable private AudioFile newRandomAccessFile() throws IOException {
            final File file = new File(filePath);
            if (file.exists()) {
                return BaseAudioFile.create(file);
            } else {
                PlaybackSignalSource.this.stop();
                LOGE(TAG, "Cant load file " + filePath + ", it doesn't exist!!");
            }

            return null;
        }
    }

    /**
     * Listener for handling different audio file playback events.
     */
    public interface PlaybackListener {

        /**
         * Triggered on playback start.
         *
         * @param length Length of the playback in bytes.
         * @param sampleRate Sample rate of the played file.
         * @param channelCount Number of channels of the played file.
         * @param bitsPerSample Number of bits per sample of the played file.
         */
        void onStart(long length, int sampleRate, int channelCount, int bitsPerSample);

        /**
         * Triggered when playback resumes after pause.
         *
         * @param sampleRate Sample rate of the played file.
         * @param channelCount Number of channels of the played file.
         * @param bitsPerSample Number of bits per sample of the played file.
         */
        void onResume(int sampleRate, int channelCount, int bitsPerSample);

        /**
         * Triggered constantly during playback progress.
         *
         * @param progress Current byte being played
         * @param sampleRate Sample rate of the played file.
         * @param channelCount Number of channels of the played file.
         * @param bitsPerSample Number of bits per sample of the played file.
         */
        void onProgress(long progress, int sampleRate, int channelCount, int bitsPerSample);

        /**
         * Triggered when playback pauses.
         */
        void onPause();

        /**
         * Triggered on playback stop.
         */
        void onStop();
    }

    // Path to the audio file
    private final String filePath;
    // Whether file should start playing right away
    private final boolean autoPlay;
    // Initial position of the recording
    private final int position;

    @SuppressWarnings("WeakerAccess") PlaybackListener playbackListener;

    // Audio playback thread
    private ReadThread playbackThread;
    // Flag that indicates whether thread should be running
    @SuppressWarnings("WeakerAccess") AtomicBoolean working = new AtomicBoolean(true);
    // True if audio is currently being played, false if it's paused or stopped
    @SuppressWarnings("WeakerAccess") AtomicBoolean playing = new AtomicBoolean(false);
    // Whether audio is currently being sought.
    @SuppressWarnings("WeakerAccess") AtomicBoolean seeking = new AtomicBoolean(false);
    // Position of the playback head
    @SuppressWarnings("WeakerAccess") AtomicLong progress = new AtomicLong();
    // Length of the audio file in bytes
    @SuppressWarnings("WeakerAccess") AtomicLong duration = new AtomicLong();
    // Index of the first sample being played back
    @SuppressWarnings("WeakerAccess") AtomicLong fromSample = new AtomicLong();
    // Index of the last sample being played back
    @SuppressWarnings("WeakerAccess") AtomicLong toSample = new AtomicLong();
    // Number of samples that should be prepended while playing begining of the audio file
    @SuppressWarnings("WeakerAccess") AtomicInteger samplesToPrepend = new AtomicInteger();
    // Holds all events saved for the played file
    @SuppressWarnings("WeakerAccess") List<Pair<Integer, String>> allEvents;

    // Used for passing event indices to samplesWithEvents
    @SuppressWarnings("WeakerAccess") int[] eventIndices;
    // Used for passing event names to samplesWithEvents
    @SuppressWarnings("WeakerAccess") String[] eventNames;

    public PlaybackSignalSource(@NonNull String filePath, boolean autoPlay, int position) {
        super(AudioUtils.DEFAULT_SAMPLE_RATE, AudioUtils.DEFAULT_CHANNEL_COUNT,
            AudioUtils.DEFAULT_BITS_PER_SAMPLE);

        this.filePath = filePath;
        this.autoPlay = autoPlay;
        this.position = position;
    }

    /**
     * Whether playback is in progress.
     */
    public boolean isPlaying() {
        return playing.get();
    }

    /**
     * Whether playback is being sought.
     */
    public boolean isSeeking() {
        return seeking.get();
    }

    /**
     * Returns length of playback in bytes.
     */
    public long getLength() {
        return duration.get();
    }

    /**
     * Pauses playback.
     */
    public void pausePlayback() {
        if (playbackThread != null) {
            if (seeking.get()) return;

            playing.set(false);

            LOGD(TAG, "Playback paused");

            if (playbackListener != null) playbackListener.onPause();
        }
    }

    /**
     * Resumes playback.
     */
    public void resumePlayback() {
        if (playbackThread != null) {
            if (seeking.get()) return;

            // reset input stream
            if (progress.get() == duration.get()) playbackThread.rewind();

            playing.set(true);

            LOGD(TAG, "Playback resumed");

            if (playbackListener != null) {
                playbackListener.onResume(getSampleRate(), getChannelCount(), getBitsPerSample());
            }
        }
    }

    /**
     * Seeks audio file to spec
     * ified byte position.
     */
    public void seek(int position) {
        if (playbackThread != null) {
            progress.set(position);

            try {
                playbackThread.seekToPosition();
            } catch (IOException e) {
                FirebaseCrashlytics.getInstance().recordException(e);
                LOGE(TAG, "Error reading random access file stream", e);
            }
        }
    }

    /**
     * Needs to be called on seek start and seek end so that thread is aware that seeking is in progress. {@code start}
     * should be {@code true} when method is called on seek start, false otherwise.
     */
    public void seek(boolean start) {
        if (playbackThread != null) {
            if (start && playing.get()) pausePlayback();

            seeking.set(start);
        }
    }

    /**
     * Copies last {@code len} bytes of the currently played file to the buffer.
     */
    public void readLast(byte[] buffer, int len) {
        if (playbackThread != null) {
            try {
                playbackThread.readLast(buffer, len);
            } catch (IOException e) {
                FirebaseCrashlytics.getInstance().recordException(e);
                LOGE(TAG, "Error reading random access file stream", e);
            }
        }
    }

    /**
     * Registers a callback to be invoked when different audio file playback events occur.
     *
     * @param listener The callback that will be run. This value may be {@code null}.
     */
    public void setPlaybackListener(@Nullable PlaybackListener listener) {
        this.playbackListener = listener;
    }

    // Stops the audio playback
    private void stopPlayback() {
        seeking.set(false);
        playing.set(false);
        working.set(false);
        fromSample.set(0);
        toSample.set(0);
        samplesToPrepend.set(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override public void start() {
        if (playbackThread == null) {
            // Start playback in a thread
            playbackThread = new ReadThread(filePath, autoPlay, position);
            playbackThread.start();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override public void stop() {
        if (playbackThread != null) {
            stopPlayback();
            playbackThread = null;

            LOGD(TAG, "Playback stopped");

            if (playbackListener != null) playbackListener.onStop();
        }
    }

    //private final Benchmark benchmark = new Benchmark("PLAYBACK_TEST").warmUp(200)
    //    .sessions(10)
    //    .measuresPerSession(200)
    //    .logBySession(false)
    //    .listener(() -> {
    //        //EventBus.getDefault().post(new ShowToastEvent("PRESS BACK BUTTON!!!!"));
    //    });

    @Override
    public void processIncomingData(@NonNull SignalData outData, byte[] inData, int inDataLength) {
        //benchmark.start();
        JniUtils.processPlaybackStream(outData, inData, inDataLength, eventIndices, eventNames,
            eventIndices.length, fromSample.get(), toSample.get(), samplesToPrepend.get());
        //benchmark.end();
    }

    @Override public int getType() {
        return Type.FILE;
    }
}
