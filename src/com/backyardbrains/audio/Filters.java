package com.backyardbrains.audio;

import android.support.annotation.Nullable;
import com.backyardbrains.filters.Filter;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.ObjectUtils;
import uk.me.berndporr.iirj.Butterworth;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
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
    private static final double FREQ_HIGH_CUTOFF_MUSCLE = 2500d;
    // Low cut-off frequency for Neuron PRO
    private static final double FREQ_LOW_CUTOFF_NEURON_PRO = 160d;
    // High cut-off frequency for Neuron PRO
    private static final double FREQ_HIGH_CUTOFF_NEURON_PRO = 3700d;
    // Order used the all filters
    private static final int FILTER_ORDER = 2;

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

    // Filter used for additional filtering
    private Butterworth customFilter;
    // Current filter
    private Filter filter;
    // Sample rate to be used when filtering signals (default is 44100)
    private int sampleRate = AudioUtils.SAMPLE_RATE;

    public Filters() {
        customFilter = new Butterworth();
    }

    /**
     * Applies currently configured filter to specified {@code sample} and returns it, or just returns the {@code
     * sample} if filter is not configured.
     */
    public short apply(short sample) {
        // apply additional filtering if necessary
        if (filter != null) return (short) customFilter.filter(sample);

        return sample;
    }

    /**
     * Sets sample rate to be used when configuring filters.
     */
    public void setSampleRate(int sampleRate) {
        if (this.sampleRate == sampleRate) return;
        if (sampleRate <= 0) return; // sample rate needs to be positive

        this.sampleRate = sampleRate;

        setFilter(filter, true);
    }

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
        setFilter(filter, false);
    }

    // Actually sets up the filter that's applied during processing of incoming data.
    private void setFilter(@Nullable Filter filter, boolean internal) {
        if (internal || !ObjectUtils.equals(this.filter, filter)) {
            if (filter != null) {
                // if both cut-off frequencies are negative, or if low cut-off is minimum cut-off value
                // and high cut-off is maximum cut-off value we should not use filter
                if ((!filter.isLowCutOffFrequencySet() && !filter.isHighCutOffFrequencySet()) || (
                    filter.getLowCutOffFrequency() == Filter.FREQ_MIN_CUT_OFF
                        && filter.getHighCutOffFrequency() == Filter.FREQ_MAX_CUT_OFF)) {
                    this.filter = null;
                    return;
                }

                setFilterCutOffFrequencies(filter.getLowCutOffFrequency(), filter.getHighCutOffFrequency());
            }

            this.filter = filter;
        }
    }

    // Sets filter cut-off frequencies to be applied when processing of incoming data.
    private void setFilterCutOffFrequencies(double lowCutOffFreq, double highCutOffFreq) {
        // high cut-off frequency cannot be lower then low cut off frequency
        if (highCutOffFreq < lowCutOffFreq) return;
        // both cut-off frequencies cannot be negative
        if (lowCutOffFreq == Filter.FREQ_NO_CUT_OFF && highCutOffFreq == Filter.FREQ_NO_CUT_OFF) return;

        // reset custom filter
        customFilter.reset();
        if (lowCutOffFreq != Filter.FREQ_NO_CUT_OFF && highCutOffFreq != Filter.FREQ_NO_CUT_OFF) { // band pass
            double freq = Math.abs(highCutOffFreq - lowCutOffFreq);
            customFilter.bandPass(FILTER_ORDER, sampleRate, freq / 2 + lowCutOffFreq, freq);
        } else if (highCutOffFreq != Filter.FREQ_NO_CUT_OFF) { // low pass
            customFilter.lowPass(FILTER_ORDER, sampleRate, highCutOffFreq);
        } else { // high pass
            customFilter.highPass(FILTER_ORDER, sampleRate, lowCutOffFreq);
        }
    }
}
