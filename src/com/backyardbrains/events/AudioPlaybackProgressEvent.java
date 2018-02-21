package com.backyardbrains.events;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class AudioPlaybackProgressEvent {

    private final long progress;
    private final int sampleRate;

    public AudioPlaybackProgressEvent(long progress, int sampleRate) {
        this.progress = progress;
        this.sampleRate = sampleRate;
    }

    public long getProgress() {
        return progress;
    }

    public int getSampleRate() {
        return sampleRate;
    }
}
