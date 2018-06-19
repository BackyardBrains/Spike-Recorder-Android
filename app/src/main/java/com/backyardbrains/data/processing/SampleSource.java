package com.backyardbrains.data.processing;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import com.backyardbrains.usb.SamplesWithMarkers;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public interface SampleSource {

    /**
     * Defines different sample source types.
     */
    @Retention(RetentionPolicy.SOURCE) @IntDef({
        Type.AUDIO, Type.USB
    }) @interface Type {
        /**
         * Audio sample source.
         */
        int AUDIO = 0;
        /**
         * Usb sample source.
         */
        int USB = 1;
    }

    /**
     * Interface definition for a callback to be invoked when chunk of data is received and processed from the sample
     * source.
     */
    interface OnSamplesReceivedListener {
        /**
         * Called when chunk of data is received.
         *
         * @param samplesWithMarkers The received data which contains processed samples and events.
         */
        void onSamplesReceived(@NonNull SamplesWithMarkers samplesWithMarkers);
    }

    /**
     * Starts reading sample source data and passing it to the set {@link OnSamplesReceivedListener}.
     */
    void start();

    /**
     * Pauses reading sample source data.
     */
    void pause();

    /**
     * Resumes reading sample source data.
     */
    void resume();

    /**
     * Stops reading sample source data.
     */
    void stop();

    /**
     * Returns type of the sample source. One of {@link Type} constants.
     */
    @Type int getType();
}
