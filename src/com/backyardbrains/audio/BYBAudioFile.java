package com.backyardbrains.audio;

import java.io.IOException;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public interface BYBAudioFile {

    /**
     * Returns absolute path to the underlying audio file.
     */
    String getAbsolutePath();

    /**
     * Number of channels.
     */
    int numChannels();

    /**
     * Audio sample rate.
     */
    int sampleRate();

    /**
     * Number of bits per audio sample.
     */
    int bitsPerSample();

    /**
     * Length of the file in bytes.
     *
     * @throws IOException
     */
    long length() throws IOException;

    /**
     * Closes the file and releases the resources.
     *
     * @throws IOException
     */
    void close() throws IOException;

    /**
     * Seeks to a specified {@code offset} starting from the file beginning.
     *
     * @throws IOException
     */
    void seek(long offset) throws IOException;

    /**
     * Reads {@code b.length} bytes from the current file pointer and copies it to the specified {@code b} array.
     *
     * @throws IOException
     */
    int read(byte[] b) throws IOException;

    /**
     * Reads {@code len} bytes from the file starting from the specified {@code off} and copies it tot the specified
     * {@code b} array.
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
