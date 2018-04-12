package com.backyardbrains.filters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.audio.Filters;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class UsbSerialFilterSettingsDialog extends FilterSettingsDialog {

    private static final int FILTER_COUNT = 4;

    // Array of predefined filter names
    private static final String[] FILTER_NAMES = new String[FILTER_COUNT];

    static {
        FILTER_NAMES[0] = "Muscle (EMG)";
        FILTER_NAMES[1] = "Heart(EKG)";
        FILTER_NAMES[2] = "Brain(EEG)";
        FILTER_NAMES[3] = "Plant";
    }

    ;
    // Array of predefined filters (EMG, EKG, EEG, Plant)
    private static final Filter[] FILTERS = new Filter[FILTER_COUNT];

    static {
        FILTERS[0] = Filters.FILTER_MUSCLE;
        FILTERS[1] = Filters.FILTER_HEART;
        FILTERS[2] = Filters.FILTER_BRAIN;
        FILTERS[3] = Filters.FILTER_PLANT;
    }

    private static final double FREQ_MIN_CUT_OFF = 0d;
    private static final double FREQ_MAX_CUT_OFF = 5000d;

    public UsbSerialFilterSettingsDialog(@NonNull Context context, @Nullable FilterSelectionListener listener) {
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
