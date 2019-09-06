package com.backyardbrains.events;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class AudioPlaybackStartedEvent {

    private final long length;
    private final int sampleRate;
    private final int channelCount;
    private final int bitsPerSample;

    public AudioPlaybackStartedEvent(long length, int sampleRate, int channelCount, int bitsPerSample) {
        this.length = length;
        this.sampleRate = sampleRate;
        this.channelCount = channelCount;
        this.bitsPerSample = bitsPerSample;
    }

    public long getLength() {
        return length;
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
