package com.backyardbrains.usb;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.utils.SampleStreamUtils;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * Implementation of {@link AbstractUsbSampleSource} capable of USB serial communication with BYB hardware.
 *
 * @author Tihomir Leka <ticapeca at gmail.com.
 */

public class SerialSampleSource extends AbstractUsbSampleSource {

    private static final String TAG = makeLogTag(SerialSampleSource.class);

    // Arduino Vendor ID
    private static final int ARDUINO_VENDOR_ID_1 = 0x2341;
    // Arduino Vendor ID
    private static final int ARDUINO_VENDOR_ID_2 = 0x2A03;
    // FTDI Vendor ID
    private static final int FTDI_VENDOR_ID = 0x0403;
    // CH340 Chinese boards Vendor ID
    private static final int CH340_VENDOR_ID = 0x1A86;

    private static final int BAUD_RATE = 230400;

    private static final String MSG_CONFIG_PREFIX = "conf ";
    private static final String MSG_SAMPLE_RATE = "s:%d;";
    private static final String MSG_CHANNELS = "c:%d;";

    private static final String MSG_BOARD_TYPE_INQUIRY = "b:;\n";
    private static final String MSG_CONFIG_SAMPLE_RATE_AND_CHANNELS;

    static {
        MSG_CONFIG_SAMPLE_RATE_AND_CHANNELS =
            MSG_CONFIG_PREFIX + String.format(Locale.getDefault(), MSG_SAMPLE_RATE, SampleStreamUtils.SAMPLE_RATE)
                + String.format(Locale.getDefault(), MSG_CHANNELS, 1) + "\n";
    }

    private UsbSerialDevice serialDevice;

    private ReadThread readThread;

    /**
     * Thread used for reading data sent by connected USB device
     */
    protected class ReadThread extends Thread {

        private static final int DEFAULT_READ_BUFFER_SIZE = 256;

        private final byte[] dataBuffer = new byte[DEFAULT_READ_BUFFER_SIZE];

        private UsbSerialDevice serialDevice;
        private AtomicBoolean working = new AtomicBoolean(true);

        @Override public void run() {
            while (working.get()) {
                if (serialDevice != null) {
                    int numberBytes = serialDevice.syncRead(dataBuffer, 64);
                    if (numberBytes > 0) writeToBuffer(Arrays.copyOfRange(dataBuffer, 0, numberBytes));
                }
            }
        }

        void setSerialDevice(UsbSerialDevice serialDevice) {
            this.serialDevice = serialDevice;
        }

        void stopReadThread() {
            working.set(false);
        }
    }

    private SerialSampleSource(@NonNull UsbDevice device, @NonNull UsbDeviceConnection connection,
        @Nullable OnSamplesReceivedListener listener) {
        super(device, listener);

        serialDevice = UsbSerialDevice.createUsbSerialDevice(device, connection);
    }

    /**
     * Creates and returns new {@link AbstractUsbSampleSource} based on specified {@code device} capable for serial
     * communication,
     * or {@code null} if specified device is not supported by BYB.
     *
     * @return BYB USB device interface configured for serial communication
     */
    public static AbstractUsbSampleSource createUsbDevice(@NonNull UsbDevice device,
        @NonNull UsbDeviceConnection connection, @Nullable OnSamplesReceivedListener listener) {
        return new SerialSampleSource(device, connection, listener);
    }

    /**
     * Checks whether specified {@code device} is serial capable device supported by BYB.
     */
    public static boolean isSupported(@NonNull UsbDevice device) {
        int vid = device.getVendorId();
        return UsbSerialDevice.isSupported(device) && (vid == BYB_VENDOR_ID || vid == ARDUINO_VENDOR_ID_1
            || vid == ARDUINO_VENDOR_ID_2 || vid == FTDI_VENDOR_ID || vid == CH340_VENDOR_ID);
    }

    @Override protected void onInputStart() {
        // prepare serial usb device for communication
        if (serialDevice != null) {
            serialDevice.setBaudRate(BAUD_RATE);
            serialDevice.setDataBits(UsbSerialInterface.DATA_BITS_8);
            serialDevice.setStopBits(UsbSerialInterface.STOP_BITS_1);
            serialDevice.setParity(UsbSerialInterface.PARITY_NONE);
            serialDevice.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
        }

        super.onInputStart();
    }

    /**
     * {@inheritDoc}
     */
    @Override protected void onInputStop() {
        //if (serialDevice != null) serialDevice.close();
        if (readThread != null) {
            readThread.stopReadThread();
            readThread = null;
        }
        if (serialDevice != null) serialDevice.syncClose();
    }

    /**
     * {@inheritDoc}
     */
    @Override public boolean open() {
        //return serialDevice != null && serialDevice.open();
        boolean ret = serialDevice != null && serialDevice.syncOpen();
        if (ret) {
            if (readThread == null) {
                readThread = new ReadThread();
                readThread.setSerialDevice(serialDevice);
                readThread.start();
            }

            return true;
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override public void write(byte[] buffer) {
        if (serialDevice != null) serialDevice.write(buffer);
    }

    /**
     * {@inheritDoc}
     */
    @Override public void startReadingStream() {
        // we don't actually start the stream, it's automatically stared after connection, but we should
        // configure sample rate and num of channels at startup
        write(MSG_CONFIG_SAMPLE_RATE_AND_CHANNELS.getBytes());
    }

    /**
     * {@inheritDoc}
     */
    @Override public void checkHardwareType() {
        write(MSG_BOARD_TYPE_INQUIRY.getBytes());
    }
}
