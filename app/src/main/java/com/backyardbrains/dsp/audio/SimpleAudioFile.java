package com.backyardbrains.dsp.audio;

import android.media.MediaExtractor;
import androidx.annotation.NonNull;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class SimpleAudioFile extends BaseAudioFile {

    SimpleAudioFile(@NonNull String absolutePath, @NonNull MediaExtractor extractor) {
        super(absolutePath, extractor);
    }

    @Override public void close() {
        throw new UnsupportedOperationException("close() is not supported for M4A audio files.");
    }

    @Override public void seek(long position) {
        throw new UnsupportedOperationException(
            "seek(position) is not supported for M4A audio files.");
    }

    @Override public int read(byte[] b, int off, int len) {
        throw new UnsupportedOperationException(
            "read(b, off, len) is not supported for M4A audio files.");
    }

    @Override public long getFilePointer() {
        throw new UnsupportedOperationException(
            "getFilePointer() is not supported for M4A audio files.");
    }
}
