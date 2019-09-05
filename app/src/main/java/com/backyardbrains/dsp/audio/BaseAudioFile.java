package com.backyardbrains.dsp.audio;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;
import com.backyardbrains.utils.AudioUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public abstract class BaseAudioFile implements AudioFile {

    private static final String WAV_EXT = ".wav";
    private static final Set<String> WAV_MIME_TYPES = new ArraySet<>();

    static {
        WAV_MIME_TYPES.add("audio/raw");
        WAV_MIME_TYPES.add("audio/wav");
        WAV_MIME_TYPES.add("audio/wave");
        WAV_MIME_TYPES.add("audio/x-wav");
        WAV_MIME_TYPES.add("audio/x-pn-wav");
        WAV_MIME_TYPES.add("audio/vnd.wav");
    }

    private final String absolutePath;

    private String mimeType;
    private int channelCount;
    private int sampleRate;
    private int bitsPerSample;
    private long byteCount;
    private long sampleCount;
    private float lengthInSeconds;

    BaseAudioFile(@NonNull String absolutePath, @NonNull MediaExtractor extractor) {
        // save absolute file path
        this.absolutePath = absolutePath;

        final MediaFormat format = extractor.getTrackFormat(0);
        mimeType = format.getString(MediaFormat.KEY_MIME);
        channelCount = format.containsKey(MediaFormat.KEY_CHANNEL_COUNT) ? format.getInteger(
            MediaFormat.KEY_CHANNEL_COUNT) : AudioUtils.DEFAULT_CHANNEL_COUNT;
        sampleRate = format.containsKey(MediaFormat.KEY_SAMPLE_RATE) ? format.getInteger(
            MediaFormat.KEY_SAMPLE_RATE) : AudioUtils.DEFAULT_SAMPLE_RATE;
        bitsPerSample = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && format.containsKey(
            MediaFormat.KEY_PCM_ENCODING) ? AudioUtils.getBitsPerSample(
            format.getInteger(MediaFormat.KEY_PCM_ENCODING)) : AudioUtils.DEFAULT_BITS_PER_SAMPLE;
        if (format.containsKey(MediaFormat.KEY_DURATION)) {
            lengthInSeconds = (float) format.getLong(MediaFormat.KEY_DURATION) / 1000000;
            sampleCount = (long) (lengthInSeconds * sampleRate);
            byteCount = sampleCount * channelCount * bitsPerSample / 8;
        }
    }

    @Nullable public static AudioFile create(@NonNull File file) {
        final MediaExtractor extractor = new MediaExtractor();
        try (FileInputStream source = new FileInputStream(file)) {
            return create(file, extractor, source);
        } catch (IOException ignored) {
            return null;
        } finally {
            extractor.release();
        }
    }

    @Nullable public static AudioFile create(@NonNull File file, @NonNull MediaExtractor extractor,
        @NonNull FileInputStream source) throws IOException {
        if (file.exists()) {
            final String absolutePath = file.getAbsolutePath();
            final String ext = absolutePath.substring(absolutePath.lastIndexOf("."));
            extractor.setDataSource(source.getFD());

            if (extractor.getTrackCount() != 1) {
                throw new IOException("More then one track per file is not supported.");
            }

            final MediaFormat format = extractor.getTrackFormat(0);
            final String mime = format.getString(MediaFormat.KEY_MIME);

            if (mime != null) {
                if (!mime.startsWith("audio/")) {
                    throw new IOException("Unsupported file format (not audio).");
                }

                if (WAV_EXT.equalsIgnoreCase(ext) && WAV_MIME_TYPES.contains(mime)) {
                    return new WavAudioFile(file, extractor);
                } else {
                    return new SimpleAudioFile(absolutePath, extractor);
                }
            }
        }

        return null;
    }

    @Override public String getAbsolutePath() {
        return absolutePath;
    }

    @Override public String mimeType() {
        return mimeType;
    }

    @Override public int channelCount() {
        return channelCount;
    }

    @Override public int sampleRate() {
        return sampleRate;
    }

    @Override public int bitsPerSample() {
        return bitsPerSample;
    }

    @Override public long length() {
        return byteCount;
    }

    @Override public long sampleCount() {
        return sampleCount;
    }

    @Override public float duration() {
        return lengthInSeconds;
    }

    @Override public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }
}
