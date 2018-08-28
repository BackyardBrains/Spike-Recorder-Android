package com.backyardbrains.utils;

import android.support.annotation.NonNull;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class SampleStreamUtils {

    // Hardware type SpikerBox reply message prefix.
    private static final String HARDWARE_TYPE_PREFIX = "HWT:";
    // Plant SpikerBox reply message for hardware type inquiry.
    private static final String HARDWARE_TYPE_PLANT = HARDWARE_TYPE_PREFIX + "PLANTSS;";
    // Muscle SpikerBox reply message for hardware type inquiry.
    private static final String HARDWARE_TYPE_MUSCLE = HARDWARE_TYPE_PREFIX + "MUSCLESS;";
    // Brain & Heart SpikerBox reply message for hardware type inquiry (old 1 channel, new 6 channels).
    private static final String HARDWARE_TYPE_HEART_AND_BRAIN_6CH = HARDWARE_TYPE_PREFIX + "HEARTSS;";
    // Heart & Brain SpikerBox reply message for hardware type inquiry.
    private static final String HARDWARE_TYPE_HEART_AND_BRAIN = HARDWARE_TYPE_PREFIX + "HBLEOSB;";
    // Neuron PRO SpikerBox reply message for hardware type inquiry.
    private static final String HARDWARE_TYPE_NEURON_PRO = HARDWARE_TYPE_PREFIX + "NEURONSB;";
    // Muscle PRO SpikerBox reply message for hardware type inquiry.
    private static final String HARDWARE_TYPE_MUSCLE_PRO = HARDWARE_TYPE_PREFIX + "MUSCLESB;";
    // Sample rate SpikerBox reply message prefix
    private static final String SAMPLE_RATE_PREFIX = "MSF:";
    // Number of channels SpikerBox reply message prefix
    private static final String NUM_OF_CHANNELS_PREFIX = "MNC:";
    // Event message prefix
    private static final String EVENT_PREFIX = "EVNT:";

    /**
     * Sample rate used throughout the app.
     */
    public static final int SAMPLE_RATE = 10000;

    /**
     * Whether specified {@code message} sent from SpikerBox is a hardware type message.
     */
    public static boolean isHardwareTypeMsg(@NonNull String message) {
        return message.contains(HARDWARE_TYPE_PREFIX);
    }

    /**
     * Parses specified SpikerBox {@code message} and returns SpikerBox hardware type.
     */
    public static @SpikerBoxHardwareType int getBoardType(@NonNull String message) {
        if (ObjectUtils.equals(HARDWARE_TYPE_PLANT, message)) return SpikerBoxHardwareType.PLANT;
        if (ObjectUtils.equals(HARDWARE_TYPE_MUSCLE, message)) return SpikerBoxHardwareType.MUSCLE;
        if (ObjectUtils.equals(HARDWARE_TYPE_HEART_AND_BRAIN_6CH, message)) return SpikerBoxHardwareType.HEART;
        if (ObjectUtils.equals(HARDWARE_TYPE_HEART_AND_BRAIN, message)) return SpikerBoxHardwareType.HEART;
        if (message.contains(HARDWARE_TYPE_NEURON_PRO)) return SpikerBoxHardwareType.NEURON_PRO;
        if (message.contains(HARDWARE_TYPE_MUSCLE_PRO)) return SpikerBoxHardwareType.MUSCLE_PRO;
        return SpikerBoxHardwareType.UNKNOWN;
    }

    /**
     * Whether specified {@code message} sent by SpikerBox is message that contains max sample rate and number of
     * channels.
     */
    public static boolean isSampleRateAndNumOfChannelsMsg(@NonNull String message) {
        return message.startsWith(SAMPLE_RATE_PREFIX) && message.contains(NUM_OF_CHANNELS_PREFIX);
    }

    /**
     * Parses the specified SpikerBox {@code message} and returns max sample rate.
     */
    public static int getMaxSampleRate(@NonNull String message) {
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
    public static int getChannelCount(@NonNull String message) {
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
     * Whether specified {@code message} sent by SpikerBox is an event message.
     */
    public static boolean isEventMsg(@NonNull String message) {
        return message.startsWith(EVENT_PREFIX);
    }

    /**
     * Parses the specified SpikerBox {@code message} and returns number of the event.
     */
    @NonNull public static String getEventNumber(@NonNull String message) {
        try {
            String eventNumber = message.replace(EVENT_PREFIX, "");
            return eventNumber.replace(";", "");
        } catch (Exception e) {
            return "";
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
