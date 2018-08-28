package com.backyardbrains.events;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
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
