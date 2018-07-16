package com.backyardbrains.events;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
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
