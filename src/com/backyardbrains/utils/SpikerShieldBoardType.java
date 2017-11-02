package com.backyardbrains.utils;

import android.support.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
@Retention(RetentionPolicy.SOURCE) @IntDef({
    SpikerShieldBoardType.UNKNOWN, SpikerShieldBoardType.PLANT, SpikerShieldBoardType.MUSCLE,
    SpikerShieldBoardType.HEART
}) public @interface SpikerShieldBoardType {

    /**
     * SpikerShield unknown board type.
     */
    int UNKNOWN = -1;

    /**
     * SpikerShield Plant board type.
     */
    int PLANT = 0;

    /**
     * SpikerShield Muscle board type.
     */
    int MUSCLE = 1;

    /**
     * SpikerShield Brain & Heart board type.
     */
    int HEART = 2;
}
