package com.backyardbrains.filters;

import com.backyardbrains.dsp.Filters;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class NotchFilter {

    private final double centerFrequency;

    public NotchFilter() {
        this.centerFrequency = Filters.FREQ_NO_CUT_OFF;
    }

    public NotchFilter(double centerFrequency) {
        this.centerFrequency = centerFrequency;
    }

    public double getCenterFrequency() {
        return centerFrequency;
    }

    public boolean isEqual(double centerFrequency) {
        return centerFrequency == this.centerFrequency;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final NotchFilter that = (NotchFilter) o;

        return centerFrequency == that.centerFrequency;
    }
}
