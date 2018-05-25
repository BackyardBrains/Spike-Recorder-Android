package com.backyardbrains.events;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class ThresholdAverageSampleCountSet {

    private final int averageSampleCount;

    public ThresholdAverageSampleCountSet(int averageSampleCount) {
        this.averageSampleCount = averageSampleCount;
    }

    public int getAverageSampleCount() {
        return averageSampleCount;
    }
}
