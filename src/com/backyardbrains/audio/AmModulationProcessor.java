package com.backyardbrains.audio;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.data.SampleProcessor;
import com.backyardbrains.filters.Filter;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.ObjectUtils;
import java.util.Arrays;
import uk.me.berndporr.iirj.Butterworth;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class AmModulationProcessor implements SampleProcessor {

    private static final String TAG = makeLogTag(AmModulationProcessor.class);

    // Buffer size of temporary buffer that's used while processing incoming samples
    private static final int BUFFER_SIZE = AudioUtils.SAMPLE_RATE * 2; // 2 secs
    // Order used the all filters
    private static final int FILTER_ORDER = 2;
    // Carrier frequency for AM modulation detection
    private static final int FREQ_CARRIER = 5000;
    private static final int WIDTH_IN_FREQ = 2500;
    // Cut-off frequency for AM demodulation
    private static final int FREQ_CUTOFF_LOW_PASS_AM_MODULATION = 500;

    // Buffer that holds samples after filtering (buffer size is larger than number of incomming samples)
    private final short[] filteredSamples = new short[BUFFER_SIZE];
    // Actual number of incoming samples
    @SuppressWarnings("FieldCanBeLocal") private int sampleCount;

    // Whether we are in AM modulation or not
    private boolean amModulationDetected;
    // Band stop filter
    private Butterworth bandStopFilter;
    // Low pass filter used for AM demodulation
    private Butterworth amLowPassFilter1;
    private Butterworth amLowPassFilter2;
    private Butterworth amLowPassFilter3;
    // Filter used for additional filtering
    private Butterworth customFilter;
    // Both below are used to detect whether signal is modulated or not
    private double rmsOfOriginalSignal;
    private double rmsOfNotchedAMSignal;
    // Average signal which we use to avoid signal offset
    private double average;
    // Current filter
    private Filter filter;

    /**
     * Listens for AM modulation detection and informs interested parties about it's start and end.
     */
    interface AmModulationDetectionListener {
        void onAmModulationStart();

        void onAmModulationEnd();
    }

    private AmModulationDetectionListener listener;

    AmModulationProcessor(@Nullable AmModulationDetectionListener listener) {
        this.listener = listener;

        init();
    }

    @Nullable @Override public short[] process(@NonNull short[] data) {
        if (data.length > 0) return processIncomingData(data);

        return new short[0];
    }

    /**
     * Whether we are currently in AM modulation.
     */
    public boolean isAmModulationDetected() {
        return amModulationDetected;
    }

    /**
     * Returns currently applied filter.
     */
    public Filter getFilter() {
        return filter;
    }

    /**
     * Sets filter that should be additionally applied during AM modulation.
     */
    public void setFilter(@Nullable Filter filter) {
        if (!ObjectUtils.equals(this.filter, filter)) {
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

    // Sets filter cut-off frequencies to be applied when in AM modulation.
    private void setFilterCutOffFrequencies(int lowCutOffFreq, int highCutOffFreq) {
        // high cut-off frequency cannot be lower then low cut off frequency
        if (highCutOffFreq < lowCutOffFreq) return;
        // both cut-off frequencies cannot be negative
        if (lowCutOffFreq == Filter.FREQ_NO_CUT_OFF && highCutOffFreq == Filter.FREQ_NO_CUT_OFF) return;

        // reset custom filter
        customFilter.reset();
        if (lowCutOffFreq != Filter.FREQ_NO_CUT_OFF && highCutOffFreq != Filter.FREQ_NO_CUT_OFF) { // band pass
            int freq = Math.abs(highCutOffFreq - lowCutOffFreq);
            customFilter.bandPass(FILTER_ORDER, AudioUtils.SAMPLE_RATE, freq / 2 + lowCutOffFreq, freq);
        } else if (highCutOffFreq != Filter.FREQ_NO_CUT_OFF) { // low pass
            customFilter.lowPass(FILTER_ORDER, AudioUtils.SAMPLE_RATE, highCutOffFreq);
        } else { // high pass
            customFilter.highPass(FILTER_ORDER, AudioUtils.SAMPLE_RATE, lowCutOffFreq);
        }
    }

    // Initializes all filters.
    private void init() {
        LOGD(TAG, "init()");
        bandStopFilter = new Butterworth();
        bandStopFilter.bandStop(FILTER_ORDER, AudioUtils.SAMPLE_RATE, FREQ_CARRIER, WIDTH_IN_FREQ);
        amLowPassFilter1 = new Butterworth();
        amLowPassFilter1.lowPass(FILTER_ORDER, AudioUtils.SAMPLE_RATE, FREQ_CUTOFF_LOW_PASS_AM_MODULATION);
        amLowPassFilter2 = new Butterworth();
        amLowPassFilter2.lowPass(FILTER_ORDER, AudioUtils.SAMPLE_RATE, FREQ_CUTOFF_LOW_PASS_AM_MODULATION);
        amLowPassFilter3 = new Butterworth();
        amLowPassFilter3.lowPass(FILTER_ORDER, AudioUtils.SAMPLE_RATE, FREQ_CUTOFF_LOW_PASS_AM_MODULATION);
        customFilter = new Butterworth();
    }

    // Does actual processing of incoming samples.
    private short[] processIncomingData(@NonNull short[] samples) {
        sampleCount = samples.length;

        for (int i = 0; i < sampleCount; i++) {
            rmsOfOriginalSignal = 0.0001 * Math.pow(samples[i], 2) + 0.9999 * rmsOfOriginalSignal;
            filteredSamples[i] = (short) bandStopFilter.filter(samples[i]);
            rmsOfNotchedAMSignal = 0.0001 * Math.pow(filteredSamples[i], 2) + 0.9999 * rmsOfNotchedAMSignal;
        }

        if (Math.sqrt(rmsOfOriginalSignal) / Math.sqrt(rmsOfNotchedAMSignal) > 5) {
            if (!amModulationDetected) {
                amModulationDetected = true;
                if (listener != null) listener.onAmModulationStart();
            }

            for (int i = 0; i < sampleCount; i++) {
                filteredSamples[i] = (short) amLowPassFilter1.filter(Math.abs(samples[i]));
                filteredSamples[i] = (short) amLowPassFilter2.filter(Math.abs(filteredSamples[i]));
                filteredSamples[i] = (short) amLowPassFilter3.filter(Math.abs(filteredSamples[i]));

                // calculate average sample
                average = 0.00001 * filteredSamples[i] + 0.99999 * average;
                // use average to remove offset
                filteredSamples[i] = (short) (filteredSamples[i] - average);

                // apply additional filtering if necessary
                if (filter != null) filteredSamples[i] = (short) customFilter.filter(filteredSamples[i]);
            }

            return Arrays.copyOfRange(filteredSamples, 0, sampleCount);
        }

        if (amModulationDetected) {
            amModulationDetected = false;
            if (listener != null) listener.onAmModulationEnd();
        }

        return samples;
    }
}
