package com.backyardbrains.drawing;

import com.backyardbrains.utils.EventUtils;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class EventsDrawData {

    public int[] eventIndices;
    public String[] eventNames;
    public int eventCount;

    public EventsDrawData() {
        this.eventIndices = new int[EventUtils.MAX_EVENT_COUNT];
        this.eventNames = new String[EventUtils.MAX_EVENT_COUNT];
        this.eventCount = 0;
    }
}
