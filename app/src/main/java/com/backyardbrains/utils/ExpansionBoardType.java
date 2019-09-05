package com.backyardbrains.utils;

import androidx.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
@Retention(RetentionPolicy.SOURCE) @IntDef({
    ExpansionBoardType.NONE, ExpansionBoardType.ADDITIONAL_INPUTS, ExpansionBoardType.HAMMER,
    ExpansionBoardType.JOYSTICK
}) public @interface ExpansionBoardType {

    /**
     * Expansion board detached.
     */
    int NONE = 0;

    /**
     * Type of expansion board with additional inputs.
     */
    int ADDITIONAL_INPUTS = 1;

    /**
     * Hammer expansion board type.
     */
    int HAMMER = 4;

    /**
     * Joystick expansion board type.
     */
    int JOYSTICK = 5;
}
