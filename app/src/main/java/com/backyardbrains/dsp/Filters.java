package com.backyardbrains.dsp;

import android.support.annotation.Nullable;
import com.backyardbrains.filters.Filter;
import com.backyardbrains.utils.ObjectUtils;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class Filters {

    // Low cut-off frequency for ECG and EEG
    private static final double FREQ_LOW_CUTOFF_HEART_BRAIN_PLANT = 1d;
    // High cut-off frequency for EKG
    private static final double FREQ_HIGH_CUTOFF_HEART = 50d;
    // High cut-off frequency for EEG
    private static final double FREQ_HIGH_CUTOFF_BRAIN = 100d;
    // High cut-off frequency for Plant
    private static final double FREQ_HIGH_CUTOFF_PLANT = 10d;
    // Low cut-off frequency for EMG
    private static final double FREQ_LOW_CUTOFF_MUSCLE = 70d;
    // High cut-off frequency for EMG
    private static final double FREQ_HIGH_CUTOFF_MUSCLE = 2499d;
    // Low cut-off frequency for Neuron PRO
    private static final double FREQ_LOW_CUTOFF_NEURON_PRO = 160d;
    // High cut-off frequency for Neuron PRO
    private static final double FREQ_HIGH_CUTOFF_NEURON_PRO = 3700d;

    /**
     * Predefined filter configured for EKG.
     */
    public static final Filter FILTER_HEART = new Filter(FREQ_LOW_CUTOFF_HEART_BRAIN_PLANT, FREQ_HIGH_CUTOFF_HEART);
    /**
     * Predefined filter configured for EEG.
     */
    public static final Filter FILTER_BRAIN = new Filter(FREQ_LOW_CUTOFF_HEART_BRAIN_PLANT, FREQ_HIGH_CUTOFF_BRAIN);
    /**
     * Predefined filter configured for Plant.
     */
    public static final Filter FILTER_PLANT = new Filter(FREQ_LOW_CUTOFF_HEART_BRAIN_PLANT, FREQ_HIGH_CUTOFF_PLANT);
    /**
     * Predefined filter configured for EMG.
     */
    public static final Filter FILTER_MUSCLE = new Filter(FREQ_LOW_CUTOFF_MUSCLE, FREQ_HIGH_CUTOFF_MUSCLE);
    /**
     * Predefined filter configured for Neuron Pro.
     */
    public static final Filter FILTER_NEURON_PRO = new Filter(FREQ_LOW_CUTOFF_NEURON_PRO, FREQ_HIGH_CUTOFF_NEURON_PRO);

    // Current filter
    private Filter filter;

    /**
     * Returns currently applied filter.
     */
    public Filter getFilter() {
        return filter;
    }

    /**
     * Sets filter that should be additionally applied during processing of incoming data.
     */
    public void setFilter(@Nullable Filter filter) {
        if (!ObjectUtils.equals(this.filter, filter)) {
            if (filter != null) {
                // if both cut-off frequencies are negative, or if low cut-off is minimum cut-off value
                // and high cut-off is maximum cut-off value we should not use filter
                if ((!filter.isLowCutOffFrequencySet() && !filter.isHighCutOffFrequencySet()) || (
                    filter.getLowCutOffFrequency() == Filter.FREQ_MIN_CUT_OFF
                        && filter.getHighCutOffFrequency() == Filter.FREQ_MAX_CUT_OFF)) {
                    this.filter = null;
                    return;
                }
            }

            this.filter = filter;
        }
    }
}