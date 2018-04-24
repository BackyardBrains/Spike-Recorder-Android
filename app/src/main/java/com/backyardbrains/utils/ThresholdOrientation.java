package com.backyardbrains.utils;

import android.support.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
@Retention(RetentionPolicy.SOURCE) @IntDef({
    ThresholdOrientation.LEFT, ThresholdOrientation.RIGHT
}) public @interface ThresholdOrientation {
    int LEFT = 0;
    int RIGHT = 1;
}