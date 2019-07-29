package com.backyardbrains.dsp.usb;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.support.annotation.NonNull;
import com.backyardbrains.utils.SampleStreamUtils;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import java.util.Locale;

import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * Implementation of {@link AbstractUsbSignalSource} capable of USB serial communication with BYB hardware.
 *
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */

public class SerialSignalSource extends AbstractUsbSignalSource {

    private static final String TAG = makeLogTag(SerialSignalSource.class);

    // Arduino Vendor ID
    private static final int ARDUINO_VENDOR_ID_1 = 0x2341; // 9025
    // Arduino Vendor ID
    private static final int ARDUINO_VENDOR_ID_2 = 0x2A03; // 10755
    // Arduino Leonardo (bootloader) Product ID
    private static final int ARDUINO_LEONARDO_BOOTLOADER_PRODUCT_ID = 0x0036; // 54
    // Arduino Micro (bootloader) Product ID
    private static final int ARDUINO_MICRO_BOOTLOADER_PRODUCT_ID = 0x0037; // 55
    // Arduino Robot Control (bootloader) Product ID
    private static final int ARDUINO_ROBOT_CONTROL_BOOTLOADER_PRODUCT_ID = 0x0038; // 56
    // Arduino Robot Motor (bootloader) Product ID
    private static final int ARDUINO_ROBOT_MOTOR_BOOTLOADER_PRODUCT_ID = 0x0039; // 57
    // Arduino Micro ADK rev3 (bootloader) Product ID
    private static final int ARDUINO_MICRO_ADK_REV3_BOOTLOADER_PRODUCT_ID = 0x003A; // 58
    // Arduino Explora (bootloader) Product ID
    private static final int ARDUINO_EXPLORA_BOOTLOADER_PRODUCT_ID = 0x003C; // 60
    // Arduino Yun (bootloader) Product ID
    private static final int ARDUINO_YUN_BOOTLOADER_PRODUCT_ID = 0x0041; // 65
    // Arduino Zero Pro (bootloader) Product ID
    private static final int ARDUINO_ZERO_PRO_BOOTLOADER_PRODUCT_ID = 0x004D; // 77

    // FTDI Vendor ID
    private static final int FTDI_VENDOR_ID = 0x0403; // 1027
    // CH340 Chinese boards Vendor ID
    private static final int CH340_VENDOR_ID = 0x1A86; // 6790

    private static final int BAUD_RATE = 230400;

    private static final String MSG_CONFIG_PREFIX = "conf ";
    private static final String MSG_SAMPLE_RATE = "s:%d;";
    private static final String MSG_CHANNELS = "c:%d;";

    private static final String MSG_BOARD_TYPE_INQUIRY = "b:;";
    private static final String MSG_CONFIG_SAMPLE_RATE_AND_CHANNELS;

    static {
        MSG_CONFIG_SAMPLE_RATE_AND_CHANNELS = MSG_CONFIG_PREFIX + String.format(Locale.getDefault(), MSG_SAMPLE_RATE,
            SampleStreamUtils.DEFAULT_SAMPLE_RATE) + String.format(Locale.getDefault(), MSG_CHANNELS, 1) + "\n";
    }

    //private ReadThread readThread;
    //private WriteThread writeThread;

    @SuppressWarnings("WeakerAccess") UsbSerialDevice serialDevice;
    @SuppressWarnings("WeakerAccess") final SerialBuffer usbBuffer;

    private UsbSerialInterface.UsbReadCallback readCallback = data -> {
        if (serialDevice != null) {
            //LOGD(TAG, "READ(" + data.length + ") -> " + Arrays.toString(data));
            if (data.length > 0) {
                writeToBuffer(data, data.length);
            }
        }
    };

    /**
     * Thread used for reading data sent by connected USB device
     */
    //protected class ReadThread extends Thread {
    //
    //    private AtomicBoolean working = new AtomicBoolean(true);
    //
    //    private final byte[] dataReceived = new byte[SerialBuffer.DEFAULT_READ_BUFFER_SIZE];
    //
    //    @Override public void run() {
    //        while (working.get()) {
    //            if (serialDevice != null) {
    //                int numberBytes = serialDevice.syncRead(usbBuffer.getReadBuffer(), 64);
    //                LOGD(TAG, "READ(" + numberBytes + ") -> " + Arrays.toString(
    //                    Arrays.copyOfRange(usbBuffer.getReadBuffer(), 0, numberBytes)));
    //                if (numberBytes > 0) {
    //                    usbBuffer.getDataReceived(dataReceived, numberBytes);
    //
    //                    writeToBuffer(dataReceived, numberBytes);
    //                }
    //            }
    //        }
    //    }
    //
    //    void stopReadThread() {
    //        working.set(false);
    //    }
    //}

