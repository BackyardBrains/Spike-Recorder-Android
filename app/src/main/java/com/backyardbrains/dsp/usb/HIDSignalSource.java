package com.backyardbrains.dsp.usb;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbRequest;
import android.support.annotation.NonNull;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.LOGI;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * Implementation of {@link AbstractUsbSignalSource} capable of USB HID communication with BYB hardware.
 *
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class HIDSignalSource extends AbstractUsbSignalSource {

    private static final String TAG = makeLogTag(HIDSignalSource.class);

    // TI Vendor ID
    private static final byte TEXAS_INSTRUMENTS_VENDOR_ID = 63;

    private static final String MSG_START_STREAM = "start:;";
    private static final String MSG_STOP_STREAM = "h:;";
    private static final String MSG_HARDWARE_INQUIRY = "?:;";
    private static final String MSG_BOARD_INQUIRY = "board:;";
    private static final String MSG_SAMPLE_RATE_AND_NUM_OF_CHANNELS = "max:;";

    private ReadThread readThread;
    private WriteThread writeThread;

    @SuppressWarnings("WeakerAccess") HIDBuffer usbBuffer;
    @SuppressWarnings("WeakerAccess") UsbDeviceConnection connection;
    private UsbInterface usbInterface;
    private UsbEndpoint inEndpoint;
    private UsbEndpoint outEndpoint;
    @SuppressWarnings("WeakerAccess") int packetSize;

    /**
     * Thread used for reading data sent by connected USB device
     */
    protected class ReadThread extends Thread {

        private UsbEndpoint inEndpoint;
        private AtomicBoolean working = new AtomicBoolean(true);

        private byte[] dataReceived = new byte[HIDBuffer.DEFAULT_READ_PAYLOAD_BUFFER_SIZE];

        @Override public void run() {
            while (working.get()) {
                if (inEndpoint != null) {
                    int numberBytes = connection.bulkTransfer(inEndpoint, usbBuffer.getBufferCompatible(),
                        HIDBuffer.DEFAULT_READ_BUFFER_SIZE, 0);
                    if (numberBytes > 0) {
                        usbBuffer.getDataReceivedCompatible(dataReceived, numberBytes);

                        // first two bytes are reserved for HID Report ID(vendor specific), and number of transferred bytes
                        writeToBuffer(dataReceived, dataReceived.length);
                    }
                }
            }
        }

        void setUsbEndpoint(UsbEndpoint inEndpoint) {
            this.inEndpoint = inEndpoint;
        }

        void stopReadThread() {
            working.set(false);
        }
    }

    /**
     * Thread used for writing data to connected USB device.
     */
    protected class WriteThread extends Thread {

        private UsbEndpoint outEndpoint;
        private AtomicBoolean working;

        WriteThread() {
            working = new AtomicBoolean(true);
        }

        @Override public void run() {
            while (working.get()) {
                byte[] data = usbBuffer.getWriteBuffer();
                if (data.length > 0) {
                    // number of written bytes is greater than 252
                    while (data.length > 252) {
                        byte[] temp = Arrays.copyOfRange(data, 0, 252);
                        data = Arrays.copyOfRange(data, 253, data.length - 1);

                        byte[] arr = new byte[255];
                        arr[0] = TEXAS_INSTRUMENTS_VENDOR_ID;   // HID Report ID - vendor specific
                        arr[1] = (byte) (arr.length - 2);       // MSP430 HID data block chunk of 253 bytes

                        System.arraycopy(temp, 0, arr, 2, temp.length);
                        connection.bulkTransfer(outEndpoint, arr, arr.length, 0);  // send data to device
                    }

                    // number of written bytes is less than 252
                    byte arr[] = new byte[data.length + 2];
                    arr[0] = TEXAS_INSTRUMENTS_VENDOR_ID;   // HID Report ID - vendor specific
                    arr[1] = (byte) (arr.length - 2);       // MSP430 HID data block chunk of 253 bytes

                    System.arraycopy(data, 0, arr, 2, data.length);
                    connection.bulkTransfer(outEndpoint, arr, arr.length, 0);  // send data to device
                }
            }
        }

        void setUsbEndpoint(UsbEndpoint outEndpoint) {
            this.outEndpoint = outEndpoint;
        }

        void stopWriteThread() {
            working.set(false);
        }
    }

    private HIDSignalSource(@NonNull UsbDevice device, @NonNull UsbDeviceConnection connection) {
        super(device);

        this.usbBuffer = new HIDBuffer();
        this.connection = connection;
        this.usbInterface = device.getInterface(findFirstHID(device));

        setChannelCount(2);
    }

    /**
     * Creates and returns new {@link AbstractUsbSignalSource} based on specified {@code device} capable for HID
     * communication,
     * or
     * {@code null} if specified device is not supported by BYB.
     *
     * @return BYB USB device interface configured for HID communication
     */
    public static AbstractUsbSignalSource createUsbDevice(@NonNull UsbDevice device,
        @NonNull UsbDeviceConnection connection) {
        if (isSupported(device)) {
            return new HIDSignalSource(device, connection);
        } else {
            return null;
        }
    }

    /**
     * Checks whether specified {@code device} is HID capable device supported by BYB.
     */
    public static boolean isSupported(@NonNull UsbDevice device) {
        int vid = device.getVendorId();
        int pid = device.getProductId();

        return isHidDevice(device) && vid == BYB_VENDOR_ID && (pid == BYB_PID_MUSCLE_SB_PRO
            || pid == BYB_PID_NEURON_SB_PRO);
    }

    /**
     * {@inheritDoc}
     */
    @Override public boolean open() {
        boolean ret = openHID();

        if (ret) {
            // initialize UsbRequest
            final UsbRequest requestIn = new UsbRequest();
            requestIn.initialize(connection, inEndpoint);

            // restart the working thread if it has been killed before and  get and claim interface
            restartReadThread();
            restartWriteThread();

            // pass references to the threads
            setThreadsParams(requestIn, outEndpoint);

            return true;
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override public void write(byte[] buffer) {
        LOGD(TAG, "HID WRITE: " + new String(buffer));
        usbBuffer.putWriteBuffer(buffer);
    }

    /**
     * {@inheritDoc}
     */
    @Override public void startReadingStream() {
        // start the sample stream
        write(MSG_START_STREAM.getBytes());
        // and check maximal sample rate and number of channels
        write(MSG_SAMPLE_RATE_AND_NUM_OF_CHANNELS.getBytes());
    }

    /**
     * {@inheritDoc}
     */
    @Override public void stopReadingStream() {
        // stop sample stream
        write(MSG_STOP_STREAM.getBytes());
        // and release the resources
        close();
    }

    /**
     * {@inheritDoc}
     */
    @Override public void checkHardwareType() {
        // check what hardware are connected to
        write(MSG_HARDWARE_INQUIRY.getBytes());
        // and check whether there is an expansion board connected to that hardware
        write(MSG_BOARD_INQUIRY.getBytes());
    }

    // Pass UsbRequest to read thread and UsbEndpoint (in) to write thread.
    private void setThreadsParams(UsbRequest request, UsbEndpoint endpoint) {
        readThread.setUsbEndpoint(request.getEndpoint());
        writeThread.setUsbEndpoint(endpoint);
    }

    // Stops both read and write threads and releases resources
    private void close() {
        killReadThread();
        killWriteThread();
        connection.releaseInterface(usbInterface);
        connection.close();
    }

    // Kill readThread. This must be called when closing a device.
    private void killReadThread() {
        if (readThread != null) {
            readThread.stopReadThread();
            readThread = null;
        }
    }

    // Restart readThread if it has been killed before
    private void restartReadThread() {
        if (readThread == null) {
            readThread = new ReadThread();
            readThread.start();
            //noinspection StatementWithEmptyBody
            while (!readThread.isAlive()) {
            } // Busy waiting
        }
    }

    // Kill writeThread. This must be called when closing a device.
    private void killWriteThread() {
        if (writeThread != null) {
            writeThread.stopWriteThread();
            writeThread = null;
            usbBuffer.resetWriteBuffer();
        }
    }

    // Restart writeThread if it has been killed before
    private void restartWriteThread() {
        if (writeThread == null) {
            writeThread = new WriteThread();
            writeThread.start();
            //noinspection StatementWithEmptyBody
            while (!writeThread.isAlive()) {
            } // Busy waiting
        }
    }

    private boolean openHID() {
        if (connection.claimInterface(usbInterface, true)) {
            LOGI(TAG, "Interface successfully claimed");
        } else {
            LOGI(TAG, "Interface could not be claimed");
            return false;
        }

        // Assign endpoints
        int endpointCount = usbInterface.getEndpointCount();
        for (int i = 0; i < endpointCount; i++) {
            UsbEndpoint endpoint = usbInterface.getEndpoint(i);
            if (endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_INT
                && endpoint.getDirection() == UsbConstants.USB_DIR_IN) {
                inEndpoint = endpoint;
                packetSize = inEndpoint.getMaxPacketSize();
            } else if (endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_INT
                && endpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                outEndpoint = endpoint;
            }
        }

        if (outEndpoint == null || inEndpoint == null) {
            LOGI(TAG, "Interface does not have an IN or OUT interface");
            return false;
        }

        return true;
    }

    // Returns whether specified {@code device} is HID device.
    private static boolean isHidDevice(@NonNull UsbDevice device) {
        int interfaceCount = device.getInterfaceCount();
        for (int i = 0; i <= interfaceCount - 1; i++) {
            UsbInterface usbInterface = device.getInterface(i);
            if (usbInterface.getInterfaceClass() == UsbConstants.USB_CLASS_HID) return true;
        }

        return false;
    }

    // Checks whether specified {@code device} is HID device.
    private static int findFirstHID(@NonNull UsbDevice device) {
        int interfaceCount = device.getInterfaceCount();

        for (int i = 0; i < interfaceCount; ++i) {
            if (device.getInterface(i).getInterfaceClass() == UsbConstants.USB_CLASS_HID) return i;
        }

        LOGI(TAG, "There is no HID class interface");
        return -1;
    }
}
