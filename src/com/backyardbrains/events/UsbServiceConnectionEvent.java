package com.backyardbrains.events;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class UsbServiceConnectionEvent {

    private final boolean connected;

    public UsbServiceConnectionEvent(boolean connected) {
        this.connected = connected;
    }

    public boolean isConnected() {
        return connected;
    }
}
