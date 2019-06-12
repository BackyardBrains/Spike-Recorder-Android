package com.backyardbrains.drawing;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class EventsDrawData {

    public float[] eventIndices;
    public String[] eventNames;
    public int eventCount;

    public EventsDrawData(int maxEventCount) {
        eventIndices = new float[maxEventCount];
        eventNames = new String[maxEventCount];
        eventCount = 0;
    }
}
