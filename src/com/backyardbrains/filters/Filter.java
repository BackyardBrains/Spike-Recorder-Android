package com.backyardbrains.filters;

import android.support.annotation.NonNull;
import com.backyardbrains.utils.ObjectUtils;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class Filter {

    /**
     * Constant value that should be used when low or high cut-off frequencies should not be applied.
     */
    public static final int FREQ_NO_CUT_OFF = -1;

    private final String name;
    private final int highCutOffFrequency;
    private final int lowCutOffFrequency;

    public Filter(@NonNull String name, int highCutOffFrequency, int lowCutOffFrequency) {
        this.name = name;
        this.highCutOffFrequency = highCutOffFrequency;
        this.lowCutOffFrequency = lowCutOffFrequency;
    }

    public String getName() {
        return name;
    }

    public int getHighCutOffFrequency() {
        return highCutOffFrequency;
    }

    public int getLowCutOffFrequency() {
        return lowCutOffFrequency;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Filter that = (Filter) o;

        return ObjectUtils.equals(name, that.name) && highCutOffFrequency == that.highCutOffFrequency
            && lowCutOffFrequency == that.lowCutOffFrequency;
    }
}
