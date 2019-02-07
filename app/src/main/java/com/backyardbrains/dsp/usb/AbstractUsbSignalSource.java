package com.backyardbrains.dsp.usb;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.dsp.AbstractSignalSource;
import com.backyardbrains.dsp.SamplesWithEvents;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.Benchmark;
import com.backyardbrains.utils.JniUtils;
import com.backyardbrains.utils.SampleStreamUtils;
import com.backyardbrains.utils.SpikerBoxHardwareType;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * Wrapper for {@link UsbDevice} class. Device can only be one of supported BYB usb devices.
 *
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public abstract class AbstractUsbSignalSource extends AbstractSignalSource implements UsbSignalSource {

    @SuppressWarnings("WeakerAccess") static final String TAG = makeLogTag(AbstractUsbSignalSource.class);

    private static final int MAX_SAMPLES_PER_CHANNEL = 5000; // .5 secs of signal at 10000 Hz

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

    AbstractUsbSignalSource(@NonNull UsbDevice device) {
        super(SampleStreamUtils.SAMPLE_RATE, AudioUtils.DEFAULT_CHANNEL_COUNT, MAX_SAMPLES_PER_CHANNEL);

        this.device = device;

        // check if we can determine board type right away (through VID and PID)
        setHardwareType(getHardwareType(device));
    }

    /**
     * Creates and returns {@link AbstractUsbSignalSource} instance that is used to communicate with the connected USB
     * device.
     */
    static AbstractUsbSignalSource createUsbDevice(@NonNull UsbDevice device, @NonNull UsbDeviceConnection connection) {
        if (SerialSignalSource.isSupported(device)) {
            return SerialSignalSource.createUsbDevice(device, connection);
        } else if (HIDSignalSource.isSupported(device)) {
            return HIDSignalSource.createUsbDevice(device, connection);
        } else {
            return null;
        }
    }

    /**
     * Returns whether specified {@code device} is supported device for this app.
     */
    static boolean isSupported(@NonNull UsbDevice device) {
        return SerialSignalSource.isSupported(device) || HIDSignalSource.isSupported(device);
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

    @CallSuper @Override public void start() {
        LOGD(TAG, "start()");
        startReadingStream();
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

    private final Benchmark benchmark = new Benchmark("PROCESS_SAMPLE_STREAM_TEST").warmUp(1000)
        .sessions(10)
        .measuresPerSession(2000)
        .logBySession(false)
        .listener(() -> {
            //EventBus.getDefault().post(new ShowToastEvent("PRESS BACK BUTTON!!!!"));
        });

    /**
     * {@inheritDoc}
     */
    @Override public final void processIncomingData(@NonNull SamplesWithEvents outData, byte[] inData,
        int inDataLength) {
        //benchmark.start();
        JniUtils.processSampleStream(outData, inData, inDataLength, this);
        //benchmark.end();
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

        LOGD(TAG, "BOARD TYPE: " + SampleStreamUtils.getSpikerBoxName(hardwareType));

        this.hardwareType = hardwareType;

        if (onSpikerBoxHardwareTypeDetectionListener != null) {
            onSpikerBoxHardwareTypeDetectionListener.onHardwareTypeDetected(hardwareType);
        }
    }
}
