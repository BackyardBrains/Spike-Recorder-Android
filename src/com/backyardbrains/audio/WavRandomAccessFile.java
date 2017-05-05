package com.backyardbrains.audio;

import android.support.annotation.NonNull;
import com.backyardbrains.utils.WavUtils;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class WavRandomAccessFile implements BYBRandomAccessFile {

    private final RandomAccessFile raf;

    public WavRandomAccessFile(@NonNull File file) throws IOException {
        this.raf = new RandomAccessFile(file, "r");
        this.raf.seek(WavUtils.HEADER_SIZE);
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
