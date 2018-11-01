package com.backyardbrains.utils;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class SampleStreamUtils {

    /**
     * Sample rate used throughout the app.
     */
    public static final int SAMPLE_RATE = 10000;

    /**
     * Returns name of SpikerBox based on the specified {@code hardwareType}.
     */
    public static String getSpikerBoxName(@SpikerBoxHardwareType int hardwareType) {
        switch (hardwareType) {
            case SpikerBoxHardwareType.HEART:
                return "Heart & Brain SpikerBox";
            case SpikerBoxHardwareType.MUSCLE_PRO:
                return "Muscle PRO SpikerBox";
            case SpikerBoxHardwareType.MUSCLE:
                return "Muscle SpikerBox";
            case SpikerBoxHardwareType.NEURON_PRO:
                return "Neuron PRO SpikerBox";
            case SpikerBoxHardwareType.PLANT:
                return "Plant SpikerBox";
            default:
            case SpikerBoxHardwareType.UNKNOWN:
                return "UNKNOWN";
        }
    }
}
