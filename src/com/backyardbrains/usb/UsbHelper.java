package com.backyardbrains.usb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import com.backyardbrains.audio.AudioService;
import com.backyardbrains.audio.InputSource;
import com.crashlytics.android.Crashlytics;
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

    private static final IntentFilter USB_INTENT_FILTER;

    static {
        USB_INTENT_FILTER = new IntentFilter();
        USB_INTENT_FILTER.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        USB_INTENT_FILTER.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        USB_INTENT_FILTER.addAction(ACTION_USB_PERMISSION);
    }

    // Receives broadcast sent by Android related to USB device interface
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

    private final InputSource.OnSamplesReceivedListener service;
    private final UsbManager manager;
    private final UsbHelper.UsbListener listener;

    private UsbInputSource usbDevice;
    private UsbDevice device;

    private final List<UsbDevice> devices = new ArrayList<>();
    private final Map<String, UsbDevice> devicesMap = new HashMap<>();

    private CommunicationThread communicationThread;

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
     * Stops reading incoming data from the connected usb device. Communication with the device is not finished, just
     * paused until {@link #resume()} is called.
     */
    public void pause() {
        if (usbDevice != null) usbDevice.pause();
    }

    /**
     * Starts reading incoming data from the connected usb device.
     */
    public void resume() {
        if (usbDevice != null) usbDevice.resume();
    }

    /**
     * Stops the helper.
     */
    public void stop(@NonNull Context context) {
        context.unregisterReceiver(usbConnectionReceiver);
    }

    /**
     * Returns USB device for the specified {@code index} or {@code null} if there are no connected devices or index is
     * out of range.
     */
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
     * Returns currently connected SpikerBox device, or {@code null} if none is connected.
     */
    @Nullable public UsbInputSource getUsbDevice() {
        return usbDevice;
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
     * caller will need to request permission to connect with the device.
     */
    public void close() {
        if (usbDevice != null) {
            usbDevice.stop();
            usbDevice = null;
        }
        communicationThread = null;

        if (listener != null) listener.onDataTransferEnd();
    }

    private class CommunicationThread extends Thread {

        @Override public void run() {
            final UsbDeviceConnection connection = manager.openDevice(device);
            if (UsbInputSource.isSupported(device)) {
                usbDevice = UsbInputSource.createUsbDevice(device, connection, service);
                if (usbDevice != null) {
                    if (usbDevice.open()) {
                        if (listener != null) listener.onDataTransferStart();

                        usbDevice.start();
                    } else {
                        LOGD(TAG, "PORT NOT OPEN");
                        Crashlytics.logException(new RuntimeException("Failed to open USB communication port!"));
                    }
                } else {
                    LOGD(TAG, "PORT IS NULL");
                    Crashlytics.logException(new RuntimeException("Failed to create USB device!"));
                }
            } else {
                LOGD(TAG, "DEVICE NOT SUPPORTED");
                Crashlytics.logException(new RuntimeException("Connected USB device is not supported!"));
            }
        }
    }

    // Refreshes the connected devices list with only supported serial and hid ones.
    private void refreshDevices() {
        devicesMap.clear();
        devices.clear();

        final Map<String, UsbDevice> devices = new ArrayMap<>();
        if (manager.getDeviceList().size() > 0) {
            for (UsbDevice device : manager.getDeviceList().values()) {
                if (UsbInputSource.isSupported(device)) devices.put(device.getDeviceName(), device);
            }
        }

        devicesMap.putAll(devices);
        this.devices.addAll(devices.values());
    }
}