    /**
     * Thread used for writing data to connected USB device.
     */
    //protected class WriteThread extends Thread {
    //
    //    private AtomicBoolean working = new AtomicBoolean(true);
    //
    //    @Override public void run() {
    //        while (working.get()) {
    //            if (serialDevice != null) {
    //                byte[] data = usbBuffer.getWriteBuffer();
    //                LOGD(TAG, "WRITE(" + data.length + ") -> " + Arrays.toString(data));
    //                if (data.length > 0) {
    //                    serialDevice.syncWrite(data, 64);
    //                }
    //            }
    //        }
    //    }
    //
    //    void stopWriteThread() {
    //        working.set(false);
    //    }
    //}
    private SerialSignalSource(@NonNull UsbDevice device, @NonNull UsbDeviceConnection connection) {
        super(device);

        usbBuffer = new SerialBuffer();
        serialDevice = UsbSerialDevice.createUsbSerialDevice(device, connection);
    }

    /**
     * Creates and returns new {@link AbstractUsbSignalSource} based on specified {@code device} capable for serial
     * communication,
     * or {@code null} if specified device is not supported by BYB.
     *
     * @return BYB USB device interface configured for serial communication
     */
    public static AbstractUsbSignalSource createUsbDevice(@NonNull UsbDevice device,
        @NonNull UsbDeviceConnection connection) {
        return new SerialSignalSource(device, connection);
    }

    /**
     * Checks whether specified {@code device} is serial capable device supported by BYB.
     */
    public static boolean isSupported(@NonNull UsbDevice device) {
        int vid = device.getVendorId();
        int pid = device.getProductId();
        return UsbSerialDevice.isSupported(device) && (vid == BYB_VENDOR_ID || (vid == ARDUINO_VENDOR_ID_1
            && isNotArduinoBootloader(pid)) || (vid == ARDUINO_VENDOR_ID_2 && isNotArduinoBootloader(pid))
            || vid == FTDI_VENDOR_ID || vid == CH340_VENDOR_ID);
    }

    // Checks whether specified PID is Arduino bootloader PID
    private static boolean isNotArduinoBootloader(int pid) {
        return pid != ARDUINO_LEONARDO_BOOTLOADER_PRODUCT_ID && pid != ARDUINO_MICRO_BOOTLOADER_PRODUCT_ID
            && pid != ARDUINO_ROBOT_CONTROL_BOOTLOADER_PRODUCT_ID && pid != ARDUINO_ROBOT_MOTOR_BOOTLOADER_PRODUCT_ID
            && pid != ARDUINO_MICRO_ADK_REV3_BOOTLOADER_PRODUCT_ID && pid != ARDUINO_EXPLORA_BOOTLOADER_PRODUCT_ID
            && pid != ARDUINO_YUN_BOOTLOADER_PRODUCT_ID && pid != ARDUINO_ZERO_PRO_BOOTLOADER_PRODUCT_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override public boolean open() {
        boolean ret = serialDevice != null && serialDevice.open()/*serialDevice.syncOpen()*/;
        if (ret) {
            // restart the working thread if it has been killed before and  get and claim interface
            //restartReadThread();
            //restartWriteThread();

            return true;
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override public void write(byte[] buffer) {
        //LOGD(TAG, "WRITE(" + buffer.length + ") -> " + Arrays.toString(buffer));
        //usbBuffer.putWriteBuffer(buffer);
        serialDevice.write(buffer);
    }

    /**
     * {@inheritDoc}
     */
    @Override public void startReadingStream() {
        // prepare serial usb device for communication
        if (serialDevice != null) {
            serialDevice.setBaudRate(BAUD_RATE);
            serialDevice.setDataBits(UsbSerialInterface.DATA_BITS_8);
            serialDevice.setStopBits(UsbSerialInterface.STOP_BITS_1);
            serialDevice.setParity(UsbSerialInterface.PARITY_NONE);
            serialDevice.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);

            serialDevice.read(readCallback);
        }

        // we don't actually start the stream, it's automatically stared after connection, but we should
        // configure sample rate and num of channels at startup
        write(MSG_CONFIG_SAMPLE_RATE_AND_CHANNELS.getBytes());
    }

    /**
     * {@inheritDoc}
     */
    @Override public void stopReadingStream() {
        //killReadThread();
        //killWriteThread();
        if (serialDevice != null) {
            //serialDevice.syncClose();
            serialDevice.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override public void checkHardwareType() {
        write(MSG_BOARD_TYPE_INQUIRY.getBytes());
    }

    // Kill readThread. This must be called when closing a device.
    //private void killReadThread() {
    //    if (readThread != null) {
    //        readThread.stopReadThread();
    //        readThread = null;
    //    }
    //}

    // Restart readThread if it has been killed before
    //private void restartReadThread() {
    //    if (readThread == null) {
    //        readThread = new ReadThread();
    //        readThread.start();
    //    }
    //}

    // Kill writeThread. This must be called when closing a device.
    //private void killWriteThread() {
    //    if (writeThread != null) {
    //        writeThread.stopWriteThread();
    //        writeThread = null;
    //        usbBuffer.resetWriteBuffer();
    //    }
    //}

    // Restart writeThread if it has been killed before
    //private void restartWriteThread() {
    //    if (writeThread == null) {
    //        writeThread = new WriteThread();
    //        writeThread.start();
    //    }
    //}
}
