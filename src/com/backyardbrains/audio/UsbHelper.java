package com.backyardbrains.audio;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import com.angrygoat.buffer.CircularByteBuffer;
import com.backyardbrains.utils.UsbUtils;
import com.crashlytics.android.Crashlytics;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class UsbHelper {

    private static final String TAG = makeLogTag(UsbHelper.class);

    private static final String ACTION_USB_PERMISSION = "com.backyardbrains.usb.USB_PERMISSION";

    private static final int BUFFER_SIZE = UsbUtils.SAMPLE_RATE; // 1 sec

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

    private static final int BAUD_RATE = 230400;

    private static final IntentFilter USB_INTENT_FILTER;

    static {
        USB_INTENT_FILTER = new IntentFilter();
        USB_INTENT_FILTER.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        USB_INTENT_FILTER.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        USB_INTENT_FILTER.addAction(ACTION_USB_PERMISSION);
    }

    private BroadcastReceiver usbConnectionReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                if (granted) {
                    if (listener != null) listener.onPermissionGranted();

                    device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    communicationThread = new CommunicationThread();
                    communicationThread.start();
                    //openDevice((UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE));
                } else {
                    if (listener != null) listener.onPermissionDenied();
                }
            } else {
                final boolean attached = UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action);
                if (!attached) close();

                refreshDevices();

                if (listener != null) {
                    if (attached) {
                        listener.onDeviceAttached();
                    } else {
                        listener.onDeviceDetached();
                    }
                }
            }
        }
    };

    /**
     *
     */
    public interface UsbListener {
        /**
         *
         */
        void onDeviceAttached();

        /**
         *
         */
        void onDeviceDetached();

        /**
         *
         */
        void onDataTransferStart();

        /**
         *
         */
        void onDataTransferEnd();

        /**
         *
         */
        void onPermissionGranted();

        /**
         *
         */
        void onPermissionDenied();
    }

    private final ReceivesAudio service;
    private final UsbManager manager;
    private final UsbHelper.UsbListener listener;

    private UsbSerialDevice serialDevice;
    private UsbDevice device;

    private final List<UsbDevice> devices = new ArrayList<>();
    private final Map<String, UsbDevice> devicesMap = new HashMap<>();

    private final CircularByteBuffer circularBuffer = new CircularByteBuffer(BUFFER_SIZE);

    private boolean done, paused;
    private ReadThread readThread;
    private CommunicationThread communicationThread;

    // Thread used for reading data received from connected USB serial device
    private class ReadThread extends Thread {

        byte[] data;

        @Override public void run() {
            while (!done) {
                if (!paused) {
                    synchronized (service) {
                        data = new byte[circularBuffer.peekSize()];
                        circularBuffer.read(data, data.length, false);
                        service.receiveSampleStream(data);
                    }
                }
            }
        }
    }

    public UsbHelper(@NonNull Context context, @NonNull AudioService service, @Nullable UsbListener listener) {
        this.service = service;
        this.manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        this.listener = listener;

        refreshDevices();
    }

    /**
     * Starts the helper.
     */
    public void start(@NonNull Context context) {
        context.registerReceiver(usbConnectionReceiver, USB_INTENT_FILTER);
    }

    /**
     * Stops the helper.
     */
    public void stop(@NonNull Context context) {
        context.unregisterReceiver(usbConnectionReceiver);
    }

    public Map<String, UsbDevice> listDevices() {
        if (manager == null) return devicesMap;

        devicesMap.clear();
        devicesMap.putAll(manager.getDeviceList());

        return devicesMap;
    }

    @Nullable public UsbDevice getDevice(int index) {
        if (index < 0 || index >= devices.size()) return null;

        return devices.get(index);
    }

    /**
     * Returns number of currently connected serial communication capable devices.
     */
    public int getDevicesCount() {
        return devicesMap.size();
    }

    /**
     * Initiates communication with usb device with specified {@code deviceName} by requesting a permission to access
     * the device. If request is granted by the user the communication with the device will automatically be opened.
     *
     * @throws IllegalArgumentException if the device with specified {@code deviceName} is not connected.
     */
    public void requestPermission(@NonNull Context context, @NonNull String deviceName)
        throws IllegalArgumentException {
        final UsbDevice device = devicesMap.get(deviceName);
        if (device != null) {
            final PendingIntent pi = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
            manager.requestPermission(device, pi);

            return;
        }

        throw new IllegalArgumentException("Device " + deviceName + " is not connected!");
    }

    /**
     * Closes the communication with the currently connected usb device if any is connected. After calling this method
     * user will need to request permission to connect with the device.
     */
    public void close() {
        if (serialDevice != null) {
            done = true;
            serialDevice.close();
            readThread = null;
            communicationThread = null;
            serialDevice = null;
        }

        if (listener != null) listener.onDataTransferEnd();
    }

    /**
     * Stops reading incoming data from the connected usb device. Communication with the device is not finished, just
     * paused until {@link #resume()} is called.
     */
    public void pause() {
        paused = true;
    }

    /**
     * Starts reading incoming data from the connected usb device.
     */
    public void resume() {
        paused = false;
    }

    private class CommunicationThread extends Thread {

        @Override public void run() {
            final UsbDeviceConnection connection = manager.openDevice(device);
            if (UsbSerialDevice.isSupported(device)) {
                serialDevice = UsbSerialDevice.createUsbSerialDevice(device, connection);
                if (serialDevice != null) {
                    if (serialDevice.open()) {
                        if (listener != null) listener.onDataTransferStart();

                        // set serial connection parameters.
                        serialDevice.setBaudRate(BAUD_RATE);
                        serialDevice.setDataBits(UsbSerialInterface.DATA_BITS_8);
                        serialDevice.setStopBits(UsbSerialInterface.STOP_BITS_1);
                        serialDevice.setParity(UsbSerialInterface.PARITY_NONE);
                        serialDevice.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);

                        readThread = new ReadThread();
                        readThread.start();
                        if (done) done = false;

                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        // start reading data
                        serialDevice.read(new UsbSerialInterface.UsbReadCallback() {
                            @Override public void onReceivedData(byte[] data) {
                                circularBuffer.write(data);
                            }
                        });

                        // set sample rate and number of channels (into the void)
                        serialDevice.write(MSG_CONFIG_SAMPLE_RATE_AND_CHANNELS.getBytes());
                        // check which board are we connected to
                        serialDevice.write(MSG_BOARD_TYPE.getBytes());
                    } else {
                        LOGD(TAG, "PORT NOT OPEN");
                        Crashlytics.logException(new RuntimeException("Failed to open USB serial communication port!"));
                    }
                } else {
                    LOGD(TAG, "PORT IS NULL");
                    Crashlytics.logException(new RuntimeException("Failed to create USB serial device!"));
                }
            } else {
                LOGD(TAG, "DEVICE NOT SUPPORTED");
                Crashlytics.logException(new RuntimeException("Connected USB device is not supported!"));
            }
        }
    }

    @SuppressWarnings("WeakerAccess") void openDevice(@NonNull UsbDevice device) {
        final UsbDeviceConnection connection = manager.openDevice(device);
        if (UsbSerialDevice.isSupported(device)) {
            serialDevice = UsbSerialDevice.createUsbSerialDevice(device, connection);
            if (serialDevice != null) {
                if (serialDevice.open()) {
                    if (listener != null) listener.onDataTransferStart();

                    // set serial connection parameters.
                    serialDevice.setBaudRate(BAUD_RATE);
                    serialDevice.setDataBits(UsbSerialInterface.DATA_BITS_8);
                    serialDevice.setStopBits(UsbSerialInterface.STOP_BITS_1);
                    serialDevice.setParity(UsbSerialInterface.PARITY_NONE);
                    serialDevice.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);

                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    readThread = new ReadThread();
                    readThread.start();
                    if (done) done = false;

                    // start reading data
                    serialDevice.read(new UsbSerialInterface.UsbReadCallback() {
                        @Override public void onReceivedData(byte[] data) {
                            circularBuffer.write(data);
                        }
                    });

                    // set sample rate and number of channels (into the void)
                    serialDevice.write(MSG_CONFIG_SAMPLE_RATE_AND_CHANNELS.getBytes());
                    // check which board are we connected to
                    serialDevice.write(MSG_BOARD_TYPE.getBytes());
                } else {
                    LOGD(TAG, "PORT NOT OPEN");
                    Crashlytics.logException(new RuntimeException("Failed to open USB serial communication port!"));
                }
            } else {
                LOGD(TAG, "PORT IS NULL");
                Crashlytics.logException(new RuntimeException("Failed to create USB serial device!"));
            }
        } else {
            LOGD(TAG, "DEVICE NOT SUPPORTED");
            Crashlytics.logException(new RuntimeException("Connected USB device is not supported!"));
        }
    }

    // Refreshes the connected devices list with only the serial communication capable ones.
    @SuppressWarnings("WeakerAccess") void refreshDevices() {
        devicesMap.clear();
        devices.clear();

        final Map<String, UsbDevice> devices = new ArrayMap<>();
        if (manager.getDeviceList().size() > 0) {
            for (UsbDevice device : manager.getDeviceList().values()) {
                UsbInterface usbInterface;
                for (int i = 0; i < device.getInterfaceCount(); i++) {
                    usbInterface = device.getInterface(i);
                    if (usbInterface.getInterfaceClass() == UsbConstants.USB_CLASS_CDC_DATA) {
                        devices.put(device.getDeviceName(), device);
                    }
                }
            }
        }

        devicesMap.putAll(devices);
        this.devices.addAll(devices.values());
    }
}
