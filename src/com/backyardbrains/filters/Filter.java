package com.backyardbrains.filters;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
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

    public double getLowCutOffFrequency() {
        return lowCutOffFrequency;
    }

    public double getHighCutOffFrequency() {
        return highCutOffFrequency;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Filter that = (Filter) o;

        return lowCutOffFrequency == that.lowCutOffFrequency && highCutOffFrequency == that.highCutOffFrequency;
    }
}
