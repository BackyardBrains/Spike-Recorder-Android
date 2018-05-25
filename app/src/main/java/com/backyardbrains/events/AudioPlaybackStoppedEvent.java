package com.backyardbrains.events;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class AudioPlaybackStoppedEvent {

    private final boolean completed;

    public AudioPlaybackStoppedEvent(boolean completed) {
        this.completed = completed;
    }

    public boolean isCompleted() {
        return completed;
    }
}
