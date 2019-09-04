package com.backyardbrains.dsp.audio;

import androidx.annotation.NonNull;
import com.backyardbrains.utils.WavUtils;
import com.backyardbrains.utils.WavUtils.WavHeader;
import com.crashlytics.android.Crashlytics;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class WavAudioFile extends BaseAudioFile {

    private final RandomAccessFile raf;

    WavAudioFile(@NonNull File file) throws IOException {
        super(file);

        // create RandomAccessFile
        raf = new RandomAccessFile(file, "r");

        // create header
        final byte[] headerBytes = new byte[WavUtils.HEADER_SIZE];
        read(headerBytes);
        final WavHeader header = WavUtils.readHeader(new ByteArrayInputStream(headerBytes));

        // save audio file info
        mimeType(WAV_MIME_TYPE);
        channelCount(header.getChannelCount());
        sampleRate(header.getSampleRate());
        bitsPerSample(header.getBitsPerSample());
        length(header.getDataSize());
        sampleCount(header.getDataSize() * 8 / header.getBitsPerSample());
        final float duration = header.getDataSize() / (float) (
            header.getSampleRate() * header.getChannelCount() * header.getBitsPerSample() / 8);
        duration(duration);
    }

    /**
     * Saves specified {@code file} as a WAV file and closes it.
     *
     * @throws IOException
     */
    public static boolean save(@NonNull File file, int channelCount, int sampleRate, int encoding)
        throws IOException {
        // create RandomAccessFile
        final RandomAccessFile raf;
        try {
            raf = new RandomAccessFile(file, "rw");
        } catch (FileNotFoundException e) {
            Crashlytics.logException(e);
            return false;
        }

        try {
            raf.seek(0);
            raf.write(WavUtils.writeHeader(file.length(), sampleRate, channelCount, encoding));
            raf.close();
        } catch (IOException e) {
            Crashlytics.logException(e);
            return false;
        } finally {
            raf.close();
        }

        return true;
    }

    @Override public void close() throws IOException {
        raf.close();
    }

    @Override public void seek(long offset) throws IOException {
        offset = Math.min(offset + WavUtils.HEADER_SIZE, length() - 1);
        synchronized (raf) {
            raf.seek(offset);
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
