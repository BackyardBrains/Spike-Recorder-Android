package com.backyardbrains.events;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class UsbMessageEvent {

    private final String message;

    public UsbMessageEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
