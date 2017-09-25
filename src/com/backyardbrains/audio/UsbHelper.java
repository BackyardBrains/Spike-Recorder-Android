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
import com.backyardbrains.data.DataProcessor;
import com.backyardbrains.utils.UsbUtils;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    private static String MSG_START_STREAM = "start:;";
    private static String MSG_STOP_STREAM = "h:;";
    private static String MSG_INFO = "?:;";
    private static String MSG_MAX_ = "max:;";
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
                    openDevice((UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE));
                } else {
                    if (listener != null) listener.onPermissionDenied();
                }
            } else {
                final boolean attached = UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action);
                if (!attached) disconnect();

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
        void onPermissionDenied();
    }

    private final ReceivesAudio service;
    private final UsbManager manager;
    private final UsbHelper.UsbListener listener;

    private UsbSerialDevice serialDevice;

    private final List<UsbDevice> devices = new ArrayList<>();
    private final Map<String, UsbDevice> devicesMap = new HashMap<>();

    private final CircularByteBuffer circBuffer = new CircularByteBuffer(BUFFER_SIZE);

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

    /**
     * Returns number of currently connected serial communication capable devices.
     */
    public int getDevicesCount() {
        return devicesMap.size();
    }

    public Map<String, UsbDevice> listDevices() {
        if (manager == null) return devicesMap;

        devicesMap.clear();
        devicesMap.putAll(manager.getDeviceList());

        return devicesMap;
    }

    public void connect(@NonNull Context context, @NonNull String deviceName) throws IllegalArgumentException {
        final UsbDevice device = devicesMap.get(deviceName);
        if (device != null) {
            final PendingIntent pi = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
            manager.requestPermission(device, pi);

            return;
        }

        throw new IllegalArgumentException("Device " + deviceName + " is not connected!");
    }

    @Nullable public UsbDevice getDevice(int index) {
        if (index < 0 || index >= devices.size()) return null;

        return devices.get(index);
    }

    private class ReadThread extends Thread {

        byte[] data;

        @Override public void run() {
            while (!done) {
                synchronized (service) {
                    data = new byte[circBuffer.peekSize()];
                    circBuffer.read(data, data.length, false);
                    service.receiveSampleStream(data);
                }
            }
        }
    }

    private boolean done;
    private DataProcessor processor = new SampleStreamProcessor();
    private ReadThread readThread;

    @SuppressWarnings("WeakerAccess") void openDevice(@NonNull UsbDevice device) {
        final UsbDeviceConnection connection = manager.openDevice(device);
        serialDevice = UsbSerialDevice.createUsbSerialDevice(device, connection);
        if (serialDevice != null) {
            if (serialDevice.open()) { //Set Serial Connection Parameters.
                if (listener != null) listener.onDataTransferStart();

                serialDevice.setBaudRate(BAUD_RATE);
                serialDevice.setDataBits(UsbSerialInterface.DATA_BITS_8);
                serialDevice.setStopBits(UsbSerialInterface.STOP_BITS_1);
                serialDevice.setParity(UsbSerialInterface.PARITY_NONE);
                serialDevice.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);

                serialDevice.read(new UsbSerialInterface.UsbReadCallback() {
                    @Override public void onReceivedData(byte[] data) {
                        circBuffer.write(data);
                    }
                });

                readThread = new ReadThread();
                readThread.start();

                if (done) done = false;

                //serialDevice.read(new UsbSerialInterface.UsbReadCallback() {
                //    @Override public void onReceivedData(byte[] bytes) {
                //        if (bytes != null && bytes.length > 0) {
                //            LOGD(TAG, "==============================");
                //            //LOGD(TAG, "BYTES: " + Arrays.toString(bytes));
                //            //LOGD(TAG, "1. USB - BEFORE sync");
                //            synchronized (service) {
                //                //LOGD(TAG, "2. USB - BEFORE receive");
                //                //LOGD(TAG, "BYTES: " + Arrays.toString(bytes));
                //                service.receiveAudio(ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder()));
                //                //LOGD(TAG, "8. USB - AFTER receive");
                //            }
                //            //LOGD(TAG, "9. USB - AFTER sync");
                //        }
                //    }
                //});
            } else {
                LOGD("SERIAL", "PORT NOT OPEN");
            }
        } else {
            LOGD("SERIAL", "PORT IS NULL");
        }
    }

    public void disconnect() {
        if (serialDevice != null) {
            done = true;
            serialDevice.close();
            readThread = null;
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
