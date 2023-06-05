package com.backyardbrains.filters;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.backyardbrains.dsp.Filters;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
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
    private static final BandFilter[] FILTERS = new BandFilter[FILTER_COUNT];

    static {
        FILTERS[0] = Filters.FILTER_BAND_MUSCLE;
        FILTERS[1] = Filters.FILTER_BAND_HEART;
        FILTERS[2] = Filters.FILTER_BAND_BRAIN;
        FILTERS[3] = Filters.FILTER_BAND_PLANT;
        FILTERS[4] = Filters.FILTER_BAND_HUMAN;
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

    @Override protected BandFilter[] getFilters() {
        return FILTERS;
    }

    @Override protected String[] getFilterNames() {
        return FILTER_NAMES;
    }
}
