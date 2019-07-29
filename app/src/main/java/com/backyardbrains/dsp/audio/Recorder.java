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

package com.backyardbrains.dsp.audio;

import android.media.AudioTrack;
import androidx.annotation.NonNull;
import android.util.Pair;
import com.backyardbrains.dsp.SignalData;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.JniUtils;
import com.backyardbrains.utils.RecordingUtils;
import com.crashlytics.android.Crashlytics;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.greenrobot.essentials.io.CircularByteBuffer;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class Recorder {

    @SuppressWarnings("WeakerAccess") static final String TAG = makeLogTag(Recorder.class);

    @SuppressWarnings("WeakerAccess") static final String EVENT_MARKERS_FILE_HEADER_CONTENT =
        "# Marker IDs can be arbitrary strings.\n# Marker ID,\tTime (in s)";

    private class WriteThread extends Thread {

        private static final int BUFFER_SIZE_IN_SEC = 1;
        private static final int BUFFER_SIZE_IN_SAMPLES = AudioUtils.DEFAULT_SAMPLE_RATE * BUFFER_SIZE_IN_SEC;
        private static final int BUFFER_SIZE_IN_BYTES = BUFFER_SIZE_IN_SAMPLES * 2;

        private final ByteBuffer bb;

        // Sample rate of the recorded file
        private int sampleRate;
        // Number of channels the recorded file should have
        private int channelCount;
        // Number of bits per sample of the recorded file
        private int bitsPerSample = AudioUtils.DEFAULT_BITS_PER_SAMPLE; // always record 16 bits per sample

        private File audioFile;
        private OutputStream outputStream;
        private File eventsFile;
        private AudioTrack audioTrack;
        private StringBuffer eventsFileContent = new StringBuffer(EVENT_MARKERS_FILE_HEADER_CONTENT);
        private List<Pair<Integer, String>> events = new ArrayList<>();
        private CircularByteBuffer buffer = new CircularByteBuffer(BUFFER_SIZE_IN_BYTES);
        private byte[] byteBuffer = new byte[BUFFER_SIZE_IN_BYTES];
        private short[] samples = new short[BUFFER_SIZE_IN_SAMPLES];

        WriteThread() {
            // crate byte buffer that will be used for converting shorts to bytes
            bb = ByteBuffer.allocate(BUFFER_SIZE_IN_BYTES).order(ByteOrder.nativeOrder());
        }

        @Override public void run() {
            try {
                while (working.get()) {
                    try {
                        int size = buffer.get(byteBuffer);
                        if (size > 0) {
                            if (recording.get()) outputStream.write(byteBuffer, 0, size);
                            if (playing.get()) audioTrack.write(byteBuffer, 0, size);
                        }
                    } catch (IOException e) {
                        throw new IllegalStateException("Could not write sample to file", e);
                    }
                }
            } catch (IllegalStateException e) {
                Crashlytics.logException(e);
            }
        }

        void startRecording(int sampleRate, int visibleChannelCount) throws IOException {
            this.sampleRate = sampleRate;
            this.channelCount = visibleChannelCount;

            // create recording file
            audioFile = RecordingUtils.createRecordingFile();
            // and stream to write samples to
            try {
                outputStream = new FileOutputStream(audioFile);
            } catch (FileNotFoundException e) {
                Crashlytics.logException(e);
                throw new IOException("Could not build OutputStream from audio file: " + audioFile.getAbsolutePath(),
                    e);
            }
            // create events file
            eventsFile = RecordingUtils.createEventsFile(audioFile);
            events.clear();
            eventsFileContent.delete(0, eventsFileContent.length());

            // start
            recording.set(true);
        }

        void stopRecording() {
            if (!recording.get()) return;

            // stop
            recording.set(false);

            // close the stream and save the recorded file
            saveFiles();
        }

        void startPlayback(int sampleRate, int channelCount, int bitsPerSample) {
            if (audioTrack != null) stopPlayback();
            // create and start audio track
            audioTrack = AudioUtils.createAudioTrack(sampleRate, channelCount, bitsPerSample);
            audioTrack.play();

            // start
            playing.set(true);
        }

        void stopPlayback() {
            if (!playing.get()) return;

            // stop
            playing.set(false);

            // stop the playback and release resources
            if (audioTrack != null) {
                audioTrack.release();
                audioTrack = null;
            }
        }

        /**
         * Appends specified {@code sampleWithEvents} to previously saved ones.
         */
        void writeData(@NonNull SignalData signalData) {
            if (working.get()) {
                int frameCount = 0;
                boolean isRecording = recording.get();

                // we need to save current recording length before writing the actual samples
                if (isRecording) {
                    frameCount = (int) AudioUtils.getFrameCount(audioFile.length(), channelCount, bitsPerSample);
                }

                // save samples to buffer as bytes
                if (isRecording || playing.get()) {
                    int sampleCount = JniUtils.interleaveSignal(samples, signalData);
                    bb.asShortBuffer().put(samples, 0, sampleCount);
                    buffer.put(bb.array(), 0, sampleCount * 2);
                }

                // save events
                if (isRecording) {
                    String event;
                    for (int i = 0; i < signalData.eventCount; i++) {
                        event = signalData.eventNames[i];
                        if (event != null) events.add(new Pair<>(frameCount + signalData.eventIndices[i], event));
                    }
                }
            }
        }

        /**
         * Returns current length of the recorded file.
         *
         * @return Length of the recorded file in bytes.
         */
        long getCurrentLength() {
            return audioFile != null ? audioFile.length() : 0;
        }

        // Closes the audio stream and saves the audio file to storage
        private void saveFiles() {
            try {
                if (outputStream != null) {
                    outputStream.flush();
                    outputStream.close();
                }
                if (audioFile != null) WavAudioFile.save(audioFile, sampleRate, channelCount);

                if (events.size() > 0) saveEventFile();
            } catch (IOException e) {
                Crashlytics.logException(e);
            }
        }

        // Populates and saves file with all the events
        private void saveEventFile() throws IOException {
            // construct the events file content
            int len = events.size();
            Pair<Integer, String> event;
            for (int i = 0; i < len; i++) {
                event = events.get(i);
                eventsFileContent.append("\n")
                    .append(event.second)
                    .append(",\t")
                    .append(event.first / (float) sampleRate);
            }
            // there needs to be a RETURN char at the end of the events file
            // for the desktop app to be able to parse it properly
            eventsFileContent.append("\n");

            LOGD(TAG, eventsFileContent.toString());

            final FileOutputStream outputStream;
            try {
                outputStream = new FileOutputStream(eventsFile);
                outputStream.write(eventsFileContent.toString().getBytes());
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
                Crashlytics.logException(e);
                throw new IOException("could not build OutputStream from events file: " + audioFile.getAbsolutePath(),
                    e);
            }
        }
    }

    // Audio recording thread
    @SuppressWarnings("WeakerAccess") WriteThread writeThread;
    // Flag that indicates whether writing thread should be running
    @SuppressWarnings("WeakerAccess") final AtomicBoolean working = new AtomicBoolean(true);
    // Flag that indicates whether signal should be recorder
    @SuppressWarnings("WeakerAccess") final AtomicBoolean recording = new AtomicBoolean(false);
    // Flag that indicates whether signal should be played back
    @SuppressWarnings("WeakerAccess") final AtomicBoolean playing = new AtomicBoolean(false);

    public Recorder() {
        // start sample writing thread
        writeThread = new WriteThread();
        writeThread.start();
    }

    /**
     * Returns whether signals is currently being recorded.
     */
    public boolean isRecording() {
        return recording.get();
    }

    /**
     * Starts recording incoming signal.
     *
     * @throws IOException
     */
    public void startRecording(int sampleRate, int visibleChannelCount) throws IOException {
        writeThread.startRecording(sampleRate, visibleChannelCount);
    }

    /**
     * Stops recording incoming signal.
     */
    public void stopRecording() {
        writeThread.stopRecording();
    }

    /**
     * Returns whether signal is currently being played back.
     */
    public boolean isPlaying() {
        return playing.get();
    }

    /**
     * Starts playing back incoming signal.
     */
    public void startPlaying(int sampleRate, int channelCount, int bitsPerSample) {
        writeThread.startPlayback(sampleRate, channelCount, bitsPerSample);
    }

    /**
     * Stops playing back incoming signal.
     */
    public void stopPlaying() {
        writeThread.stopPlayback();
    }

    /**
     * Writes specified {@code sampleWithEvents} to the audio stream.
     */
    public void write(@NonNull SignalData signalData) {
        if (writeThread != null) writeThread.writeData(signalData);
    }

    /**
     * Returns currently recorder length.
     */
    public long getAudioLength() {
        return writeThread != null ? writeThread.getCurrentLength() : 0;
    }

    /**
     * Requests the recording to stop.
     */
    public void requestStop() {
        writeThread.stopRecording();
        writeThread.stopPlayback();
        working.set(false);
        writeThread = null;
    }
}
