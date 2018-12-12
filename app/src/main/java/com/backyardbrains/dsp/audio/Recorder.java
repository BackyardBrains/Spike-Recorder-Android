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

import android.support.annotation.NonNull;
import android.util.SparseArray;
import com.backyardbrains.dsp.SamplesWithEvents;
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

        // Sample rate of the recorded file
        private final int sampleRate;
        // Number of channels the recorded file should have
        private final int channelCount;

        private final File audioFile;
        private final OutputStream outputStream;
        private final File eventsFile;
        private final ByteBuffer bb;

        private StringBuffer eventsFileContent = new StringBuffer(EVENT_MARKERS_FILE_HEADER_CONTENT);
        private SparseArray<String> events = new SparseArray<>();
        private CircularByteBuffer buffer = new CircularByteBuffer(BUFFER_SIZE_IN_BYTES);
        private byte[] byteBuffer = new byte[BUFFER_SIZE_IN_BYTES];
        private short[] samples = new short[BUFFER_SIZE_IN_SAMPLES];

        WriteThread(int sampleRate, int channelCount) throws IOException {
            this.sampleRate = sampleRate;
            this.channelCount = channelCount;

            // create recording file
            audioFile = RecordingUtils.createRecordingFile();
            // and stream to write sample to
            try {
                outputStream = new FileOutputStream(audioFile);
            } catch (FileNotFoundException e) {
                Crashlytics.logException(e);
                throw new IOException("Could not build OutputStream from audio file: " + audioFile.getAbsolutePath(),
                    e);
            }

            // crate byte buffer that will be used for converting shorts to bytes
            bb = ByteBuffer.allocate(BUFFER_SIZE_IN_BYTES).order(ByteOrder.nativeOrder());

            // create events file
            eventsFile = RecordingUtils.createEventsFile(audioFile);
        }

        @Override public void run() {
            try {
                while (working.get()) {
                    try {
                        int size = buffer.get(byteBuffer);
                        if (size > 0) outputStream.write(byteBuffer, 0, size);
                    } catch (IOException e) {
                        throw new IllegalStateException("Could not write sample to file", e);
                    }
                }
            } catch (IllegalStateException e) {
                Crashlytics.logException(e);
            } finally {
                // close the stream and save the recorded file
                saveFiles();
            }
        }

        /**
         * Appends specified {@code sampleWithEvents} to previously saved ones.
         */
        void writeData(@NonNull SamplesWithEvents samplesWithEvents) {
            if (working.get()) {
                // we need to save current recording length before writing the actual samples
                int writtenSamples = (int) AudioUtils.getSampleCount(audioFile.length());

                // save samples to buffer as bytes
                int sampleCount = JniUtils.interleaveSignal(samples, samplesWithEvents);
                bb.asShortBuffer().put(samples, 0, sampleCount);
                buffer.put(bb.array(), 0, sampleCount * 2);

                // save events
                String event;
                for (int i = 0; i < samplesWithEvents.eventCount; i++) {
                    event = samplesWithEvents.eventNames[i];
                    if (event != null) events.put(writtenSamples + samplesWithEvents.eventIndices[i], event);
                }
            }
        }

        /**
         * Returns current length of the recorded file.
         *
         * @return Length of the recorded file in bytes.
         */
        long getCurrentLength() {
            return audioFile.length();
        }

        // Closes the audio stream and saves the audio file to storage
        private void saveFiles() {
            try {
                outputStream.flush();
                outputStream.close();
                WavAudioFile.save(audioFile, sampleRate, channelCount);

                if (events.size() > 0) saveEventFile();

                writeThread = null;
            } catch (IOException e) {
                Crashlytics.logException(e);
            }
        }

        // Populates and saves file with all the events
        private void saveEventFile() throws IOException {
            // construct the events file content
            int len = events.size();
            for (int i = 0; i < len; i++) {
                eventsFileContent.append("\n")
                    .append(events.valueAt(i))
                    .append(",\t")
                    .append(events.keyAt(i) / (float) sampleRate);
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

    public Recorder(int sampleRate, int channelCount) throws IOException {
        // start sample writing thread
        writeThread = new WriteThread(sampleRate, channelCount);
        writeThread.start();
    }

    /**
     * Writes specified {@code sampleWithEvents} to the audio stream.
     */
    public void writeAudioWithEvents(@NonNull SamplesWithEvents samplesWithEvents) {
        if (writeThread != null) writeThread.writeData(samplesWithEvents);
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
        working.set(false);
    }
}
