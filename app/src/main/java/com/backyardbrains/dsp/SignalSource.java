package com.backyardbrains.dsp;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public interface SignalSource {

    /**
     * Defines different sample source types.
     */
    @Retention(RetentionPolicy.SOURCE) @IntDef({
        Type.MICROPHONE, Type.USB, Type.FILE
    }) @interface Type {
        /**
         * Audio sample source.
         */
        int MICROPHONE = 0;
        /**
         * Usb sample source.
         */
        int USB = 1;
        /**
         * File sample source.
         */
        int FILE = 2;
    }

    /**
     * A processor receives notifications from a signal source. Notifications indicate signal source related events,
     * such as when new batch of bytes is available or signal source sample rate or channel count is changed.
     */
    interface Processor {

        /**
         * Called when new chunk of data from the sample source is available.
         *
         * @param data The buffer that contains received data.
         * @param length The length of the received data.
         */
        void onDataReceived(@NonNull byte[] data, int length);

        //void onSignalChanged(int sampleRate, int channelCount);

        /**
         * Called when sample source sample rate is changed.
         *
         * @param sampleRate New sample rate.
         */
        void onSampleRateChanged(int sampleRate);

        /**
         * Called when sample source channel count is changed.
         *
         * @param channelCount New channel count.
         */
        void onChannelCountChanged(int channelCount);
    }

    /**
     * Starts reading sample source data and passing it to the set {@link Processor}.
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
}
