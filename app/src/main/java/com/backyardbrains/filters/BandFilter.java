package com.backyardbrains.filters;

import com.backyardbrains.dsp.Filters;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class BandFilter {

    public static final double FREQ_MIN_CUT_OFF = 0d;
    public static final double FREQ_MAX_CUT_OFF = 500d;

    private final double lowCutOffFrequency;
    private final double highCutOffFrequency;

    public BandFilter() {
        this.lowCutOffFrequency = Filters.FREQ_NO_CUT_OFF;
        this.highCutOffFrequency = Filters.FREQ_NO_CUT_OFF;
    }

    public BandFilter(double lowCutOffFrequency, double highCutOffFrequency) {
        this.lowCutOffFrequency = lowCutOffFrequency;
        this.highCutOffFrequency = highCutOffFrequency;
    }

    public boolean isLowCutOffFrequencySet() {
        return lowCutOffFrequency != Filters.FREQ_NO_CUT_OFF;
    }

    public double getLowCutOffFrequency() {
        return lowCutOffFrequency;
    }

    public boolean isHighCutOffFrequencySet() {
        return highCutOffFrequency != Filters.FREQ_NO_CUT_OFF;
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

        final BandFilter that = (BandFilter) o;

        return lowCutOffFrequency == that.lowCutOffFrequency && highCutOffFrequency == that.highCutOffFrequency;
    }
}
