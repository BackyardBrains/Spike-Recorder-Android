package com.backyardbrains.dsp.audio;

import android.media.MediaExtractor;
import androidx.annotation.NonNull;
import com.backyardbrains.utils.WavUtils;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class WavAudioFile extends BaseAudioFile {

    private final RandomAccessFile raf;

    WavAudioFile(@NonNull File file, @NonNull MediaExtractor extractor) throws IOException {
        super(file.getAbsolutePath(), extractor);

        // create RandomAccessFile
        raf = new RandomAccessFile(file, "r");
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
            FirebaseCrashlytics.getInstance().recordException(e);
            return false;
        }

        try {
            raf.seek(0);
            raf.write(WavUtils.writeHeader(file.length(), sampleRate, channelCount, encoding));
            raf.close();
        } catch (IOException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
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
