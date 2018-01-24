package com.backyardbrains.usb;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.support.annotation.NonNull;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>.
 */
public abstract class BybUsbDevice implements BybUsbInterface {

    final UsbDeviceConnection connection;

    public BybUsbDevice(@NonNull UsbDeviceConnection connection) {
        this.connection = connection;
    }

    /**
     * Creates and returns {@link BybUsbDevice} instance that should be used to communicate with the connected USB
     * device.
     */
    public static BybUsbDevice createUsbDevice(@NonNull UsbDevice device, @NonNull UsbDeviceConnection connection) {
        if (SerialDevice.isSupported(device)) {
            return SerialDevice.createUsbDevice(device, connection);
        } else if (HIDDevice.isSupported(device)) {
            return HIDDevice.createUsbDevice(device, connection);
        } else {
            return null;
        }
    }

    /**
     * Returns whether specified {@code device} is supported device for this app.
     */
    public static boolean isSupported(@NonNull UsbDevice device) {
        return SerialDevice.isSupported(device) || HIDDevice.isSupported(device);
    }

    @Override public abstract boolean open();

    @Override public abstract void write(byte[] buffer);

    @Override public abstract void read(BybUsbReadCallback callback);

    @Override public abstract void close();
}
