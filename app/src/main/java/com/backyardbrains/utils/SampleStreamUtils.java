package com.backyardbrains.utils;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class SampleStreamUtils {

    /**
     * Sample rate used throughout the app.
     */
    public static final int DEFAULT_SAMPLE_RATE = 10000;
    /**
     * Sample rate used throughout the app.
     */
    public static final int SAMPLE_RATE_5000 = 5000;
    /**
     * Default channel count for SpikerBox Pro
     */
    public static final int SPIKER_BOX_PRO_CHANNEL_COUNT = 2;
    /**
     * Default channel config for SpikerBox Pro
     */
    public static final boolean[] DEFAULT_SPIKER_BOX_PRO_CHANNEL_CONFIG = new boolean[] { true, false };
    /**
     * Default channel config for Hammer
     */
    public static final boolean[] DEFAULT_HAMMER_CHANNEL_CONFIG = new boolean[] { true, false, true };

    /**
     * Returns name of SpikerBox based on the specified {@code hardwareType}.
     */
    public static String getSpikerBoxHardwareName(@SpikerBoxHardwareType int hardwareType) {
        switch (hardwareType) {
            case SpikerBoxHardwareType.NONE:
                return "No BYB Board attached";
            case SpikerBoxHardwareType.HEART_AND_BRAIN:
                return "Heart & Brain SpikerBox";
            case SpikerBoxHardwareType.MUSCLE_PRO:
                return "Muscle PRO SpikerBox";
            case SpikerBoxHardwareType.MUSCLE:
                return "Muscle SpikerBox";
            case SpikerBoxHardwareType.NEURON_PRO:
                return "Neuron PRO SpikerBox";
            case SpikerBoxHardwareType.PLANT:
                return "Plant SpikerBox";
            case SpikerBoxHardwareType.HUMAN_PRO:
                return "Human PRO SpikerBox";
            default:
            case SpikerBoxHardwareType.UNKNOWN:
                return "UNKNOWN";
        }
    }

    /**
     * Returns name of the expansion board based on the specified {@code expansionBoardType}.
     */
    public static String getExpansionBoardName(@ExpansionBoardType int expansionBoardType) {
        switch (expansionBoardType) {
            default:
            case ExpansionBoardType.NONE:
                return "No Expansion Board attached";
            case ExpansionBoardType.ADDITIONAL_INPUTS:
                return "Expansion Board with Additional Inputs";
            case ExpansionBoardType.HAMMER:
                return "Hammer Expansion Board";
            case ExpansionBoardType.JOYSTICK:
                return "Joystick Expansion Board";
        }
    }
}
