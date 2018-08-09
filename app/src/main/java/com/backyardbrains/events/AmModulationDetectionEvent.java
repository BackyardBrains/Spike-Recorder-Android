package com.backyardbrains.events;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class AmModulationDetectionEvent {

    private boolean start;

    public AmModulationDetectionEvent() {
        start = false;
    }

    public AmModulationDetectionEvent(boolean start) {
        this.start = start;
    }

    public boolean isStart() {
        return start;
    }

    public void setStart(boolean start) {
        this.start = start;
    }
}
