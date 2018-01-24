package com.backyardbrains.usb;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbRequest;
import android.support.annotation.NonNull;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.backyardbrains.utils.LogUtils.LOGI;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <ticapeca at gmail.com.
 */
public class HIDDevice extends BybUsbDevice {

    private static final String TAG = makeLogTag(HIDDevice.class);

    // TI Vendor ID
    private static final byte TEXAS_INSTRUMENTS_VENDOR_ID = 63;
    // BYB Vendor ID
    private static final int BYB_VENDOR_ID = 0x2E73;
    // BYB Muscle SpikerBox Pro Product ID
    private static final int BYB_PID_MUSCLE_SB_PRO = 0x1;
    // BYB Neuron SpikerBox Pro Product ID
    private static final int BYB_PID_NEURON_SB_PRO = 0x2;

    private static final String MSG_START_STREAM = "start:;";
    private static final String MSG_STOP_STREAM = "h:;";

    private ReadThread readThread;
    private WriteThread writeThread;

    private HIDBuffer usbBuffer;
    private UsbInterface usbInterface;
    private UsbEndpoint inEndpoint;
    private UsbEndpoint outEndpoint;
    private int packetSize;

    /**
     * Thread used for reading data sent by connected USB device
     */
    protected class ReadThread extends Thread {

        private BybUsbReadCallback callback;
        private UsbRequest requestIn;
        private AtomicBoolean working;

        ReadThread() {
            working = new AtomicBoolean(true);
        }

        @Override public void run() {
            while (working.get()) {
                UsbRequest request = connection.requestWait();
                if (request != null && request.getEndpoint().getType() == UsbConstants.USB_ENDPOINT_XFER_INT
                    && request.getEndpoint().getDirection() == UsbConstants.USB_DIR_IN) {
                    byte[] data = usbBuffer.getDataReceived();

                    // clear buffer, execute the callback
                    usbBuffer.clearReadBuffer();
                    onReceivedData(data);

                    // queue a new request
                    requestIn.queue(usbBuffer.getReadBuffer(), packetSize);
                }
            }
        }

        void setCallback(BybUsbReadCallback callback) {
            this.callback = callback;
        }

        void setUsbRequest(UsbRequest request) {
            this.requestIn = request;
        }

        UsbRequest getUsbRequest() {
            return requestIn;
        }

        private void onReceivedData(byte[] data) {
            if (callback != null) callback.onReceivedData(data);
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
                        connection.bulkTransfer(outEndpoint, arr, arr.length, 255);  // send data to device
                    }

                    // number of written bytes is less than 252
                    byte arr[] = new byte[data.length + 2];
                    arr[0] = TEXAS_INSTRUMENTS_VENDOR_ID;   // HID Report ID - vendor specific
                    arr[1] = (byte) (arr.length - 2);       // MSP430 HID data block chunk of 253 bytes

                    System.arraycopy(data, 0, arr, 2, data.length);
                    connection.bulkTransfer(outEndpoint, arr, arr.length, 255);  // send data to device
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

    private HIDDevice(@NonNull UsbDevice device, @NonNull UsbDeviceConnection connection) {
        super(connection);

        usbBuffer = new HIDBuffer();
        usbInterface = device.getInterface(findFirstHID(device));
    }

    public static BybUsbDevice createUsbDevice(@NonNull UsbDevice device, @NonNull UsbDeviceConnection connection) {
        if (isSupported(device)) {
            return new HIDDevice(device, connection);
        } else {
            return null;
        }
    }

    public static boolean isSupported(@NonNull UsbDevice device) {
        int vid = device.getVendorId();
        int pid = device.getProductId();

        return isHidDevice(device) && vid == BYB_VENDOR_ID && (pid == BYB_PID_MUSCLE_SB_PRO
            || pid == BYB_PID_NEURON_SB_PRO);
    }

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

    @Override public void write(byte[] buffer) {
        usbBuffer.putWriteBuffer(buffer);
    }

    @Override public void startStreaming() {
        write(MSG_START_STREAM.getBytes());
    }

    @Override public void stopStreaming() {
        write(MSG_STOP_STREAM.getBytes());
    }

    @Override public void read(BybUsbReadCallback callback) {
        if (readThread != null) {
            readThread.setCallback(callback);
            readThread.getUsbRequest().queue(usbBuffer.getReadBuffer(), packetSize);
        }
    }

    @Override public void close() {
        killReadThread();
        killWriteThread();
        connection.releaseInterface(usbInterface);
        connection.close();
    }

    private void setThreadsParams(UsbRequest request, UsbEndpoint endpoint) {
        readThread.setUsbRequest(request);
        writeThread.setUsbEndpoint(endpoint);
    }

    /*
     * Kill readThread. This must be called when closing a device.
     */
    private void killReadThread() {
        if (readThread != null) {
            readThread.stopReadThread();
            readThread = null;
        }
    }

    /*
     * Restart readThread if it has been killed before
     */
    private void restartReadThread() {
        if (readThread == null) {
            readThread = new ReadThread();
            readThread.start();
            //noinspection StatementWithEmptyBody
            while (!readThread.isAlive()) {
            } // Busy waiting
        }
    }

    /*
     * Kill writeThread. This must be called when closing a device.
     */
    private void killWriteThread() {
        if (writeThread != null) {
            writeThread.stopWriteThread();
            writeThread = null;
            usbBuffer.resetWriteBuffer();
        }
    }

    /*
     * Restart writeThread if it has been killed before
     */
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

    /**
     * Returns whether specified {@code device} is HID device.
     */
    private static boolean isHidDevice(@NonNull UsbDevice device) {
        int interfaceCount = device.getInterfaceCount();
        for (int i = 0; i <= interfaceCount - 1; i++) {
            UsbInterface usbInterface = device.getInterface(i);
            if (usbInterface.getInterfaceClass() == UsbConstants.USB_CLASS_HID) return true;
        }

        return false;
    }

    /**
     * Checks whether specified {@code device} is HID device.
     *
     * @param device Device that should be queried for HID interface
     * @return Index of the HID interface if it exists, {@code -1} otherwise.
     */
    private static int findFirstHID(@NonNull UsbDevice device) {
        int interfaceCount = device.getInterfaceCount();

        for (int i = 0; i < interfaceCount; ++i) {
            if (device.getInterface(i).getInterfaceClass() == UsbConstants.USB_CLASS_HID) return i;
        }

        LOGI(TAG, "There is no HID class interface");
        return -1;
    }
}
