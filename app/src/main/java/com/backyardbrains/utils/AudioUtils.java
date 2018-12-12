package com.backyardbrains.utils;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.os.Build;

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
     * Default channel configuration that will be used for input audio source.
     */
    public static final int DEFAULT_CHANNEL_MASK = AudioFormat.CHANNEL_IN_MONO;
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
        int intBufferSize = AudioRecord.getMinBufferSize(DEFAULT_SAMPLE_RATE, DEFAULT_CHANNEL_MASK, DEFAULT_ENCODING);
        DEFAULT_IN_BUFFER_SIZE =
            intBufferSize == AudioRecord.ERROR || intBufferSize == AudioRecord.ERROR_BAD_VALUE ? DEFAULT_SAMPLE_RATE * 2
                : intBufferSize * BUFFER_SIZE_FACTOR;
    }

    /**
     * Creates and returns configured {@link AudioTrack} for playing recorded audio files.
     */
    public static AudioTrack createAudioTrack(int sampleRate) {
        LOGD(TAG, "Create new AudioTrack");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new AudioTrack.Builder().setAudioAttributes(
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build())
                .setAudioFormat(new AudioFormat.Builder().setEncoding(DEFAULT_ENCODING)
                    .setSampleRate(sampleRate)
                    .setChannelMask(DEFAULT_CHANNEL_MASK)
                    .build())
                .setBufferSizeInBytes(getOutBufferSize(sampleRate))
                .build();
        } else {
            //noinspection deprecation
            return new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, DEFAULT_CHANNEL_MASK, DEFAULT_ENCODING,
                getOutBufferSize(sampleRate), AudioTrack.MODE_STREAM);
        }
    }

    /**
     * Returns estimated minimum buffer size required for an {@link AudioTrack} object to be created.
     */
    public static int getOutBufferSize(int sampleRate) {
        // out buffer size
        final int outBufferSize = AudioTrack.getMinBufferSize(sampleRate, DEFAULT_CHANNEL_MASK, DEFAULT_CHANNEL_MASK);
        return outBufferSize == AudioTrack.ERROR || outBufferSize == AudioTrack.ERROR_BAD_VALUE ? sampleRate * 2
            : outBufferSize * BUFFER_SIZE_FACTOR;
    }

    public static AudioRecord createAudioRecord() {
        return createAudioRecord(DEFAULT_SAMPLE_RATE, DEFAULT_CHANNEL_MASK, DEFAULT_ENCODING, DEFAULT_IN_BUFFER_SIZE);
    }

    /**
     * Creates and returns configured {@link AudioRecord} for recording audio files.
     */
    public static AudioRecord createAudioRecord(int sampleRate, int channelMask, int encoding, int inBufferSize) {
        LOGD(TAG, "Create new AudioRecorder");
        final AudioRecord ar;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ar = new AudioRecord.Builder().setAudioSource(MediaRecorder.AudioSource.DEFAULT)
                .setAudioFormat(new AudioFormat.Builder().setEncoding(encoding)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelMask)
                    .build())
                .setBufferSizeInBytes(inBufferSize)
                .build();
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
        return byteCount / 2;
    }

    /**
     * Returns number of bytes that this number of samples represents using current audio format.
     */
    public static int getByteCount(int sampleCount) {
        return sampleCount * 2;
    }
}
