package com.backyardbrains.audio;

import android.support.annotation.NonNull;
import com.backyardbrains.utils.WavUtils;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class WavAudioFile implements BYBAudioFile {

    private final RandomAccessFile raf;
    private final WavUtils.WavInfo header;
    private final String absolutePath;

    public WavAudioFile(@NonNull File file) throws IOException {
        // save absolute file path
        absolutePath = file.getAbsolutePath();
        // create RandomAccessFile
        this.raf = new RandomAccessFile(file, "r");
        // read file header
        final byte[] headerBytes = new byte[WavUtils.HEADER_SIZE];
        raf.read(headerBytes, 0, headerBytes.length);
        this.header = WavUtils.readHeader(new ByteArrayInputStream(headerBytes));
    }

    @Override public String getAbsolutePath() {
        return absolutePath;
    }

    @Override public int numChannels() {
        return header.getNumChannels();
    }

    @Override public int sampleRate() {
        return header.getSampleRate();
    }

    @Override public int bitsPerSample() {
        return header.getBitsPerSample();
    }

    @Override public long length() throws IOException {
        return raf.length() - WavUtils.HEADER_SIZE;
    }

    @Override public void close() throws IOException {
        raf.close();
    }

    @Override public void seek(long offset) throws IOException {
        offset = Math.min(offset + WavUtils.HEADER_SIZE, raf.length() - 1);
        synchronized (raf) {
            raf.seek(offset);
        }
    }

    @Override public int read(byte[] b) throws IOException {
        synchronized (raf) {
            return raf.read(b);
        }
    }

    @Override public int read(byte[] b, int off, int len) throws IOException {
        synchronized (raf) {
            return raf.read(b, off, len);
        }
    }

    @Override public long getFilePointer() throws IOException {
        synchronized (raf) {
            return raf.getFilePointer() - WavUtils.HEADER_SIZE;
        }
    }
}
