package com.backyardbrains.dsp.audio;

import java.io.IOException;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public interface AudioFile {

    String WAV_MIME_TYPE = "audio/wave";

    /**
     * Returns absolute path to the underlying audio file.
     */
    String getAbsolutePath();

    /**
     * Audio mime type.
     */
    String mimeType();

    /**
     * Number of channels.
     */
    int channelCount();

    /**
     * Audio sample rate.
     */
    int sampleRate();

    /**
     * Number of bits per audio sample.
     */
    int bitsPerSample();

    /**
     * Total number of bytes.
     */
    long length();

    /**
     * Total number of audio samples in the file.
     */
    long sampleCount();

    /**
     * Duration of the audio in seconds.
     */
    float duration();

    /**
     * Closes the file and releases the resources.
     *
     * @throws IOException
     */
    void close() throws IOException;

    /**
     * Seeks to sample at specified {@code position}.
     *
     * @throws IOException
     */
    void seek(long position) throws IOException;

    /**
     * Reads {@code b.length} bytes from the current file pointer and copies it to the specified
     * {@code b} array.
     *
     * @throws IOException
     */
    int read(byte[] b) throws IOException;

    /**
     * Reads {@code len} bytes from the file starting from the specified {@code off} and copies it
     * to the specified {@code b} array.
     *
     * @throws IOException
     */
    int read(byte[] b, int off, int len) throws IOException;

    /**
     * Returns current file pointer.
     *
     * @throws IOException
     */
    long getFilePointer() throws IOException;
}
