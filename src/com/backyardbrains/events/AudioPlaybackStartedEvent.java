package com.backyardbrains.events;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class AudioPlaybackStartedEvent {

    private final long length;

    public AudioPlaybackStartedEvent(long length) {
        this.length = length;
    }

    public long getLength() {
        return length;
    }
}
