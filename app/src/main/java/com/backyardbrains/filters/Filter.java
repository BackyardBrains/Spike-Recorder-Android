package com.backyardbrains.filters;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class Filter {

    /**
     * Constant value that should be used when low or high cut-off frequencies should not be applied.
     */
    public static final double FREQ_NO_CUT_OFF = -1d;
    public static final double FREQ_MIN_CUT_OFF = 0d;
    public static final double FREQ_MAX_CUT_OFF = 500d;

    private final double lowCutOffFrequency;
    private final double highCutOffFrequency;

    public Filter() {
        this.lowCutOffFrequency = FREQ_NO_CUT_OFF;
        this.highCutOffFrequency = FREQ_NO_CUT_OFF;
    }

    public Filter(double lowCutOffFrequency, double highCutOffFrequency) {
        this.lowCutOffFrequency = lowCutOffFrequency;
        this.highCutOffFrequency = highCutOffFrequency;
    }

    public boolean isLowCutOffFrequencySet() {
        return lowCutOffFrequency != FREQ_NO_CUT_OFF;
    }

    public double getLowCutOffFrequency() {
        return lowCutOffFrequency;
    }

    public boolean isHighCutOffFrequencySet() {
        return highCutOffFrequency != FREQ_NO_CUT_OFF;
    }

    public double getHighCutOffFrequency() {
        return highCutOffFrequency;
    }

    public boolean isEqual(double lowCutOffFrequency, double highCutOffFrequency) {
        return lowCutOffFrequency == this.lowCutOffFrequency && highCutOffFrequency == this.highCutOffFrequency;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Filter that = (Filter) o;

        return lowCutOffFrequency == that.lowCutOffFrequency && highCutOffFrequency == that.highCutOffFrequency;
    }
}
