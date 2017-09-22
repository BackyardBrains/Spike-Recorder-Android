package com.backyardbrains.events;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class UsbDeviceConnectionEvent {

    private final boolean attached;

    public UsbDeviceConnectionEvent(boolean attached) {
        this.attached = attached;
    }

    public boolean isAttached() {
        return attached;
    }
}
