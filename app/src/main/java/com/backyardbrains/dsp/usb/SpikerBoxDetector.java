package com.backyardbrains.dsp.usb;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import com.backyardbrains.dsp.SignalData;
import com.backyardbrains.dsp.SignalSource;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.JniUtils;
import com.backyardbrains.utils.SpikerBoxHardwareType;
import com.crashlytics.android.Crashlytics;
import java.util.Map;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * Helper class used for detection of hardware type of the connected USB device.
 *
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
class SpikerBoxDetector {

    private static final String TAG = makeLogTag(SpikerBoxDetector.class);

    private static final int MAX_ATTEMPTS = 10;
    private static final int BUFFER_SIZE = 5000;

    private final Map<String, DetectionThread> detectionThreadMap = new ArrayMap<>();

    private UsbManager manager;
    private OnSpikerBoxDetectionListener listener;

    /**
     * Listens for detection of the connected usb device hardware type.
     */
    interface OnSpikerBoxDetectionListener {
        /**
         * Triggered when hardware type of connected usb device is detected.
         */
        void onSpikerBoxDetected(@NonNull UsbDevice device, @SpikerBoxHardwareType int hardwareType);

        /**
         * Triggered when hardware type of the connected usb device couldn't be detected.
         */
        void onSpikerBoxDetectionFailure(@NonNull UsbDevice device);

        /**
         * Triggered when error occures during detection of the connected usb device hardware type.
         */
        void onSpikerBoxDetectionError(@NonNull String deviceName, @NonNull String reason);
    }

    private SpikerBoxDetector(@Nullable UsbManager manager, @Nullable OnSpikerBoxDetectionListener listener) {
        this.manager = manager;
        this.listener = listener;
    }

    private static SpikerBoxDetector detector;

    /**
     * Returns singleton instance of the {@link SpikerBoxDetector} class.
     */
    static SpikerBoxDetector get(@Nullable UsbManager manager, @Nullable OnSpikerBoxDetectionListener listener) {
        if (detector == null) {
            synchronized (SpikerBoxDetector.class) {
                if (detector == null) detector = new SpikerBoxDetector(manager, listener);
            }
        }

        return detector;
    }

    /**
     * Starts the hardware type detection process for the provided usb {@code device} in the background thread.
     */
    void startDetection(@NonNull UsbDevice device) {
        if (manager != null) {
            final UsbDeviceConnection connection = manager.openDevice(device);
            final AbstractUsbSignalSource usbDevice = AbstractUsbSignalSource.createUsbDevice(device, connection);
            if (usbDevice != null) {
                usbDevice.setProcessor(new SignalSource.Processor() {

                    private SignalData signalData =
                        new SignalData(usbDevice.getChannelCount(), BUFFER_SIZE, AudioUtils.DEFAULT_BITS_PER_SAMPLE);

                    @Override public void onDataReceived(@NonNull byte[] data, int length) {
                        JniUtils.processSampleStream(signalData, data, length, usbDevice);
                    }

                    @Override public void onSampleRateChanged(int sampleRate) {
                    }

                    @Override public void onChannelCountChanged(int channelCount) {
                    }

                    @Override public void onBitsPerSampleChanged(int bitsPerSample) {
                    }
                });
                // For some devices we set hardware type on creation just by checking VID and PID
                if (usbDevice.getHardwareType() != SpikerBoxHardwareType.UNKNOWN && listener != null) {
                    listener.onSpikerBoxDetected(device, usbDevice.getHardwareType());
                    return;
                }

                if (usbDevice.open()) {
                    DetectionThread detectionThread = new DetectionThread(usbDevice);
                    detectionThreadMap.put(device.getDeviceName(), detectionThread);
                    detectionThread.start();
                } else {
                    if (listener != null) {
                        listener.onSpikerBoxDetectionError(device.getDeviceName(),
                            "Failed to open USB communication port!");
                    }
                    LOGD(TAG, "PORT NOT OPEN");
                    Crashlytics.logException(new RuntimeException("Failed to open USB communication port!"));
                }
            } else {
                if (listener != null) {
                    listener.onSpikerBoxDetectionError(device.getDeviceName(), "Failed to connect to USB device!");
                }
                LOGD(TAG, "PORT IS NULL");
                Crashlytics.logException(new RuntimeException("Failed to connect to USB device!"));
            }
        } else {
            if (listener != null) {
                listener.onSpikerBoxDetectionError(device.getDeviceName(), "Connected USB device is not supported!");
            }
            LOGD(TAG, "DEVICE NOT SUPPORTED");
            Crashlytics.logException(new RuntimeException("Connected USB device is not supported!"));
        }
    }

    /**
     * Cancels the hardware type detection process for the usb device with the specified {@code deviceName).
     */
    void cancelDetection(@NonNull String deviceName) {
        final DetectionThread thread = detectionThreadMap.get(deviceName);
        if (thread != null) thread.cancel();
    }

    // Informs listener that the hardware type of the connected usb device was detected.
    @SuppressWarnings("WeakerAccess") void deviceDetectionSuccess(@NonNull UsbDevice device,
        @SpikerBoxHardwareType int hardwareType) {
        detectionThreadMap.remove(device.getDeviceName());
        // inform listener that we managed to detect the hardware type
        if (listener != null) listener.onSpikerBoxDetected(device, hardwareType);
    }

    // Informs listener that the hardware type of the connected usb device couldn't be detected.
    @SuppressWarnings("WeakerAccess") void deviceDetectionFailure(@NonNull UsbDevice device) {
        detectionThreadMap.remove(device.getDeviceName());
        // inform listener that we couldn't detect the hardware type
        if (listener != null) listener.onSpikerBoxDetectionFailure(device);
    }

    /**
     * Thread used for detection of the connected SpikerBox hardware type. Tries to get the information about the
     * connected SpikerBox device hardware type for 10 times before informing listener about the detection failure.
     */
    class DetectionThread extends Thread {

        private final String TAG = makeLogTag(DetectionThread.class);

        private boolean working = true;
        private boolean canceled = false;
        private int counter = 0;

        private UsbSignalSource usbDevice;

        DetectionThread(@NonNull AbstractUsbSignalSource usbDevice) {
            this.usbDevice = usbDevice;
        }

        /**
         * Clears resources.
         */
        void cancel() {
            canceled = true;
            working = false;
        }

        @Override public void run() {
            if (usbDevice != null) usbDevice.start();

            while (working) {
                if (!canceled && usbDevice != null) {
                    usbDevice.checkHardwareType();
                    LOGD(TAG, counter + ". DETECTION ATTEMPT FOR DEVICE: " + usbDevice.getUsbDevice().getDeviceName());
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (usbDevice != null && usbDevice.getHardwareType() != SpikerBoxHardwareType.UNKNOWN) {
                    // we managed to detect the hardware type
                    if (!canceled) {
                        deviceDetectionSuccess(usbDevice.getUsbDevice(), usbDevice.getHardwareType());
                        usbDevice.stop();
                        usbDevice = null;
                    }
                    return;
                }

                if (++counter > MAX_ATTEMPTS) working = false;
            }

            // we couldn't detect the SpikerBox hardware type so inform listener about the failure
            if (usbDevice != null) {
                if (!canceled) deviceDetectionFailure(usbDevice.getUsbDevice());
                usbDevice.stop();
                usbDevice = null;
            }
        }
    }
}