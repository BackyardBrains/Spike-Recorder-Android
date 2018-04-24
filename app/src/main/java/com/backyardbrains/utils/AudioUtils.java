package com.backyardbrains.utils;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.os.Build;
import android.support.annotation.NonNull;

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
     * Returns whether wired headset is plugged in into the device.
     */
    public static boolean isWiredHeadsetOn(@NonNull Context context) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (am != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                final AudioDeviceInfo[] audioDevices = am.getDevices(AudioManager.GET_DEVICES_INPUTS);
                for (AudioDeviceInfo aui : audioDevices) {
                    if (aui.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET) return true;
                }
            } else {
                //noinspection deprecation
                return am.isWiredHeadsetOn();
            }
        }

        return false;
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
