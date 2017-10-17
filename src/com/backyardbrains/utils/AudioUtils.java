package com.backyardbrains.utils;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class AudioUtils {

    private static final String TAG = makeLogTag(AudioUtils.class);

    /**
     * Sample rate used throughout the app.
     */
    public static final int SAMPLE_RATE = 44100;
    /**
     * Buffer size used for audio input throughout the app.
     */
    public static final int IN_BUFFER_SIZE;

    //
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int IN_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int OUT_CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO;

    static {
        // in buffer size
        final int intBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, IN_CHANNEL_CONFIG, AUDIO_FORMAT);
        IN_BUFFER_SIZE =
            intBufferSize == AudioTrack.ERROR || intBufferSize == AudioTrack.ERROR_BAD_VALUE ? SAMPLE_RATE * 2
                : intBufferSize;
    }

    /**
     * Creates and returns configured {@link AudioTrack} for playing recorded audio files.
     */
    public static AudioTrack createAudioTrack(int sampleRate) {
        LOGD(TAG, "Create new AudioTrack");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            //noinspection deprecation
            return new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO, AUDIO_FORMAT,
                getOutBufferSize(sampleRate), AudioTrack.MODE_STREAM);
        } else {
            return new AudioTrack.Builder().setAudioAttributes(
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build())
                .setAudioFormat(new AudioFormat.Builder().setEncoding(AUDIO_FORMAT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build())
                .setBufferSizeInBytes(getOutBufferSize(sampleRate))
                .build();
        }
    }

    /**
     * Returns estimated minimum buffer size required for an {@link AudioTrack} object to be created.
     */
    public static int getOutBufferSize(int sampleRate) {
        // out buffer size
        final int outBufferSize = AudioTrack.getMinBufferSize(sampleRate, OUT_CHANNEL_CONFIG, AUDIO_FORMAT);
        return outBufferSize == AudioTrack.ERROR || outBufferSize == AudioTrack.ERROR_BAD_VALUE ? sampleRate * 2
            : outBufferSize;
    }

    /**
     * Creates and returns configured {@link AudioRecord} for recording audio files.
     */
    public static AudioRecord createAudioRecord() {
        LOGD(TAG, "Create new AudioRecorder");
        return new AudioRecord(MediaRecorder.AudioSource.DEFAULT, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
            AUDIO_FORMAT, IN_BUFFER_SIZE);
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
