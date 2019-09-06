package com.backyardbrains.filters;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.backyardbrains.dsp.Filters;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class UsbMuscleProFilterSettingsDialog extends FilterSettingsDialog {

    private static final int FILTER_COUNT = 1;

    // Array of predefined filter names
    private static final String[] FILTER_NAMES = new String[FILTER_COUNT];

    static {
        FILTER_NAMES[0] = "Muscle";
    }

    ;
    // Array of predefined filters (EMG, EKG, EEG, Plant)
    private static final BandFilter[] FILTERS = new BandFilter[FILTER_COUNT];

    static {
        FILTERS[0] = Filters.FILTER_BAND_MUSCLE;
    }

    private static final double FREQ_MIN_CUT_OFF = 0d;
    private static final double FREQ_MAX_CUT_OFF = 5000d;

    public UsbMuscleProFilterSettingsDialog(@NonNull Context context, @Nullable FilterSelectionListener listener) {
        super(context, listener);
    }

    @Override protected double getMinCutOff() {
        return FREQ_MIN_CUT_OFF;
    }

    @Override protected double getMaxCutOff() {
        return FREQ_MAX_CUT_OFF;
    }

    @Override protected BandFilter[] getFilters() {
        return FILTERS;
    }

    @Override protected String[] getFilterNames() {
        return FILTER_NAMES;
    }
}
