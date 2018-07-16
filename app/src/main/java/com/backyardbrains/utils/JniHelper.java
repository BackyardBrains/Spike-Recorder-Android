package com.backyardbrains.utils;

import android.support.annotation.Keep;
import com.backyardbrains.events.AmModulationDetectionEvent;
import org.greenrobot.eventbus.EventBus;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class JniHelper {

    @SuppressWarnings("unused") @Keep public static void onAmDemodulationChange(boolean start) {
        EventBus.getDefault().post(new AmModulationDetectionEvent(start));
    }
}
