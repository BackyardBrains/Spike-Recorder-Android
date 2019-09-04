package com.backyardbrains.dsp.audio;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public abstract class BaseAudioFile implements AudioFile {

    private final String absolutePath;

    private String mimeType;
    private int channelCount;
    private int sampleRate;
    private int bitsPerSample;
    private long byteCount;
    private long sampleCount;
    private float lengthInSeconds;

    BaseAudioFile(@NonNull File file) {
        // save name and absolute file path
        absolutePath = file.getAbsolutePath();
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
            try {
                extractor.setDataSource(source.getFD());
            } catch (IOException e) {
                // instantiation of MediaExtractor failed, fallback to file extension
                final String absolutePath = file.getAbsolutePath();
                final String ext = absolutePath.substring(absolutePath.lastIndexOf("."));
                if (".m4a".equalsIgnoreCase(ext)) {
                    return new M4aAudioFile(file);
                } else if (".wav".equalsIgnoreCase(ext)) {
                    return new WavAudioFile(file);
                }
            }

            if (extractor.getTrackCount() != 1) {
                throw new IOException("More then one track per file is not supported.");
            }

            final MediaFormat format = extractor.getTrackFormat(0);
            final String mime = format.getString(MediaFormat.KEY_MIME);

            if (mime != null) {
                if (!mime.startsWith("audio/")) {
                    throw new IOException("Unsupported file format (not audio).");
                }

                switch (mime) {
                    case "audio/mp4a-latm":
                        return new M4aAudioFile(file);
                    case "audio/raw":
                        return new WavAudioFile(file);
                }
            }
        }

        return null;
    }

    public final boolean isWav() {
        return WAV_MIME_TYPE.equals(mimeType);
    }

    @Override public String getAbsolutePath() {
        return absolutePath;
    }

    protected void mimeType(@NonNull String mimeType) {
        this.mimeType = mimeType;
    }

    @Override public String mimeType() {
        return mimeType;
    }

    protected void channelCount(int channelCount) {
        this.channelCount = channelCount;
    }

    @Override public int channelCount() {
        return channelCount;
    }

    protected void sampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    @Override public int sampleRate() {
        return sampleRate;
    }

    protected void bitsPerSample(int bitsPerSample) {
        this.bitsPerSample = bitsPerSample;
    }

    @Override public int bitsPerSample() {
        return bitsPerSample;
    }

    protected void length(long length) {
        this.byteCount = length;
    }

    @Override public long length() {
        return byteCount;
    }

    protected void sampleCount(long sampleCount) {
        this.sampleCount = sampleCount;
    }

    @Override public long sampleCount() {
        return sampleCount;
    }

    protected void duration(float duration) {
        this.lengthInSeconds = duration;
    }

    @Override public float duration() {
        return lengthInSeconds;
    }

    @Override public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }
}
