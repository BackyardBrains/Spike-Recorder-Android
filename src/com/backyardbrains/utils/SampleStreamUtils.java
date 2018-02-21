package com.backyardbrains.utils;

import android.support.annotation.NonNull;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class SampleStreamUtils {

    // Board type SpikerBox reply message prefix.
    private static final String BOARD_TYPE_PREFIX = "HWT:";
    // Plant SpikerBox reply message for board type inquiry.
    private static final String BOARD_TYPE_PLANT = BOARD_TYPE_PREFIX + "PLANTSS;";
    // Muscle SpikerBox reply message for board type inquiry.
    private static final String BOARD_TYPE_MUSCLE = BOARD_TYPE_PREFIX + "MUSCLESS;";
    // Brain & Heart SpikerBox reply message for board type inquiry (old 1 channel, new 6 channels).
    private static final String BOARD_TYPE_HEART_AND_BRAIN_6CH = BOARD_TYPE_PREFIX + "HEARTSS;";
    // Heart & Brain SpikerBox reply message for board type inquiry.
    private static final String BOARD_TYPE_HEART_AND_BRAIN = BOARD_TYPE_PREFIX + "HBLEOSB;";
    // Neuron PRO SpikerBox reply message for board type inquiry.
    private static final String BOARD_TYPE_NEURON_PRO = BOARD_TYPE_PREFIX + "NEURONSB;";
    // Muscle PRO SpikerBox reply message for board type inquiry.
    private static final String BOARD_TYPE_MUSCLE_PRO = BOARD_TYPE_PREFIX + "MUSCLESB;";
    // Sample rate SpikerBox reply message prefix
    private static final String SAMPLE_RATE_PREFIX = "MSF:";
    // Number of channels SpikerBox reply message prefix
    private static final String NUM_OF_CHANNELS_PREFIX = "MNC:";

    /**
     * Sample rate used throughout the app.
     */
    public static final int SAMPLE_RATE = 10000;

    /**
     * Whether specified {@code msg} sent from SpikerBox is a hardware type message.
     */
    public static boolean isHardwareTypeMsg(@NonNull String msg) {
        return msg.contains(BOARD_TYPE_PREFIX);
    }

    /**
     * Parses specified SpikerBox {@code message} and returns SpikerBox hardware type.
     */
    public static @SpikerBoxHardwareType int getBoardType(@NonNull String message) {
        if (ObjectUtils.equals(BOARD_TYPE_PLANT, message)) return SpikerBoxHardwareType.PLANT;
        if (ObjectUtils.equals(BOARD_TYPE_MUSCLE, message)) return SpikerBoxHardwareType.MUSCLE;
        if (ObjectUtils.equals(BOARD_TYPE_HEART_AND_BRAIN_6CH, message)) return SpikerBoxHardwareType.HEART;
        if (ObjectUtils.equals(BOARD_TYPE_HEART_AND_BRAIN, message)) return SpikerBoxHardwareType.HEART;
        if (message.contains(BOARD_TYPE_NEURON_PRO)) return SpikerBoxHardwareType.NEURON_PRO;
        if (message.contains(BOARD_TYPE_MUSCLE_PRO)) return SpikerBoxHardwareType.MUSCLE_PRO;
        return SpikerBoxHardwareType.UNKNOWN;
    }

    /**
     * Whether specified {@code msg} sent by SpikerBox is message that contains max sample rate and number of channels.
     */
    public static boolean isSampleRateAndNumOfChannelsMsg(@NonNull String msg) {
        return msg.startsWith(SAMPLE_RATE_PREFIX) && msg.contains(NUM_OF_CHANNELS_PREFIX);
    }

    /**
     * Parses the specified SpikerBox {@code message} and returns max sample rate.
     */
    public static int getMaxSampleRate(String message) {
        try {
            String maxSampleRate = message.replace(SAMPLE_RATE_PREFIX, "");
            maxSampleRate = maxSampleRate.substring(0, maxSampleRate.indexOf(NUM_OF_CHANNELS_PREFIX));
            maxSampleRate = maxSampleRate.replace(";", "");
            return Integer.valueOf(maxSampleRate);
        } catch (Exception e) {
            return SAMPLE_RATE;
        }
    }

    /**
     * Parses the specified SpikerBox {@code message} and returns max sample rate.
     */
    public static int getChannelCount(String message) {
        try {
            String numOfChannels =
                message.substring(message.indexOf(NUM_OF_CHANNELS_PREFIX) + NUM_OF_CHANNELS_PREFIX.length());
            numOfChannels = numOfChannels.replace(";", "");
            return Integer.valueOf(numOfChannels);
        } catch (Exception e) {
            return 1;
        }
    }

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
