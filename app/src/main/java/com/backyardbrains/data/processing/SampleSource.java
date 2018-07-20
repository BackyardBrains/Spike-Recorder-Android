package com.backyardbrains.data.processing;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
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
     * An sample source listener receives notifications from a sample source. Notifications indicate sample source
     * related events, such as when new batch of samples are available or sample source sample rate is detected.
     */
    interface SampleSourceListener {
        /**
         * Called when new chunk of data from the sample source is available.
         *
         * @param samplesWithEvents The received data which contains processed samples and events.
         */
        void onSamplesReceived(@NonNull SamplesWithEvents samplesWithEvents);

        /**
         * Called when sample source sample rate is detected.
         *
         * @param sampleRate The detected sample rate.
         */
        void onSampleRateDetected(int sampleRate);
    }

    /**
     * Starts reading sample source data and passing it to the set {@link SampleSourceListener}.
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
