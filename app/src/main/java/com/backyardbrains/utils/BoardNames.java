package com.backyardbrains.utils;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public final class BoardNames {
    private static final String B_NONE = "_B_NONE";
    private static final String B_UNKNOWN = "_B_UNKNOWN";
    private static final String B_PLANT = "_B_PLANT=";
    private static final String B_MUSCLE = "_B_MUSCLE";
    private static final String B_HEART_AND_BRAIN = "_B_HEART_AND_BRAIN";
    private static final String B_MUSCLE_PRO = "_B_MUSCLE_PRO";
    private static final String B_NEURON_PRO = "_B_NEURON_PRO";
    private static final String EB_NONE = "_EB_NONE";
    private static final String EB_ADDITIONAL_INPUTS = "_EB_ADDITIONAL_INPUTS";
    private static final String EB_HAMMER = "_EB_HAMMER";
    private static final String EB_JOYSTICK = "_EB_JOYSTICK";

    public static String toBoardName(@SpikerBoxHardwareType int boardType) {
        switch (boardType) {
            case SpikerBoxHardwareType.HEART_AND_BRAIN:
                return B_HEART_AND_BRAIN;
            case SpikerBoxHardwareType.MUSCLE_PRO:
                return B_MUSCLE_PRO;
            case SpikerBoxHardwareType.MUSCLE:
                return B_MUSCLE;
            case SpikerBoxHardwareType.NEURON_PRO:
                return B_NEURON_PRO;
            default:
            case SpikerBoxHardwareType.NONE:
                return B_NONE;
            case SpikerBoxHardwareType.PLANT:
                return B_PLANT;
            case SpikerBoxHardwareType.UNKNOWN:
                return B_UNKNOWN;
        }
    }

    public static String toExpansionBoardName(@ExpansionBoardType int expansionBoardType) {
        switch (expansionBoardType) {
            case ExpansionBoardType.ADDITIONAL_INPUTS:
                return EB_ADDITIONAL_INPUTS;
            case ExpansionBoardType.HAMMER:
                return EB_HAMMER;
            case ExpansionBoardType.JOYSTICK:
                return EB_JOYSTICK;
            default:
            case ExpansionBoardType.NONE:
                return EB_NONE;
        }
    }
}
