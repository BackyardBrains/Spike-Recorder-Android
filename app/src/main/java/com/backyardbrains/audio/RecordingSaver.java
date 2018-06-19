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

import android.support.annotation.NonNull;
import com.backyardbrains.usb.SamplesWithMarkers;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.ObjectUtils;
import com.backyardbrains.utils.RecordingUtils;
import com.crashlytics.android.Crashlytics;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

class RecordingSaver {

    @SuppressWarnings("WeakerAccess") static final String TAG = makeLogTag(RecordingSaver.class);

    @SuppressWarnings("WeakerAccess") static final String EVENT_MARKERS_FILE_HEADER_CONTENT =
        "# Marker IDs can be arbitrary strings.\n# Marker ID,\tTime (in s)";

    @SuppressWarnings("WeakerAccess") WriteThread writeThread;

    private class WriteThread extends Thread {

        private final File audioFile;
        private final OutputStream outputStream;
        private final File eventsFile;
        private final AtomicBoolean working = new AtomicBoolean(true);
        private final List<SamplesWithMarkers> samples = new CopyOnWriteArrayList<>();

        private int sampleRate = AudioUtils.SAMPLE_RATE;
        private StringBuilder eventsFileContent = new StringBuilder(EVENT_MARKERS_FILE_HEADER_CONTENT);
        private ByteBuffer bb;

        WriteThread() throws IOException {
            // create recording file
            audioFile = RecordingUtils.createRecordingFile();
            // and stream to write sample to
            try {
                outputStream = new FileOutputStream(audioFile);
            } catch (FileNotFoundException e) {
                Crashlytics.logException(e);
                throw new IOException("could not build OutputStream from audio file: " + audioFile.getAbsolutePath(),
                    e);
            }

            // create events file
            eventsFile = RecordingUtils.createEventsFile(audioFile);
        }

        @Override public void run() {
            try {
                while (working.get()) {
                    if (samples.size() > 0) {
                        SamplesWithMarkers samplesWithMarkers = samples.remove(0);
                        // we first need to write all the events before start writing the samples
                        // so we get the precise times for events
                        int writtenSamples = (int) AudioUtils.getSampleCount(audioFile.length());
                        int len = samplesWithMarkers.eventIndices.length;
                        String event;
                        for (int i = 0; i < len; i++) {
                            event = samplesWithMarkers.eventLabels[i];
                            if (event != null) {
                                eventsFileContent.append("\n")
                                    .append(event)
                                    .append(",\t")
                                    .append((writtenSamples + samplesWithMarkers.eventIndices[i]) / (float) sampleRate);
                            }
                        }

                        // now we can write to audio stream
                        bb = ByteBuffer.allocate(samplesWithMarkers.samples.length * 2).order(ByteOrder.nativeOrder());
                        bb.asShortBuffer().put(samplesWithMarkers.samples);
                        try {
                            outputStream.write(bb.array());
                        } catch (IOException e) {
                            throw new IllegalStateException("Could not write sample to file", e);
                        }
                    }
                }
                // let's record all left samples
                for (int i = 0; i < samples.size(); i++) {
                    SamplesWithMarkers samplesWithMarkers = samples.get(i);
                    // we first need to write all the events before start writing the samples
                    // so we get the precise times for events
                    int writtenSamples = (int) AudioUtils.getSampleCount(audioFile.length());
                    int len = samplesWithMarkers.eventIndices.length;
                    String event;
                    for (int j = 0; j < len; j++) {
                        event = samplesWithMarkers.eventLabels[j];
                        if (event != null) {
                            eventsFileContent.append("\n")
                                .append(event)
                                .append(",\t")
                                .append((writtenSamples + samplesWithMarkers.eventIndices[j]) / (float) sampleRate);
                        }
                    }
                    // now we can write to audio stream
                    bb = ByteBuffer.allocate(samplesWithMarkers.samples.length * 2).order(ByteOrder.nativeOrder());
                    bb.asShortBuffer().put(samplesWithMarkers.samples);
                    try {
                        outputStream.write(bb.array());
                    } catch (IOException e) {
                        throw new IllegalStateException("Could not write sample to file!", e);
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
         * Appends specified {@code samples} to previously saved ones.
         */
        void writeData(@NonNull SamplesWithMarkers samplesWithMarkers) {
            if (working.get()) this.samples.add(samplesWithMarkers);
        }

        /**
         * Sets sample rate of the currently recorded audio file.
         */
        void setSampleRate(int sampleRate) {
            if (this.sampleRate == sampleRate) return;
            if (sampleRate <= 0) return; // sample rate need to be positive

            this.sampleRate = sampleRate;
        }

        /**
         * Returns current length of the recorded file.
         *
         * @return Length of the recorded file in bytes.
         */
        long getCurrentLength() {
            return audioFile.length();
        }

        /**
         * Initiates ending of recording the audio.
         */
        void stopRecording() {
            working.set(false);
        }

        // Closes the audio stream and saves the audio file to storage
        private void saveFiles() {
            try {
                outputStream.flush();
                outputStream.close();
                WavAudioFile.save(audioFile, sampleRate);

                if (!ObjectUtils.equals(EVENT_MARKERS_FILE_HEADER_CONTENT, eventsFileContent.toString())) {
                    saveEventFile();
                }

                writeThread = null;
            } catch (IOException e) {
                Crashlytics.logException(e);
            }
        }

        // Populates and saves file with all the events
        private void saveEventFile() throws IOException {
            LOGD(TAG, eventsFileContent.toString());

            // there needs to be a RETURN char at the end of the events file
            // for the desktop app to be able to parse it properly
            eventsFileContent.append("\n");

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

    RecordingSaver() throws IOException {
        // start sample writing thread
        writeThread = new WriteThread();
        writeThread.start();
    }

    /**
     * Writes specified {@code samples} to the audio stream.
     */
    void writeAudioWithEvents(@NonNull SamplesWithMarkers samplesWithMarkers) {
        if (writeThread != null) writeThread.writeData(samplesWithMarkers);
    }

    /**
     * Sets the sample rate tha will be used when saving WAV file.
     */
    void setSampleRate(int sampleRate) {
        if (writeThread != null) writeThread.setSampleRate(sampleRate);
    }

    /**
     * Returns currently recorder length.
     */
    long getAudioLength() {
        return writeThread != null ? writeThread.getCurrentLength() : 0;
    }

    /**
     * Requests the recording to stop.
     */
    void requestStop() {
        if (writeThread != null) writeThread.stopRecording();
    }
}
