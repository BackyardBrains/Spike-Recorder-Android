package com.backyardbrains.events;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class AudioPlaybackStartedEvent {

    private final long length;
    private final int sampleRate;

    public AudioPlaybackStartedEvent(long length, int sampleRate) {
        this.length = length;
        this.sampleRate = sampleRate;
    }

    public long getLength() {
        return length;
    }

    public int getSampleRate() {
        return sampleRate;
    }
}
