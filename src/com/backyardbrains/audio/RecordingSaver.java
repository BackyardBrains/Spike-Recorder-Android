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

import com.backyardbrains.utils.RecordingUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

class RecordingSaver {

    private final File file;
    private final OutputStream outputStream;

    RecordingSaver() throws IOException {
        file = RecordingUtils.createRecordingFile();

        try {
            outputStream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            throw new IOException("could not build OutputStream from this file: " + file.getAbsolutePath(), e);
        }
    }

    /**
     * Writes specified {@code audioInfo} to the audio stream.
     *
     * @throws IllegalStateException
     */
    void writeAudio(ByteBuffer audioInfo) throws IllegalStateException {
        try {
            outputStream.write(audioInfo.array());
        } catch (IOException e) {
            throw new IllegalStateException("Could not write bytes out to file");
        }
    }

    /**
     * Returns currently recorder length.
     */
    long getAudioLength() {
        return file.length();
    }

    /**
     * Closes the audio stream and save the audio file to storage.
     *
     * @throws IllegalStateException
     */
    void stopRecording() throws IllegalStateException {
        try {
            outputStream.flush();
            outputStream.close();

            WavAudioFile.save(file);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write wav header.");
        }
    }
}
