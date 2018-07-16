package com.backyardbrains.events;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class HeartbeatEvent {

    private final int beatsPerMinute;

    public HeartbeatEvent(int beatsPerMinute) {
        this.beatsPerMinute = beatsPerMinute;
    }

    public int getBeatsPerMinute() {
        return beatsPerMinute;
    }
}
