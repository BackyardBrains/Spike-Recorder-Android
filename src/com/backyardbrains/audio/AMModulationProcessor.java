package com.backyardbrains.audio;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.data.SampleProcessor;
import com.backyardbrains.utils.AudioUtils;
import java.util.Arrays;
import uk.me.berndporr.iirj.Butterworth;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class AMModulationProcessor implements SampleProcessor {

    private static final String TAG = makeLogTag(AMModulationProcessor.class);

    private static final int BUFFER_SIZE = AudioUtils.SAMPLE_RATE * 2; // 2 secs

    private static final int FILTER_ORDER = 2;
    private static final int CARRIER_FREQ = 5000;
    private static final int WIDTH_IN_FREQ = 2500;
    private static final int LOW_PASS_CUTOFF_FREQ = 500;

    // Holds samples after filtering
    private final short[] filteredSamples = new short[BUFFER_SIZE];
    @SuppressWarnings("FieldCanBeLocal") private int sampleCount;

    // Band stop filter
    private Butterworth bandStopFilter;
    // Low pass filter
    private Butterworth lowPassFilter1;
    private Butterworth lowPassFilter2;
    private Butterworth lowPassFilter3;
    // Both below are used to detect whether signal is modulated or not
    private double rmsOfOriginalSignal;
    private double rmsOfNotchedAMSignal;
    // Average signal which we use to avoid signal offset
    private double average;

    public AMModulationProcessor() {
        // init buffer
        reset();
    }

    @Nullable @Override public short[] process(@NonNull short[] data) {
        if (data.length > 0) return processIncomingData(data);

        return new short[0];
    }

    private void reset() {
        LOGD(TAG, "reset()");
        bandStopFilter = new Butterworth();
        bandStopFilter.bandStop(FILTER_ORDER, AudioUtils.SAMPLE_RATE, CARRIER_FREQ, WIDTH_IN_FREQ);
        lowPassFilter1 = new Butterworth();
        lowPassFilter1.lowPass(FILTER_ORDER, AudioUtils.SAMPLE_RATE, LOW_PASS_CUTOFF_FREQ);
        lowPassFilter2 = new Butterworth();
        lowPassFilter2.lowPass(FILTER_ORDER, AudioUtils.SAMPLE_RATE, LOW_PASS_CUTOFF_FREQ);
        lowPassFilter3 = new Butterworth();
        lowPassFilter3.lowPass(FILTER_ORDER, AudioUtils.SAMPLE_RATE, LOW_PASS_CUTOFF_FREQ);
    }

    private short[] processIncomingData(@NonNull short[] samples) {
        sampleCount = samples.length;

        for (int i = 0; i < sampleCount; i++) {
            rmsOfOriginalSignal = 0.0001 * Math.pow(samples[i], 2) + 0.9999 * rmsOfOriginalSignal;
            filteredSamples[i] = (short) bandStopFilter.filter(samples[i]);
            rmsOfNotchedAMSignal = 0.0001 * Math.pow(filteredSamples[i], 2) + 0.9999 * rmsOfNotchedAMSignal;
        }

        if (Math.sqrt(rmsOfOriginalSignal) / Math.sqrt(rmsOfNotchedAMSignal) > 5) {
            for (int i = 0; i < sampleCount; i++) {
                filteredSamples[i] = (short) lowPassFilter1.filter(Math.abs(samples[i]));
                filteredSamples[i] = (short) lowPassFilter2.filter(Math.abs(filteredSamples[i]));
                filteredSamples[i] = (short) lowPassFilter3.filter(Math.abs(filteredSamples[i]));

                // calculate average sample
                average = 0.00001 * filteredSamples[i] + 0.99999 * average;
                // use average to remove offset and flip sample (undoing hardware flip)
                filteredSamples[i] = (short) ((filteredSamples[i] - average) * -1);
            }

            return Arrays.copyOfRange(filteredSamples, 0, sampleCount);
        }

        return samples;
    }
}
