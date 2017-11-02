package com.backyardbrains.events;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class UsbCommunicationEvent {

    private final boolean started;

    public UsbCommunicationEvent(boolean started) {
        this.started = started;
    }

    public boolean isStarted() {
        return started;
    }
}
