package com.backyardbrains.data;

import com.backyardbrains.drawing.ThresholdOrientation;

/**
 * @author Tihomir Leka <ticapeca at gmail.com.
 */
public class Threshold {

    private final int[] thresholds;

    public Threshold() {
        thresholds = new int[2];
    }

    public Threshold(int leftThreshold, int rightThreshold) {
        thresholds = new int[2];
        this.thresholds[ThresholdOrientation.LEFT] = leftThreshold;
        this.thresholds[ThresholdOrientation.RIGHT] = rightThreshold;
    }

    public int getThreshold(@ThresholdOrientation int orientation) {
        return thresholds[orientation];
    }

    public void setThreshold(@ThresholdOrientation int orientation, int threshold) {
        thresholds[orientation] = threshold;
    }
}
