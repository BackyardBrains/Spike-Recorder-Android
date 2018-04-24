package com.backyardbrains.usb;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.data.processing.AbstractSampleSource;
import com.backyardbrains.data.processing.DataProcessor;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.SampleStreamUtils;
import com.backyardbrains.utils.SpikerBoxHardwareType;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * Wrapper for {@link UsbDevice} class. Device can only be one of supported BYB usb devices.
 *
 * @author Tihomir Leka <ticapeca at gmail.com>.
 */
public abstract class AbstractUsbSampleSource extends AbstractSampleSource implements UsbSampleSource {

    @SuppressWarnings("WeakerAccess") static final String TAG = makeLogTag(AbstractUsbSampleSource.class);

    @SuppressWarnings("WeakerAccess") final SampleStreamProcessor processor;
    private final UsbDevice device;

    /**
     * Interface definition for a callback to be invoked when SpikerBox hardware type is detected after connection.
     */
    public interface OnSpikerBoxHardwareTypeDetectionListener {
        /**
         * Called when SpikerBox hardware type is detected.
         *
         * @param hardwareType Type of the connected SpikerBox hardware. One of {@link SpikerBoxHardwareType}.
         */
        void onHardwareTypeDetected(@SpikerBoxHardwareType int hardwareType);
    }

    private OnSpikerBoxHardwareTypeDetectionListener onSpikerBoxHardwareTypeDetectionListener;

    private @SpikerBoxHardwareType int hardwareType = SpikerBoxHardwareType.UNKNOWN;

    AbstractUsbSampleSource(@NonNull UsbDevice device, @Nullable final OnSamplesReceivedListener listener) {
        super(listener);

        this.device = device;

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
        setHardwareType(getHardwareType(device));

        // set initial max sample rate and number of channels
        setSampleRate(SampleStreamUtils.SAMPLE_RATE);
        setChannelCount(1);
    }

    /**
     * Creates and returns {@link AbstractUsbSampleSource} instance that is used to communicate with the connected USB
     * device.
     */
    static AbstractUsbSampleSource createUsbDevice(@NonNull UsbDevice device, @NonNull UsbDeviceConnection connection,
        @Nullable OnSamplesReceivedListener listener) {
        if (SerialSampleSource.isSupported(device)) {
            return SerialSampleSource.createUsbDevice(device, connection, listener);
        } else if (HIDSampleSource.isSupported(device)) {
            return HIDSampleSource.createUsbDevice(device, connection, listener);
        } else {
            return null;
        }
    }

    /**
     * Returns whether specified {@code device} is supported device for this app.
     */
    static boolean isSupported(@NonNull UsbDevice device) {
        return SerialSampleSource.isSupported(device) || HIDSampleSource.isSupported(device);
    }

    /**
     * Returns SpikerBox hardware type for the specified {@code device} by checking VID and PID. If it's not possible to
     * determine hardware type {@link SpikerBoxHardwareType#UNKNOWN} is returned.
     */
    static @SpikerBoxHardwareType int getHardwareType(@NonNull UsbDevice device) {
        int vid = device.getVendorId();
        int pid = device.getProductId();
        if (vid == BYB_VENDOR_ID) {
            if (pid == BYB_PID_MUSCLE_SB_PRO) {
                return SpikerBoxHardwareType.MUSCLE_PRO;
            } else if (pid == BYB_PID_NEURON_SB_PRO) return SpikerBoxHardwareType.NEURON_PRO;
        }

        return SpikerBoxHardwareType.UNKNOWN;
    }

    /**
     * Starts reading data from the usb endpoint.
     */
    protected abstract void startReadingStream();

    /**
     * {@inheritDoc}
     *
     * <p><em>Derived classes must call through to the super class's implementation of this method.</em></p>
     */
    @CallSuper @Override protected void onInputStart() {
        LOGD(TAG, "onInputStart()");
        startReadingStream();
    }

    @Override protected void onInputStop() {
        LOGD(TAG, "onInputStart()");
    }

    /**
     * Registers a callback to be invoked when connected SpikerBox hardware type is detected.
     *
     * @param listener The callback that will be run. This value may be {@code null}.
     */
    public void setOnSpikerBoxHardwareTypeDetectionListener(
        @Nullable OnSpikerBoxHardwareTypeDetectionListener listener) {
        onSpikerBoxHardwareTypeDetectionListener = listener;
    }

    /**
     * {@inheritDoc}
     */
    @Override protected final void setChannelCount(int channelCount) {
        super.setChannelCount(channelCount);

        processor.setChannelCount(channelCount);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull @Override protected final DataProcessor.SamplesWithMarkers processIncomingData(byte[] data,
        long lastByteIndex) {
        DataProcessor.SamplesWithMarkers swm = processor.process(data);
        swm.lastSampleIndex = AudioUtils.getSampleCount(lastByteIndex);
        return swm;
    }

    /**
     * {@inheritDoc}
     */
    @Override public final int getType() {
        return Type.USB;
    }

    /**
     * Returns wrapped {@link UsbDevice}.
     */
    @Override public UsbDevice getUsbDevice() {
        return device;
    }

    /**
     * {@inheritDoc}
     */
    @Override public @SpikerBoxHardwareType int getHardwareType() {
        return hardwareType;
    }

    /**
     * Sets SpikerBox hardware type for the input source.
     */
    @SuppressWarnings("WeakerAccess") void setHardwareType(int hardwareType) {
        if (this.hardwareType == hardwareType) return;

        this.hardwareType = hardwareType;

        if (onSpikerBoxHardwareTypeDetectionListener != null) {
            onSpikerBoxHardwareTypeDetectionListener.onHardwareTypeDetected(hardwareType);
        }
    }
}
