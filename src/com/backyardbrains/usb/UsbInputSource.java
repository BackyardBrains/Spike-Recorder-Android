package com.backyardbrains.usb;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.audio.InputSource;
import com.backyardbrains.utils.SampleStreamUtils;
import com.backyardbrains.utils.SpikerBoxHardwareType;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>.
 */
public abstract class UsbInputSource extends InputSource implements BybUsbInterface {

    private static final String TAG = makeLogTag(UsbInputSource.class);

    private final SampleStreamProcessor processor;

    /**
     * Interface definition for a callback to be invoked when SpikerBox hardware type is detected after connection.
     */
    public interface OnSpikerBoxHardwareTypeDetectionListener {
        /**
         * Called when SpikerBox hardware type is detected.
         *
         * @param hardwareType Type of the connected SpikerBox hardware. One of {@link SpikerBoxHardwareType}.
         */
        void onSpikerBoxHardwareTypeDetected(@SpikerBoxHardwareType int hardwareType);
    }

    private OnSpikerBoxHardwareTypeDetectionListener listener;

    private @SpikerBoxHardwareType int hardwareType = SpikerBoxHardwareType.UNKNOWN;

    UsbInputSource(UsbDevice device, @NonNull OnSamplesReceivedListener listener) {
        super(listener);

        final SampleStreamProcessor.SampleStreamListener sampleStreamListener =
            new SampleStreamProcessor.SampleStreamListener() {
                @Override public void onSpikerBoxHardwareTypeDetected(@SpikerBoxHardwareType int hardwareType) {
                    LOGD(TAG, "BOARD TYPE: " + SampleStreamUtils.getSpikerBoxName(hardwareType));
                    setHardwareType(hardwareType);
                }

                @Override public void onMaxSampleRateAndNumOfChannelsReply(int maxSampleRate, int channelCount) {
                    LOGD(TAG, "SAMPLE RATE: " + maxSampleRate);
                    LOGD(TAG, "NUM OF CHANNELS: " + channelCount);
                    setSampleRate(maxSampleRate);
                    setChannelCount(channelCount);

                    processor.setChannelCount(channelCount);
                }
            };
        processor = new SampleStreamProcessor(sampleStreamListener, FILTERS);

        // check if can determine board type right away (through VID and PID)
        int vid = device.getVendorId();
        int pid = device.getProductId();
        if (vid == BYB_VENDOR_ID) {
            if (pid == BYB_PID_MUSCLE_SB_PRO) {
                setHardwareType(SpikerBoxHardwareType.MUSCLE_PRO);
            } else if (pid == BYB_PID_NEURON_SB_PRO) setHardwareType(SpikerBoxHardwareType.NEURON_PRO);
        }

        // set initial max sample rate and number of channels
        setSampleRate(SampleStreamUtils.SAMPLE_RATE);
        setChannelCount(1);
    }

    /**
     * Creates and returns {@link UsbInputSource} instance that is used to communicate with the connected USB device.
     */
    static UsbInputSource createUsbDevice(@NonNull UsbDevice device, @NonNull UsbDeviceConnection connection,
        @NonNull OnSamplesReceivedListener listener) {
        if (SerialDevice.isSupported(device)) {
            return SerialDevice.createUsbDevice(device, connection, listener);
        } else if (HIDDevice.isSupported(device)) {
            return HIDDevice.createUsbDevice(device, connection, listener);
        } else {
            return null;
        }
    }

    /**
     * Returns whether specified {@code device} is supported device for this app.
     */
    static boolean isSupported(@NonNull UsbDevice device) {
        return SerialDevice.isSupported(device) || HIDDevice.isSupported(device);
    }

    /**
     * Register a callback to be invoked when connected SpikerBox hardware type is detected.
     *
     * @param listener The callback that will be run. This value may be {@code null}.
     */
    public void setOnSpikerBoxHardwareTypeDetectionListener(
        @Nullable OnSpikerBoxHardwareTypeDetectionListener listener) {
        this.listener = listener;
    }

    /**
     * {@inheritDoc}
     */
    @Override protected void setChannelCount(int channelCount) {
        super.setChannelCount(channelCount);

        processor.setChannelCount(channelCount);
    }

    /**
     * {@inheritDoc}
     */
    @Override protected short[] processIncomingData(byte[] data) {
        return processor.process(data);
    }

    /**
     * {@inheritDoc}
     */
    @Override protected int getType() {
        return Type.USB;
    }

    /**
     * Returns SpikerBox hardware type of the input source.
     */
    public @SpikerBoxHardwareType int getHardwareType() {
        return hardwareType;
    }

    /**
     * Sets SpikerBox hardware type for the input source.
     */
    private void setHardwareType(int hardwareType) {
        if (this.hardwareType == hardwareType) return;

        this.hardwareType = hardwareType;

        if (listener != null) listener.onSpikerBoxHardwareTypeDetected(hardwareType);
    }
}
