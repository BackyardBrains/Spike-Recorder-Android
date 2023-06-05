package com.backyardbrains.dsp;

import androidx.annotation.Nullable;
import com.backyardbrains.filters.BandFilter;
import com.backyardbrains.filters.NotchFilter;
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
    private static final double FREQ_LOW_CUTOFF_HUMAN = 0d;
    // High cut-off frequency for EMG
    private static final double FREQ_HIGH_CUTOFF_MUSCLE = 2499d;
    // Low cut-off frequency for Neuron PRO
    private static final double FREQ_LOW_CUTOFF_NEURON_PRO = 160d;
    // High cut-off frequency for Neuron PRO
    private static final double FREQ_HIGH_CUTOFF_NEURON_PRO = 3700d;
    // 50Hz cut-off frequency
    private static final double FREQ_CUTOFF_50HZ = 50d;
    // 60Hz cut-off frequency
    private static final double FREQ_CUTOFF_60HZ = 60d;

    /**
     * Predefined filter configured for EKG.
     */
    public static final BandFilter FILTER_BAND_HEART =
        new BandFilter(FREQ_LOW_CUTOFF_HEART_BRAIN_PLANT, FREQ_HIGH_CUTOFF_HEART);
    /**
     * Predefined filter configured for EEG.
     */
    public static final BandFilter FILTER_BAND_BRAIN =
        new BandFilter(FREQ_LOW_CUTOFF_HEART_BRAIN_PLANT, FREQ_HIGH_CUTOFF_BRAIN);
    /**
     * Predefined filter configured for Plant.
     */
    public static final BandFilter FILTER_BAND_PLANT =
        new BandFilter(FREQ_LOW_CUTOFF_HEART_BRAIN_PLANT, FREQ_HIGH_CUTOFF_PLANT);
    /**
     * Predefined filter configured for EMG.
     */
    public static final BandFilter FILTER_BAND_MUSCLE = new BandFilter(FREQ_LOW_CUTOFF_MUSCLE, FREQ_HIGH_CUTOFF_MUSCLE);
    public static final BandFilter FILTER_BAND_HUMAN = new BandFilter(FREQ_LOW_CUTOFF_HUMAN, FREQ_HIGH_CUTOFF_MUSCLE);
    /**
     * Predefined filter configured for Neuron Pro.
     */
    public static final BandFilter FILTER_BAND_NEURON_PRO =
        new BandFilter(FREQ_LOW_CUTOFF_NEURON_PRO, FREQ_HIGH_CUTOFF_NEURON_PRO);
    /**
     * Predefined notch filter that cuts-off 50Hz frequency
     */
    public static final NotchFilter FILTER_NOTCH_50HZ = new NotchFilter(FREQ_CUTOFF_50HZ);
    /**
     * Predefined notch filter that cuts-off 60Hz frequency
     */
    public static final NotchFilter FILTER_NOTCH_60HZ = new NotchFilter(FREQ_CUTOFF_60HZ);

    /**
     * Constant value that should be used when cut-off frequency should not be applied.
     */
    public static final double FREQ_NO_CUT_OFF = -1d;
    /**
     * Minimum value for the band filter cut-off frequency
     */
    public static final double FREQ_MIN_CUT_OFF = 0d;
    /**
     * Maximum value for the band filter cut-off frequency used with low frequency boards (PLAN, BRAIN, HEART)
     */
    public static final double FREQ_LOW_MAX_CUT_OFF = 500d;
    /**
     * Maximum value for the band filter cut-off frequency used with high frequency boards (MUSCLE, NEURO)
     */
    public static final double FREQ_HIGH_MAX_CUT_OFF = 5000d;

    // Current band filter
    private BandFilter bandFilter;
    // Current notch filter
    private NotchFilter notchFilter;

    /**
     * Returns currently applied band filter.
     */
    BandFilter getBandFilter() {
        return bandFilter;
    }

    /**
     * Sets band filter that should be additionally applied during processing of incoming data.
     */
    void setBandFilter(@Nullable BandFilter bandFilter) {
        if (!ObjectUtils.equals(this.bandFilter, bandFilter)) {
            if (bandFilter != null) {
                // if both cut-off frequencies are negative, or if low cut-off is minimum cut-off value
                // and high cut-off is maximum cut-off value we should not use filter
                if ((!bandFilter.isLowCutOffFrequencySet() && !bandFilter.isHighCutOffFrequencySet()) || (
                    bandFilter.getLowCutOffFrequency() == BandFilter.FREQ_MIN_CUT_OFF
                        && bandFilter.getHighCutOffFrequency() == BandFilter.FREQ_MAX_CUT_OFF)) {
                    this.bandFilter = null;
                    return;
                }
            }

            this.bandFilter = bandFilter;
        }
    }

    /**
     * Returns currently applied notch filter.
     */
    NotchFilter getNotchFilter() {
        return notchFilter;
    }

    /**
     * Sets notch filter that should be additionally applied during processing of incoming data.
     */
    void setNotchFilter(NotchFilter notchFilter) {
        this.notchFilter = notchFilter;
    }
}