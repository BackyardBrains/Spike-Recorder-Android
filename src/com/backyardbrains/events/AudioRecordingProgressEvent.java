package com.backyardbrains.events;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class AudioRecordingProgressEvent {

    private final long progress;

    public AudioRecordingProgressEvent(long progress) {
        this.progress = progress;
    }

    public long getProgress() {
        return progress;
    }
}
