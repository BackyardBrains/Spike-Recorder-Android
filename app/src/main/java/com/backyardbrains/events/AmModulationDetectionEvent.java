package com.backyardbrains.events;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class AmModulationDetectionEvent {

    private final boolean start;

    public AmModulationDetectionEvent(boolean start) {
        this.start = start;
    }

    public boolean isStart() {
        return start;
    }
}
