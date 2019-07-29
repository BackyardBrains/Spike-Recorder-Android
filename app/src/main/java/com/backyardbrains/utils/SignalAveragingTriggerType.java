package com.backyardbrains.utils;

import androidx.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
@Retention(RetentionPolicy.SOURCE) @IntDef({
    SignalAveragingTriggerType.THRESHOLD, SignalAveragingTriggerType.ALL_EVENTS, SignalAveragingTriggerType.EVENT_1,
    SignalAveragingTriggerType.EVENT_2, SignalAveragingTriggerType.EVENT_3, SignalAveragingTriggerType.EVENT_4,
    SignalAveragingTriggerType.EVENT_5, SignalAveragingTriggerType.EVENT_6, SignalAveragingTriggerType.EVENT_7,
    SignalAveragingTriggerType.EVENT_8, SignalAveragingTriggerType.EVENT_9
}) public @interface SignalAveragingTriggerType {
    int THRESHOLD = -1;
    int ALL_EVENTS = 0;
    int EVENT_1 = 1;
    int EVENT_2 = 2;
    int EVENT_3 = 3;
    int EVENT_4 = 4;
    int EVENT_5 = 5;
    int EVENT_6 = 6;
    int EVENT_7 = 7;
    int EVENT_8 = 8;
    int EVENT_9 = 9;
}
