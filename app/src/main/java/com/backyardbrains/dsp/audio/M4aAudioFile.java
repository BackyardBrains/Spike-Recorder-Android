package com.backyardbrains.dsp.audio;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import androidx.annotation.NonNull;
import com.backyardbrains.utils.AudioUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class M4aAudioFile extends BaseAudioFile {

    M4aAudioFile(@NonNull File file) throws IOException {
        super(file);

        final MediaExtractor extractor = new MediaExtractor();
        try (FileInputStream source = new FileInputStream(file)) {
            extractor.setDataSource(source.getFD());
            extractor.selectTrack(0);

            final MediaFormat format = extractor.getTrackFormat(0);
            mimeType(format.getString(MediaFormat.KEY_MIME));
            channelCount(format.containsKey(MediaFormat.KEY_CHANNEL_COUNT) ? format.getInteger(
                MediaFormat.KEY_CHANNEL_COUNT) : AudioUtils.DEFAULT_CHANNEL_COUNT);
            sampleRate(format.containsKey(MediaFormat.KEY_SAMPLE_RATE) ? format.getInteger(
                MediaFormat.KEY_SAMPLE_RATE) : AudioUtils.DEFAULT_SAMPLE_RATE);
            bitsPerSample(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && format.containsKey(
                MediaFormat.KEY_PCM_ENCODING) ? AudioUtils.getBitsPerSample(
                format.getInteger(MediaFormat.KEY_PCM_ENCODING))
                : AudioUtils.DEFAULT_BITS_PER_SAMPLE);
            if (format.containsKey(MediaFormat.KEY_DURATION)) {
                duration((float) format.getLong(MediaFormat.KEY_DURATION) / 1000000);
                sampleCount((long) (duration() * sampleRate()));
            }
        } catch (IOException e) {
            extractor.release();

            throw e;
        }
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
