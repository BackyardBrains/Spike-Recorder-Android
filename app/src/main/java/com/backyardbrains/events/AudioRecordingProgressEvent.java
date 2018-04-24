package com.backyardbrains.events;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class AudioRecordingProgressEvent {

    private final long progress;
    private final int sampleRate;

    public AudioRecordingProgressEvent(long progress, int sampleRate) {
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
