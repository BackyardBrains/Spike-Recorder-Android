package com.backyardbrains.dsp.usb;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;

import com.backyardbrains.dsp.AbstractSignalSource;
import com.backyardbrains.dsp.SignalData;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.ExpansionBoardType;
import com.backyardbrains.utils.HumanSpikerBoardState;
import com.backyardbrains.utils.JniUtils;
import com.backyardbrains.utils.SampleStreamUtils;
import com.backyardbrains.utils.SpikerBoxHardwareType;

import java.util.Set;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * Wrapper for {@link UsbDevice} class. Device can only be one of supported BYB usb devices.
 *
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public abstract class AbstractUsbSignalSource extends AbstractSignalSource implements UsbSignalSource {

    @SuppressWarnings("WeakerAccess")
    static final String TAG = makeLogTag(AbstractUsbSignalSource.class);

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

    /**
     * Interface definition for a callback to be invoked when expansion board type is detected after it's connection.
     */
    public interface OnExpansionBoardTypeDetectionListener {
        /**
         * Called when expansion board hardware type is detected.
         *
         * @param expansionBoardType Type of the connected expansion board. One of {@link ExpansionBoardType}.
         */
        void onExpansionBoardTypeDetected(@ExpansionBoardType int expansionBoardType);
    }

    /**
     * Interface definition for a callback to be invoked when human spikerBox board state is detected .
     */
    public interface onHumanSpikerP300StateListener {
        /**
         * Called when human spikerBox board state is detected.
         *
         * @param humanSpikerBoardState Type of the state.
         */
        void onHumanSpikerP300StateDetected(@HumanSpikerBoardState int humanSpikerBoardState);
    }

    /**
     * Interface definition for a callback to be invoked when human spikerBox audio state is detected .
     */
    public interface onHumanSpikerP300AudioStateListener {
        /**
         * Called when human spikerBox audio state is detected.
         *
         * @param humanSpikerAudioState Type of the state.
         */
        void onHumanSpikerP300AudioStateDetected(@HumanSpikerBoardState int humanSpikerAudioState);
    }

    /**
     * Interface definition for a callback to be invoked when usb signal source is disconnected.
     */
    public interface OnUsbSignalSourceDisconnectListener {
        /**
         * Called when usb signal source is disconnected.
         */
        void onDisconnected();
    }

    private Set<OnSpikerBoxHardwareTypeDetectionListener> onSpikerBoxHardwareTypeDetectionListeners;
    private Set<OnExpansionBoardTypeDetectionListener> onOnExpansionBoardTypeDetectionListeners;
    private Set<onHumanSpikerP300StateListener> onHumanSpikerP300StateDetectionListener;
    private Set<onHumanSpikerP300AudioStateListener> onHumanSpikerP300AudioStateListener;
    private OnUsbSignalSourceDisconnectListener onUsbSignalSourceDisconnectListener;

    private @SpikerBoxHardwareType int hardwareType = SpikerBoxHardwareType.UNKNOWN;
    private @ExpansionBoardType int expansionBoardType = ExpansionBoardType.NONE;
    private @HumanSpikerBoardState int humanSpikerBoardp300State = HumanSpikerBoardState.OFF;
    private @HumanSpikerBoardState int humanSpikerBoardpAudioState = HumanSpikerBoardState.OFF;

    private boolean disconnecting;

    AbstractUsbSignalSource(@NonNull UsbDevice device) {
        super(SampleStreamUtils.DEFAULT_SAMPLE_RATE, AudioUtils.DEFAULT_CHANNEL_COUNT,
                AudioUtils.getBitsPerSample(AudioUtils.DEFAULT_BITS_PER_SAMPLE));

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
     * Checks whether HID signal source is currently disconnecting from host.
     */
    public boolean isDisconnecting() {
        return disconnecting;
    }

    /**
     * Returns SpikerBox hardware type for the specified {@code device} by checking VID and PID. If it's not possible to
     * determine hardware type {@link SpikerBoxHardwareType#UNKNOWN} is returned.
     */
    static @SpikerBoxHardwareType int getHardwareType(@NonNull UsbDevice device) {
        int vid = device.getVendorId();
        int pid = device.getProductId();
        if (vid == BYB_VENDOR_ID) {
            if (pid == BYB_HUMAN_SB_PRO_ID1) {
                return SpikerBoxHardwareType.HUMAN_PRO;
            } else if (pid == BYB_PID_MUSCLE_SB_PRO) {
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
     * Stops reading data from usb endpoint.
     */
    abstract protected void stopReadingStream();

    /**
     * {@inheritDoc}
     */
    @Override
    public final void start() {
        LOGD(TAG, "start()");
        startReadingStream();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void stop() {
        disconnecting = true;
        AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
            stopReadingStream();
            disconnecting = false;

            if (onUsbSignalSourceDisconnectListener != null)
                onUsbSignalSourceDisconnectListener.onDisconnected();
        });
    }

    /**
     * Adds a listener to the set of listeners to be invoked when connected SpikerBox hardware type is detected.
     *
     * @param listener the listener to be added to the current set of listeners.
     */
    public void addOnSpikerBoxHardwareTypeDetectionListener(
            @NonNull OnSpikerBoxHardwareTypeDetectionListener listener) {
        if (onSpikerBoxHardwareTypeDetectionListeners == null) {
            onSpikerBoxHardwareTypeDetectionListeners = new ArraySet<>();
        }
        onSpikerBoxHardwareTypeDetectionListeners.add(listener);
        // trigger detection callback for newly added listener so it's aware what board is connected
        listener.onHardwareTypeDetected(hardwareType);
    }

    /**
     * Removes a listener from the set listening to SpikerBox type detection.
     *
     * @param listener the listener to be removed from the current set of listeners.
     */
    public void removeOnSpikerBoxHardwareTypeDetectionListener(
            @NonNull OnSpikerBoxHardwareTypeDetectionListener listener) {
        if (onSpikerBoxHardwareTypeDetectionListeners == null) return;
        onSpikerBoxHardwareTypeDetectionListeners.remove(listener);
        if (onSpikerBoxHardwareTypeDetectionListeners.size() == 0)
            onSpikerBoxHardwareTypeDetectionListeners = null;
    }

    /**
     * Adds a listener to the set of listeners to be invoked when connected expansion board type is detected.
     *
     * @param listener the listener to be added to the current set of listeners.
     */
    public void addOnExpansionBoardTypeDetectionListener(@NonNull OnExpansionBoardTypeDetectionListener listener) {
        if (onOnExpansionBoardTypeDetectionListeners == null) {
            onOnExpansionBoardTypeDetectionListeners = new ArraySet<>();
        }
        onOnExpansionBoardTypeDetectionListeners.add(listener);
        // trigger detection callback for newly added listener so it's aware what expansion board is connected
        listener.onExpansionBoardTypeDetected(expansionBoardType);
    }

    /**
     * Removes a listener from the set listening to expansion board type detection.
     *
     * @param listener the listener to be removed from the current set of listeners.
     */
    public void removeOnExpansionBoardTypeDetectionListener(@NonNull OnExpansionBoardTypeDetectionListener listener) {
        if (onOnExpansionBoardTypeDetectionListeners == null) return;
        onOnExpansionBoardTypeDetectionListeners.remove(listener);
        if (onOnExpansionBoardTypeDetectionListeners.size() == 0)
            onOnExpansionBoardTypeDetectionListeners = null;
    }

    /**
     * Adds a listener to the set of listeners to be invoked when connected human board state is detected.
     *
     * @param listener the listener to be added to the current set of listeners.
     */
    public void addOnHumanSpikerBoxp300State(@NonNull onHumanSpikerP300StateListener listener) {
        if (onHumanSpikerP300StateDetectionListener == null) {
            onHumanSpikerP300StateDetectionListener = new ArraySet<>();
        }
        onHumanSpikerP300StateDetectionListener.add(listener);
        listener.onHumanSpikerP300StateDetected(humanSpikerBoardp300State);
    }

    public void removeOnHumanSpikerBoxp300State(@NonNull onHumanSpikerP300StateListener listener) {
        if (onHumanSpikerP300StateDetectionListener == null) return;
        onHumanSpikerP300StateDetectionListener.remove(listener);
        if (onHumanSpikerP300StateDetectionListener.size() == 0)
            onHumanSpikerP300StateDetectionListener = null;
    }

    public void addOnHumanSpikerP300AudioStateListener(@NonNull onHumanSpikerP300AudioStateListener listener) {
        if (onHumanSpikerP300AudioStateListener == null) {
            onHumanSpikerP300AudioStateListener = new ArraySet<>();
        }
        onHumanSpikerP300AudioStateListener.add(listener);
        listener.onHumanSpikerP300AudioStateDetected(humanSpikerBoardpAudioState);
    }

    public void removeOnHumanSpikerP300AudioStateListener(@NonNull onHumanSpikerP300AudioStateListener listener) {
        if (onHumanSpikerP300AudioStateListener == null) return;
        onHumanSpikerP300AudioStateListener.remove(listener);
        if (onHumanSpikerP300AudioStateListener.size() == 0)
            onHumanSpikerP300AudioStateListener = null;
    }


    /**
     * Sets a listener to be invoked when usb signal source is disconnected.
     *
     * @param listener The callback that will be run. This value may be {@code null}.
     */
    public void setOnUsbSignalSourceDisconnectListener(@Nullable OnUsbSignalSourceDisconnectListener listener) {
        onUsbSignalSourceDisconnectListener = listener;
    }

    //private final Benchmark benchmark = new Benchmark("PROCESS_SAMPLE_STREAM_TEST").warmUp(1000)
    //    .sessions(10)
    //    .measuresPerSession(2000)
    //    .logBySession(false);

    /**
     * {@inheritDoc}
     */
    @Override
    public final void processIncomingData(@NonNull SignalData outData, byte[] inData, int inDataLength) {
        //benchmark.start();
        JniUtils.processSampleStream(hardwareType, outData, inData, inDataLength, this);
        //benchmark.end();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int getType() {
        return Type.USB;
    }

    /**
     * Returns wrapped {@link UsbDevice}.
     */
    @Override
    public UsbDevice getUsbDevice() {
        return device;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @SpikerBoxHardwareType int getHardwareType() {
        return hardwareType;
    }

    /**
     * Sets SpikerBox hardware type for the input source.
     */
    @SuppressWarnings("WeakerAccess")
    void setHardwareType(int hardwareType) {
        if (this.hardwareType == hardwareType) return;

        LOGD(TAG, "HARDWARE TYPE: " + SampleStreamUtils.getSpikerBoxHardwareName(hardwareType));
        if (hardwareType == SpikerBoxHardwareType.HUMAN_PRO) {

            setChannelCount(2);
            setSampleRate(SampleStreamUtils.SAMPLE_RATE_5000);
            setBitsPerSample(14);
        }
        this.hardwareType = hardwareType;
        if (onSpikerBoxHardwareTypeDetectionListeners != null) {
            for (OnSpikerBoxHardwareTypeDetectionListener listener : onSpikerBoxHardwareTypeDetectionListeners) {
                listener.onHardwareTypeDetected(hardwareType);
            }
        }
    }

    void setHumanSpikerBoardState(int humanSpikerBoardp300State) {
        this.humanSpikerBoardp300State = humanSpikerBoardp300State;
        if (onHumanSpikerP300StateDetectionListener != null) {
            for (onHumanSpikerP300StateListener listener : onHumanSpikerP300StateDetectionListener) {
                listener.onHumanSpikerP300StateDetected(humanSpikerBoardp300State);
            }
        }
    }

    void setHumanSpikerBoardAudioState(int humanSpikerBoardpAudioState) {
        this.humanSpikerBoardpAudioState = humanSpikerBoardpAudioState;

        if (onHumanSpikerP300AudioStateListener != null) {
            for (onHumanSpikerP300AudioStateListener listener : onHumanSpikerP300AudioStateListener) {
                listener.onHumanSpikerP300AudioStateDetected(humanSpikerBoardpAudioState);
            }
        }
    }

    /**
     * Sets connected SpikerBox expansion board type for the input source.
     */
    void setExpansionBoardType(int expansionBoardType) {
        if (this.expansionBoardType == expansionBoardType) return;

        LOGD(TAG, "EXPANSION BOARD TYPE: " + SampleStreamUtils.getExpansionBoardName(expansionBoardType));

        this.expansionBoardType = expansionBoardType;

        // expansion board detected,
        // let's update sample rate and channel count depending on the board type
        prepareForExpansionBoard(expansionBoardType);

        if (onOnExpansionBoardTypeDetectionListeners != null) {
            for (OnExpansionBoardTypeDetectionListener listener : onOnExpansionBoardTypeDetectionListeners) {
                listener.onExpansionBoardTypeDetected(expansionBoardType);
            }
        }
    }

    private void prepareForExpansionBoard(@ExpansionBoardType int expansionBoardType) {
        switch (expansionBoardType) {
            case ExpansionBoardType.NONE:
                setSampleRate(SampleStreamUtils.DEFAULT_SAMPLE_RATE);
                setChannelCount(SampleStreamUtils.SPIKER_BOX_PRO_CHANNEL_COUNT);
                break;
            case ExpansionBoardType.ADDITIONAL_INPUTS:
                setSampleRate(5000);
                setChannelCount(4);
                break;
            case ExpansionBoardType.HAMMER:
            case ExpansionBoardType.JOYSTICK:
                setSampleRate(5000);
                setChannelCount(3);
                break;
        }
    }
}
