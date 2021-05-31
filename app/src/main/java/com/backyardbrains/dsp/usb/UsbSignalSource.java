package com.backyardbrains.dsp.usb;

import android.hardware.usb.UsbDevice;
import com.backyardbrains.dsp.SignalSource;
import com.backyardbrains.utils.SpikerBoxHardwareType;

/**
 * Defines common USB in/out operations.
 *
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public interface UsbSignalSource extends SignalSource {

    // BYB Vendor ID
    int BYB_VENDOR_ID = 0x2E73;
    // BYB Muscle SpikerBox Pro Product ID
    int BYB_PID_MUSCLE_SB_PRO = 0x1;
    // BYB Neuron SpikerBox Pro Product ID
    int BYB_PID_NEURON_SB_PRO = 0x2;
    // BYB Human SpikerBox Pro Product ID
    int BYB_PID_HUMAN_SB_PRO = 0x4;
    // BYB HHI BOX Product ID
    int BYB_PID_HHI_BOX = 0x5;

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