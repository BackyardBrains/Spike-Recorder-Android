package com.backyardbrains.events;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class AudioRecordingProgressEvent {

    private final long progress;
    private final int sampleRate;
    private final int channelCount;
    private final int bitsPerSample;

    public AudioRecordingProgressEvent(long progress, int sampleRate, int channelCount, int bitsPerSample) {
        this.progress = progress;
        this.sampleRate = sampleRate;
        this.channelCount = channelCount;
        this.bitsPerSample = bitsPerSample;
    }

    public long getProgress() {
        return progress;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getChannelCount() {
        return channelCount;
    }

    public int getBitsPerSample() {
        return bitsPerSample;
    }
}
