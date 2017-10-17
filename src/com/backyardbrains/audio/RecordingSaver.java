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

import com.backyardbrains.utils.AudioUtils;
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

class RecordingSaver {

    private final File file;
    private final List<short[]> allSamples;
    private final OutputStream outputStream;

    private WriteThread writeThread;
    private boolean done;
    private int sampleRate = AudioUtils.SAMPLE_RATE;

    private class WriteThread extends Thread {

        private ByteBuffer bb;

        @Override public void run() {
            try {
                while (!done) {
                    if (allSamples.size() > 0) {
                        bb = ByteBuffer.allocate(allSamples.get(0).length * 2).order(ByteOrder.nativeOrder());
                        bb.asShortBuffer().put(allSamples.remove(0));
                        try {
                            outputStream.write(bb.array());
                        } catch (IOException e) {
                            throw new IllegalStateException("Could not write sample to file", e);
                        }
                    }
                }
                // let's record all left samples
                for (int i = 0; i < allSamples.size(); i++) {
                    bb = ByteBuffer.allocate(allSamples.get(i).length * 2).order(ByteOrder.nativeOrder());
                    bb.asShortBuffer().put(allSamples.get(i));
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
                stopRecording();
            }
        }
    }

    RecordingSaver() throws IOException {
        file = RecordingUtils.createRecordingFile();
        allSamples = new CopyOnWriteArrayList<>();

        // start writing thread
        writeThread = new WriteThread();
        writeThread.start();

        try {
            outputStream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            Crashlytics.logException(e);
            throw new IOException("could not build OutputStream from this file: " + file.getAbsolutePath(), e);
        }
    }

    /**
     * Writes specified {@code samples} to the audio stream.
     */
    void writeAudio(short[] samples) {
        this.allSamples.add(samples);
    }

    /**
     * Sets the sample rate tha will be used when saving WAV file.
     */
    void setSampleRate(int sampleRate) {
        if (this.sampleRate == sampleRate) return;
        if (sampleRate <= 0) return; // sample rate need to be positive

        this.sampleRate = sampleRate;
    }

    /**
     * Returns currently recorder length.
     */
    long getAudioLength() {
        return file.length();
    }

    /**
     * Requests the recording to stop.
     */
    void requestStop() {
        done = true;
    }

    // Closes the audio stream and saves the audio file to storage
    private void stopRecording() {
        try {
            outputStream.flush();
            outputStream.close();
            writeThread = null;

            WavAudioFile.save(file, sampleRate);
        } catch (IOException e) {
            Crashlytics.logException(e);
        }
    }
}
