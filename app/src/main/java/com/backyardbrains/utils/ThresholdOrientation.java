package com.backyardbrains.utils;

import androidx.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
@Retention(RetentionPolicy.SOURCE) @IntDef({
    ThresholdOrientation.LEFT, ThresholdOrientation.RIGHT
}) public @interface ThresholdOrientation {
    int LEFT = 0;
    int RIGHT = 1;
}