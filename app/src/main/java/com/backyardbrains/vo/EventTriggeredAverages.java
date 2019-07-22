package com.backyardbrains.vo;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class EventTriggeredAverages {

    private final String[] events;
    private final float[][] averages;
    private final float[][] normAverages;
    private final boolean showConfidenceIntervals;
    private final float[] normMonteCarloAverages;
    private final float[] normMonteCarloTop;
    private final float[] normMonteCarloBottom;
    private final float min;
    private final float max;

    public EventTriggeredAverages(String[] events, float[][] averages, float[][] normAverages,
        boolean showConfidenceIntervals, float[] normMonteCarloAverages, float[] normMonteCarloTop,
        float[] normMonteCarloBottom, float min, float max) {
        this.events = events;
        this.averages = averages;
        this.normAverages = normAverages;
        this.showConfidenceIntervals = showConfidenceIntervals;
        this.normMonteCarloAverages = normMonteCarloAverages;
        this.normMonteCarloTop = normMonteCarloTop;
        this.normMonteCarloBottom = normMonteCarloBottom;
        this.min = min;
        this.max = max;
    }

    public String[] getEvents() {
        return events;
    }

    public float[][] getAverages() {
        return averages;
    }

    public float[][] getNormAverages() {
        return normAverages;
    }

    public boolean isShowConfidenceIntervals() {
        return showConfidenceIntervals;
    }

    public float[] getNormMonteCarloAverages() {
        return normMonteCarloAverages;
    }

    public float[] getNormMonteCarloTop() {
        return normMonteCarloTop;
    }

    public float[] getNormMonteCarloBottom() {
        return normMonteCarloBottom;
    }

    public float getMin() {
        return min;
    }

    public float getMax() {
        return max;
    }
}
