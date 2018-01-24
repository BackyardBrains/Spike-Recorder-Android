package com.backyardbrains.usb;

/**
 * Defines common USB in/out operations.
 *
 * @author Tihomir Leka <ticapeca at gmail.com>.
 */
public interface BybUsbInterface {

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
     * Send message to connected endpoint that streaming of sample data can start.
     */
    void startStreaming();

    /**
     * Send message to connected endpoint that streaming of sample data can stop.
     */
    void stopStreaming();

    /**
     * Starts receiving data from the usb endpoint asynchronously.
     *
     * @param callback Callback to be invoked when new batch of data is received.
     */
    void read(BybUsbReadCallback callback);

    /**
     * Closes usb communication port.
     */
    void close();

    interface BybUsbReadCallback {
        void onReceivedData(byte[] data);
    }
}
