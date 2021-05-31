package com.backyardbrains.utils;

import androidx.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
@Retention(RetentionPolicy.SOURCE) @IntDef({
    SpikerBoxHardwareType.NONE, SpikerBoxHardwareType.UNKNOWN, SpikerBoxHardwareType.PLANT,
    SpikerBoxHardwareType.MUSCLE, SpikerBoxHardwareType.HEART_AND_BRAIN,
    SpikerBoxHardwareType.MUSCLE_PRO, SpikerBoxHardwareType.NEURON_PRO,
    SpikerBoxHardwareType.HUMAN_PRO
}) public @interface SpikerBoxHardwareType {

    /**
     * Unknown hardware type.
     */
    int NONE = 100;

    /**
     * Unknown hardware type.
     */
    int UNKNOWN = -1;

    /**
     * SpikerBox Plant hardware type.
     */
    int PLANT = 0;

    /**
     * SpikerBox Muscle hardware type.
     */
    int MUSCLE = 1;

    /**
     * SpikerBox Brain & Heart hardware type.
     */
    int HEART_AND_BRAIN = 2;

    /**
     * SpikerBox Muscle PRO hardware type.
     */
    int MUSCLE_PRO = 3;

    /**
     * SpikerBox Neuron PRO hardware type.
     */
    int NEURON_PRO = 4;

    /**
     * SpikerBox Human PRO hardware type.
     */
    int HUMAN_PRO = 5;
}
