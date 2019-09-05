package com.backyardbrains.utils;

import androidx.annotation.Keep;
import com.backyardbrains.events.AmModulationDetectionEvent;
import com.backyardbrains.events.HeartbeatEvent;
import org.greenrobot.eventbus.EventBus;

import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class JniHelper {

    private static final String TAG = makeLogTag(JniHelper.class);

    private static final AmModulationDetectionEvent AM_MODULATION_DETECTION_EVENT = new AmModulationDetectionEvent();
    private static final HeartbeatEvent HEARTBEAT_EVENT = new HeartbeatEvent();

    @SuppressWarnings("unused") @Keep public static void onAmDemodulationChange(boolean start) {
        AM_MODULATION_DETECTION_EVENT.setStart(start);
        EventBus.getDefault().post(AM_MODULATION_DETECTION_EVENT);
    }

    @SuppressWarnings("unused") @Keep public static void onHeartbeat(int bpm) {
        HEARTBEAT_EVENT.setBeatsPerMinute(bpm);
        EventBus.getDefault().post(HEARTBEAT_EVENT);
    }
}
