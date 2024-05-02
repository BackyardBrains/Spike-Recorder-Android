package com.backyardbrains.utils;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
@Retention(RetentionPolicy.SOURCE) @IntDef({
    HumanSpikerBoardState.OFF, HumanSpikerBoardState.ON
}) public @interface HumanSpikerBoardState {

    int OFF = 0;

    int ON = 1;


}
