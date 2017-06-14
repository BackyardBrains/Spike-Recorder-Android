package com.backyardbrains.analysis;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class BYBAverageSpike {
    private final float[] averageSpike;
    private final float[] normAverageSpike;
    private final float[] normTopSTDLine;
    private float[] normBottomSTDLine;

    BYBAverageSpike(float[] averageSpike, float[] normAverageSpike, float[] normTopSTDLine, float[] normBottomSTDLine) {
        this.averageSpike = averageSpike;
        this.normAverageSpike = normAverageSpike;
        this.normTopSTDLine = normTopSTDLine;
        this.normBottomSTDLine = normBottomSTDLine;
    }

    public float[] getAverageSpike() {
        return averageSpike;
    }

    public float[] getNormAverageSpike() {
        return normAverageSpike;
    }

    public float[] getNormTopSTDLine() {
        return normTopSTDLine;
    }

    public float[] getNormBottomSTDLine() {
        return normBottomSTDLine;
    }
}
