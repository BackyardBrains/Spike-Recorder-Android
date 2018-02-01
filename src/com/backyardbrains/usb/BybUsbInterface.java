package com.backyardbrains.usb;

import com.backyardbrains.utils.SpikerBoxHardwareType;

/**
 * Defines common USB in/out operations.
 *
 * @author Tihomir Leka <ticapeca at gmail.com>.
 */
interface BybUsbInterface {

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
     * Starts reading data from the usb endpoint.
     */
    void startStream();
}
