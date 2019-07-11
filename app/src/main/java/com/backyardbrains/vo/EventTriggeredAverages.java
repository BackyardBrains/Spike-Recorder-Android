package com.backyardbrains.vo;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class EventTriggeredAverages {

    private final String[] events;
    private final float[][] average;
    private final float[][] normalizedAverage;

    public EventTriggeredAverages(String[] events, float[][] average, float[][] normalizedAverage) {
        this.events = events;
        this.average = average;
        this.normalizedAverage = normalizedAverage;
    }

    public String[] getEvents() {
        return events;
    }

    public float[][] getMean() {
        return average;
    }

    public float[][] getNormalizedAverage() {
        return normalizedAverage;
    }
}
