package com.backyardbrains.utils;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.os.Build;
import android.support.annotation.Nullable;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class AudioUtils {

    private static final String TAG = makeLogTag(AudioUtils.class);

    /**
     * Default sample rate that will be used for input audio source.
     */
    public static final int DEFAULT_SAMPLE_RATE = 44100;
    /**
     * Default channel count that will be used for input audio source.
     */
    public static final int DEFAULT_CHANNEL_COUNT = 1;
    /**
     * Default channel config that will be used for input audio source.
     */
    public static final boolean[] DEFAULT_CHANNEL_CONFIG = new boolean[] { true };

    /**
     * Default channel configuration that will be used for input audio source.
     */
    public static final int DEFAULT_CHANNEL_IN_MASK = AudioFormat.CHANNEL_IN_MONO;
    /**
     * Default audio format that will be used for input audio source.
     */
    public static final int DEFAULT_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    /**
     * Default buffer size used for input audio source.
     */
    public static final int DEFAULT_IN_BUFFER_SIZE;

    private static final int BUFFER_SIZE_FACTOR = 1;

    static {
        // in buffer size
        int inBufferSize = AudioRecord.getMinBufferSize(DEFAULT_SAMPLE_RATE, DEFAULT_CHANNEL_IN_MASK, DEFAULT_ENCODING);
        DEFAULT_IN_BUFFER_SIZE =
            inBufferSize == AudioRecord.ERROR || inBufferSize == AudioRecord.ERROR_BAD_VALUE ? DEFAULT_SAMPLE_RATE * 2
                : inBufferSize * BUFFER_SIZE_FACTOR;
    }

    /**
     * Creates and returns configured {@link AudioTrack} for playing recorded audio files.
     */
    public static AudioTrack createAudioTrack(int sampleRate, int channelCount) {
        LOGD(TAG, "Create new AudioTrack");

        int outBufferSize = getOutBufferSize(sampleRate, channelCount);
        int channelMask = getChannelMask(channelCount);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new AudioTrack.Builder().setAudioAttributes(
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build())
                .setAudioFormat(new AudioFormat.Builder().setEncoding(DEFAULT_ENCODING)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelMask)
                    .build())
                .setBufferSizeInBytes(outBufferSize)
                .build();
        } else {
            //noinspection deprecation
            return new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelMask, DEFAULT_ENCODING, outBufferSize,
                AudioTrack.MODE_STREAM);
        }
    }

    /**
     * Returns estimated minimum buffer size required for an {@link AudioTrack} object to be created.
     */
    public static int getOutBufferSize(int sampleRate, int channelCount) {
        // out buffer size
        final int outBufferSize =
            AudioTrack.getMinBufferSize(sampleRate, getChannelMask(channelCount), DEFAULT_ENCODING);
        return outBufferSize == AudioTrack.ERROR || outBufferSize == AudioTrack.ERROR_BAD_VALUE ? sampleRate * 2
            : outBufferSize * BUFFER_SIZE_FACTOR;
    }

    @Nullable public static AudioRecord createAudioRecord() {
        return createAudioRecord(DEFAULT_SAMPLE_RATE, DEFAULT_CHANNEL_IN_MASK, DEFAULT_ENCODING,
            DEFAULT_IN_BUFFER_SIZE);
    }

    /**
     * Creates and returns configured {@link AudioRecord} for recording audio files.
     */
    @Nullable public static AudioRecord createAudioRecord(int sampleRate, int channelMask, int encoding,
        int inBufferSize) {
        LOGD(TAG, "Create new AudioRecorder");
        AudioRecord ar = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                ar = new AudioRecord.Builder().setAudioSource(MediaRecorder.AudioSource.DEFAULT)
                    .setAudioFormat(new AudioFormat.Builder().setEncoding(encoding)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelMask)
                        .build())
                    .setBufferSizeInBytes(inBufferSize)
                    .build();
            } catch (Exception ignored) {
            }
        } else {
            ar = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, sampleRate, channelMask, encoding, inBufferSize);
        }

        return ar;
    }

    /**
     * Creates and returns configured {@link SoundPool} for playing short audio files.
     */
    public static SoundPool createSoundPool() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return new SoundPool.Builder().setMaxStreams(1)
                .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setLegacyStreamType(AudioManager.STREAM_RING)
                    .build())
                .build();
        } else {
            //noinspection deprecation
            return new SoundPool(1, AudioManager.STREAM_RING, 0);
        }
    }

    /**
     * Returns number of samples that this number of bytes represents using current audio format.
     */
    public static long getSampleCount(long byteCount) {
        return (long) (byteCount * .5f);
    }

    /**
     * Returns number of frames that this number of bytes represents using current audio format and channel count.
     */
    public static long getFrameCount(long byteCount, int channelCount) {
        return (long) (byteCount * .5f / channelCount);
    }

    /**
     * Returns number of bytes that this number of samples represents using current audio format.
     */
    public static int getByteCount(int sampleCount) {
        return sampleCount * 2;
    }

    // Returns channel configuration depending on the specified channelCount
    private static int getChannelMask(int channelCount) {
        switch (channelCount) {
            default:
            case 1:
                return AudioFormat.CHANNEL_OUT_MONO;
            case 2:
                return AudioFormat.CHANNEL_OUT_STEREO;
            case 3:
                return AudioFormat.CHANNEL_OUT_STEREO | AudioFormat.CHANNEL_OUT_FRONT_CENTER;
            case 4:
                return AudioFormat.CHANNEL_OUT_QUAD;
            case 5:
                return AudioFormat.CHANNEL_OUT_QUAD | AudioFormat.CHANNEL_OUT_FRONT_CENTER;
            case 6:
                return AudioFormat.CHANNEL_OUT_5POINT1;
            case 7:
                return AudioFormat.CHANNEL_OUT_5POINT1 | AudioFormat.CHANNEL_OUT_BACK_CENTER;
            case 8:
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? AudioFormat.CHANNEL_OUT_7POINT1_SURROUND
                    : AudioFormat.CHANNEL_OUT_7POINT1;
        }
    }
}
