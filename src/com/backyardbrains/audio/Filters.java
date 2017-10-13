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

    // High cut-off frequency for EKG
    private static final int FREQ_HIGH_CUTOFF_HEART = 50;
    // High cut-off frequency for EEG
    private static final int FREQ_HIGH_CUTOFF_BRAIN = 100;
    // High cut-off frequency for Plant
    private static final int FREQ_HIGH_CUTOFF_PLANT = 5;
    // Order used the all filters
    private static final int FILTER_ORDER = 2;

    /**
     * Predefined filter configured for EKG.
     */
    public static final Filter FILTER_HEART = new Filter(Filter.FREQ_NO_CUT_OFF, FREQ_HIGH_CUTOFF_HEART);
    /**
     * Predefined filter configured for EEG.
     */
    public static final Filter FILTER_BRAIN = new Filter(Filter.FREQ_NO_CUT_OFF, FREQ_HIGH_CUTOFF_BRAIN);
    /**
     * Predefined filter configured for Plan.
     */
    public static final Filter FILTER_PLANT = new Filter(Filter.FREQ_NO_CUT_OFF, FREQ_HIGH_CUTOFF_PLANT);

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

    //
    private void setFilter(@Nullable Filter filter, boolean internal) {
        if (internal || !ObjectUtils.equals(this.filter, filter)) {
            if (filter != null) {
                // if both cut-off frequencies are negative, or if low cut-off is minimum cut-off value
                // and high cut-off is maximum cut-off value we should kill not use filter
                if ((filter.getLowCutOffFrequency() == Filter.FREQ_NO_CUT_OFF
                    && filter.getHighCutOffFrequency() == Filter.FREQ_NO_CUT_OFF) || (
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
    private void setFilterCutOffFrequencies(int lowCutOffFreq, int highCutOffFreq) {
        // high cut-off frequency cannot be lower then low cut off frequency
        if (highCutOffFreq < lowCutOffFreq) return;
        // both cut-off frequencies cannot be negative
        if (lowCutOffFreq == Filter.FREQ_NO_CUT_OFF && highCutOffFreq == Filter.FREQ_NO_CUT_OFF) return;

        // reset custom filter
        customFilter.reset();
        if (lowCutOffFreq != Filter.FREQ_NO_CUT_OFF && highCutOffFreq != Filter.FREQ_NO_CUT_OFF) { // band pass
            int freq = Math.abs(highCutOffFreq - lowCutOffFreq);
            customFilter.bandPass(FILTER_ORDER, sampleRate, freq / 2 + lowCutOffFreq, freq);
        } else if (highCutOffFreq != Filter.FREQ_NO_CUT_OFF) { // low pass
            customFilter.lowPass(FILTER_ORDER, sampleRate, highCutOffFreq);
        } else { // high pass
            customFilter.highPass(FILTER_ORDER, sampleRate, lowCutOffFreq);
        }
    }
}
