package com.backyardbrains.audio;

import android.support.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Tihomir Leka <ticapeca at gmail.com.
 */
public interface InputSource {

    /**
     * Defines different input source types.
     */
    @Retention(RetentionPolicy.SOURCE) @IntDef({
        Type.AUDIO, Type.USB
    }) @interface Type {
        /**
         * Audio input source.
         */
        int AUDIO = 0;
        /**
         * Usb input source.
         */
        int USB = 1;
    }

    /**
     * Interface definition for a callback to be invoked when chunk of data is received from the input source.
     */
    interface OnSamplesReceivedListener {
        /**
         * Called when chunk of data is received.
         *
         * @param data The received data.
         */
        void onSamplesReceived(short[] data);
    }

    /**
     * Starts reading input source data and passing it to the set {@link OnSamplesReceivedListener}.
     */
    void start();

    /**
     * Pauses reading input source data.
     */
    void pause();

    /**
     * Resumes reading input source data.
     */
    void resume();

    /**
     * Stops reading input source data.
     */
    void stop();

    /**
     * Returns type of the input source. One of {@link Type} constants.
     */
    @Type int getType();
}
