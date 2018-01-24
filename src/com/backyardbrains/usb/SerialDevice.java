package com.backyardbrains.usb;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.support.annotation.NonNull;
import com.backyardbrains.utils.UsbUtils;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import java.util.Locale;

import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <ticapeca at gmail.com.
 */

public class SerialDevice extends BybUsbDevice {

    private static final String TAG = makeLogTag(SerialDevice.class);

    // BYB Vendor ID
    private static final int BYB_VENDOR_ID = 0x2E73;
    private static final int ARDUINO_VENDOR_ID_1 = 0x2341;
    private static final int ARDUINO_VENDOR_ID_2 = 0x2A03;
    private static final int FTDI_VENDOR_ID = 0x0403;
    private static final int CH340_VENDOR_ID = 0x1A86;

    private static final int BAUD_RATE = 230400;

    private static final String MSG_CONFIG_PREFIX = "conf ";
    private static final String MSG_SAMPLE_RATE = "s:%d;";
    private static final String MSG_CHANNELS = "c:%d;";

    private static final String MSG_BOARD_TYPE = "b:;\n";
    private static final String MSG_CONFIG_SAMPLE_RATE_AND_CHANNELS;

    static {
        MSG_CONFIG_SAMPLE_RATE_AND_CHANNELS =
            MSG_CONFIG_PREFIX + String.format(Locale.getDefault(), MSG_SAMPLE_RATE, UsbUtils.SAMPLE_RATE)
                + String.format(Locale.getDefault(), MSG_CHANNELS, 1) + "\n";
    }

    private UsbSerialDevice serialDevice;
    private BybUsbReadCallback callback;

    private SerialDevice(@NonNull UsbDevice device, @NonNull UsbDeviceConnection connection) {
        super(connection);

        serialDevice = UsbSerialDevice.createUsbSerialDevice(device, connection);
        if (serialDevice != null) {
            serialDevice.setBaudRate(BAUD_RATE);
            serialDevice.setDataBits(UsbSerialInterface.DATA_BITS_8);
            serialDevice.setStopBits(UsbSerialInterface.STOP_BITS_1);
            serialDevice.setParity(UsbSerialInterface.PARITY_NONE);
            serialDevice.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
        }
    }

    public static BybUsbDevice createUsbDevice(@NonNull UsbDevice device, @NonNull UsbDeviceConnection connection) {
        return new SerialDevice(device, connection);
    }

    public static boolean isSupported(@NonNull UsbDevice device) {
        int vid = device.getVendorId();
        return UsbSerialDevice.isSupported(device) && (vid == BYB_VENDOR_ID || vid == ARDUINO_VENDOR_ID_1
            || vid == ARDUINO_VENDOR_ID_2 || vid == FTDI_VENDOR_ID || vid == CH340_VENDOR_ID);
    }

    @Override public boolean open() {
        return serialDevice != null && serialDevice.open();
    }

    @Override public void write(byte[] buffer) {
        if (serialDevice != null) serialDevice.write(buffer);
    }

    @Override public void startStreaming() {
        // we don't actually start the stream, it's automatically stared after connection, but we should
        // configure sample rate and num of channels at startup
        write(MSG_CONFIG_SAMPLE_RATE_AND_CHANNELS.getBytes());
    }

    @Override public void stopStreaming() {
        // sending of stream sample data cannot be started
    }

    @Override public void read(BybUsbReadCallback callback) {
        this.callback = callback;
        if (serialDevice != null) {
            serialDevice.read(new UsbSerialInterface.UsbReadCallback() {
                @Override public void onReceivedData(byte[] bytes) {
                    if (SerialDevice.this.callback != null) SerialDevice.this.callback.onReceivedData(bytes);
                }
            });
        }
    }

    @Override public void close() {
        if (serialDevice != null) serialDevice.close();
    }
}
