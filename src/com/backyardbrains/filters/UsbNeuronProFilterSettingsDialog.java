package com.backyardbrains.filters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.audio.Filters;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class UsbNeuronProFilterSettingsDialog extends FilterSettingsDialog {

    private static final int FILTER_COUNT = 1;

    // Array of predefined filter names
    private static final String[] FILTER_NAMES = new String[FILTER_COUNT];

    static {
        FILTER_NAMES[0] = "Neuron";
    }

    ;
    // Array of predefined filters (EMG, EKG, EEG, Plant)
    private static final Filter[] FILTERS = new Filter[FILTER_COUNT];

    static {
        FILTERS[0] = Filters.FILTER_NEURON_PRO;
    }

    private static final double FREQ_MIN_CUT_OFF = 0d;
    private static final double FREQ_MAX_CUT_OFF = 5000d;

    public UsbNeuronProFilterSettingsDialog(@NonNull Context context, @Nullable FilterSelectionListener listener) {
        super(context, listener);
    }

    @Override protected double getMinCutOff() {
        return FREQ_MIN_CUT_OFF;
    }

    @Override protected double getMaxCutOff() {
        return FREQ_MAX_CUT_OFF;
    }

    @Override protected Filter[] getFilters() {
        return FILTERS;
    }

    @Override protected String[] getFilterNames() {
        return FILTER_NAMES;
    }
}
