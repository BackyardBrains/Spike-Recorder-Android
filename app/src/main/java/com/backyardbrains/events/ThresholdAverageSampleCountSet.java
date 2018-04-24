package com.backyardbrains.events;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
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
