package com.backyardbrains.events;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class AudioPlaybackProgressEvent {

    private final long progress;

    public AudioPlaybackProgressEvent(long progress) {
        this.progress = progress;
    }

    public long getProgress() {
        return progress;
    }
}
