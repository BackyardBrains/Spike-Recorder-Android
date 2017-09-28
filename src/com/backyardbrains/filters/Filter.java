package com.backyardbrains.filters;

import com.backyardbrains.utils.AudioUtils;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class Filter {

    /**
     * Constant value that should be used when low or high cut-off frequencies should not be applied.
     */
    public static final int FREQ_NO_CUT_OFF = -1;
    public static final int FREQ_MIN_CUT_OFF = 0;
    public static final int FREQ_MAX_CUT_OFF = AudioUtils.SAMPLE_RATE / 2;

    private final int lowCutOffFrequency;
    private final int highCutOffFrequency;

    public Filter() {
        this.lowCutOffFrequency = FREQ_NO_CUT_OFF;
        this.highCutOffFrequency = FREQ_NO_CUT_OFF;
    }

    public Filter(int lowCutOffFrequency, int highCutOffFrequency) {
        this.lowCutOffFrequency = lowCutOffFrequency;
        this.highCutOffFrequency = highCutOffFrequency;
    }

    public int getLowCutOffFrequency() {
        return lowCutOffFrequency;
    }

    public int getHighCutOffFrequency() {
        return highCutOffFrequency;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Filter that = (Filter) o;

        return lowCutOffFrequency == that.lowCutOffFrequency && highCutOffFrequency == that.highCutOffFrequency;
    }
}
