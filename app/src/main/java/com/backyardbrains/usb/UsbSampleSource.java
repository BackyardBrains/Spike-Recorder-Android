package com.backyardbrains.usb;

import android.hardware.usb.UsbDevice;
import com.backyardbrains.data.processing.SampleSource;
import com.backyardbrains.utils.SpikerBoxHardwareType;

/**
 * Defines common USB in/out operations.
 *
 * @author Tihomir Leka <ticapeca at gmail.com>.
 */
public interface UsbSampleSource extends SampleSource {

    // BYB Vendor ID
    int BYB_VENDOR_ID = 0x2E73;
    // BYB Muscle SpikerBox Pro Product ID
    int BYB_PID_MUSCLE_SB_PRO = 0x1;
    // BYB Neuron SpikerBox Pro Product ID
    int BYB_PID_NEURON_SB_PRO = 0x2;

    /**
     * Opens usb communication port.
     */
    boolean open();

    /**
     * Transfers specified array of bytes to to the connected endpoint.
     *
     * @param buffer Data that is sent to connected usb endpoint.
     */
    void write(byte[] buffer);

    /**
     * Returns wrapped {@link UsbDevice}.
     */
    UsbDevice getUsbDevice();

    /**
     * Returns SpikerBox hardware type of the input source.
     */
    @SpikerBoxHardwareType int getHardwareType();

    /**
     * Sends to inquiry message to connected USB device to check for hardware type.
     */
    void checkHardwareType();
}
