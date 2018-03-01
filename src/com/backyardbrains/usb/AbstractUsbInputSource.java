package com.backyardbrains.usb;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.audio.AbstractInputSource;
import com.backyardbrains.utils.SampleStreamUtils;
import com.backyardbrains.utils.SpikerBoxHardwareType;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * Wrapper for {@link UsbDevice} class. Device can only be one of supported BYB usb devices.
 *
 * @author Tihomir Leka <ticapeca at gmail.com>.
 */
public abstract class AbstractUsbInputSource extends AbstractInputSource implements UsbInputSource {

    @SuppressWarnings("WeakerAccess") static final String TAG = makeLogTag(AbstractUsbInputSource.class);

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

    /**
     * Interface definition for a callback to be invoked when SpikerBox event message is sent.
     */
    public interface OnSpikerBoxEventMessageReceivedListener {
        /**
         * Called when SpikerBox sends an event message.
         *
         * @param event Event sent by SpikerBox.
         * @param sampleIndex Index of the sample in the current batch where event occurred.
         */
        void onEventReceived(@NonNull String event, int sampleIndex);
    }

    @SuppressWarnings("WeakerAccess") OnSpikerBoxEventMessageReceivedListener onSpikerBoxEventMessageReceivedListener;

    AbstractUsbInputSource(@NonNull UsbDevice device, @Nullable final OnSamplesReceivedListener listener) {
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

                @Override public void onEventReceived(@NonNull String event, int sampleIndex) {
                    LOGD(TAG, "EVENT: " + event);
                    if (onSpikerBoxEventMessageReceivedListener != null) {
                        onSpikerBoxEventMessageReceivedListener.onEventReceived(event, sampleIndex);
                    }
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
     * Creates and returns {@link AbstractUsbInputSource} instance that is used to communicate with the connected USB
     * device.
     */
    static AbstractUsbInputSource createUsbDevice(@NonNull UsbDevice device, @NonNull UsbDeviceConnection connection,
        @Nullable OnSamplesReceivedListener listener) {
        if (SerialInputSource.isSupported(device)) {
            return SerialInputSource.createUsbDevice(device, connection, listener);
        } else if (HIDInputSource.isSupported(device)) {
            return HIDInputSource.createUsbDevice(device, connection, listener);
        } else {
            return null;
        }
    }

    /**
     * Returns whether specified {@code device} is supported device for this app.
     */
    static boolean isSupported(@NonNull UsbDevice device) {
        return SerialInputSource.isSupported(device) || HIDInputSource.isSupported(device);
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

    @CallSuper @Override protected void onInputStop() {
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
     * Registers a callback to be invoked when connected SpikerBox sends an event message.
     *
     * @param listener The callack that will be run. This value may be {@code null}.
     */
    public void setOnSpikerBoxEventMessageReceivedListener(@Nullable OnSpikerBoxEventMessageReceivedListener listener) {
        onSpikerBoxEventMessageReceivedListener = listener;
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
    @NonNull @Override protected final short[] processIncomingData(byte[] data) {
        return processor.process(data);
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
