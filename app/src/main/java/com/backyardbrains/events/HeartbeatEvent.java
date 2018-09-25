package com.backyardbrains.events;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class HeartbeatEvent {

    private int beatsPerMinute;

    public HeartbeatEvent() {
        this.beatsPerMinute = 0;
    }

    public int getBeatsPerMinute() {
        return beatsPerMinute;
    }

    public void setBeatsPerMinute(int beatsPerMinute) {
        this.beatsPerMinute = beatsPerMinute;
    }
}
