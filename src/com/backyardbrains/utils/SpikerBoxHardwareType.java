package com.backyardbrains.utils;

import android.support.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
@Retention(RetentionPolicy.SOURCE) @IntDef({
    SpikerBoxHardwareType.UNKNOWN, SpikerBoxHardwareType.PLANT, SpikerBoxHardwareType.MUSCLE, SpikerBoxHardwareType.HEART,
    SpikerBoxHardwareType.MUSCLE_PRO, SpikerBoxHardwareType.NEURON_PRO
}) public @interface SpikerBoxHardwareType {

    /**
     * Unknown board type.
     */
    int UNKNOWN = -1;

    /**
     * SpikerBox Plant board type.
     */
    int PLANT = 0;

    /**
     * SpikerBox Muscle board type.
     */
    int MUSCLE = 1;

    /**
     * SpikerBox Brain & Heart board type.
     */
    int HEART = 2;

    /**
     * SpikerBox Muscle PRO board type.
     */
    int MUSCLE_PRO = 3;

    /**
     * SpikerBox Neuron PRO board type.
     */
    int NEURON_PRO = 4;
}
