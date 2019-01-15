package com.backyardbrains.vo;

import com.backyardbrains.utils.ThresholdOrientation;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class Threshold {

    private final int[] thresholds;

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
