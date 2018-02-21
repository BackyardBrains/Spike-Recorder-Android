package com.backyardbrains.events;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class UsbDeviceConnectionEvent {

    private final boolean connected;

    public UsbDeviceConnectionEvent(boolean connected) {
        this.connected = connected;
    }

    public boolean isConnected() {
        return connected;
    }
}
