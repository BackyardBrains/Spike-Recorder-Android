package com.backyardbrains.audio;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.data.processing.SampleProcessor;
import com.backyardbrains.utils.AudioUtils;
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
    // Cut off frequency that is applied before the detection
    private static final int FREQ_CUT_OFF_LOW_PASS_AM_DETECTION = 6000;
    private static final int WIDTH_IN_FREQ = 2500;
    // Cut-off frequency for AM demodulation
    private static final int FREQ_CUT_OFF_LOW_PASS_AM_MODULATION = 500;

    // Buffer that holds samples after filtering (buffer size is larger than number of incoming samples)
    private final short[] filteredSamples = new short[BUFFER_SIZE];
    // Actual number of incoming samples
    @SuppressWarnings("FieldCanBeLocal") private int sampleCount;

    // Whether we are in AM modulation or not
    private boolean amModulationDetected;
    // Low pass
    // Band stop filters used for detection
    private Butterworth detectionLowPassFilter;
    private Butterworth detectionBandStopFilter;
    // Low pass filter used for AM demodulation
    private Butterworth amLowPassFilter1;
    private Butterworth amLowPassFilter2;
    private Butterworth amLowPassFilter3;
    // Both below are used to detect whether signal is modulated or not
    private double rmsOfOriginalSignal;
    private double rmsOfNotchedAMSignal;
    // Average signal which we use to avoid signal offset
    private double average;
    // Additional filtering that should be applied when AM modulation is detected
    private Filters filters;

    /**
     * Listens for AM modulation detection and informs interested parties about it's start and end.
     */
    interface AmModulationDetectionListener {
        void onAmModulationStart();

        void onAmModulationEnd();
    }

    private AmModulationDetectionListener listener;

    AmModulationProcessor(@Nullable AmModulationDetectionListener listener, @Nullable Filters filters) {
        this.listener = listener;
        this.filters = filters;

        init();
    }

    @Override public short[] process(@NonNull short[] data) {
        if (data.length > 0) return processIncomingData(data);

        return new short[0];
    }

    /**
     * Whether we are currently in AM modulation.
     */
    public boolean isAmModulationDetected() {
        return amModulationDetected;
    }

    // Initializes all filters.
    private void init() {
        LOGD(TAG, "init()");
        detectionLowPassFilter = new Butterworth();
        detectionLowPassFilter.lowPass(FILTER_ORDER, AudioUtils.SAMPLE_RATE, FREQ_CUT_OFF_LOW_PASS_AM_DETECTION);
        detectionBandStopFilter = new Butterworth();
        detectionBandStopFilter.bandStop(FILTER_ORDER, AudioUtils.SAMPLE_RATE, FREQ_CARRIER, WIDTH_IN_FREQ);
        amLowPassFilter1 = new Butterworth();
        amLowPassFilter1.lowPass(FILTER_ORDER, AudioUtils.SAMPLE_RATE, FREQ_CUT_OFF_LOW_PASS_AM_MODULATION);
        amLowPassFilter2 = new Butterworth();
        amLowPassFilter2.lowPass(FILTER_ORDER, AudioUtils.SAMPLE_RATE, FREQ_CUT_OFF_LOW_PASS_AM_MODULATION);
        amLowPassFilter3 = new Butterworth();
        amLowPassFilter3.lowPass(FILTER_ORDER, AudioUtils.SAMPLE_RATE, FREQ_CUT_OFF_LOW_PASS_AM_MODULATION);
    }

    // Does actual processing of incoming samples.
    private short[] processIncomingData(@NonNull short[] samples) {
        sampleCount = samples.length;

        for (int i = 0; i < sampleCount; i++) {
            filteredSamples[i] = (short) detectionLowPassFilter.filter(samples[i]);
            rmsOfOriginalSignal = 0.0001 * Math.pow(filteredSamples[i], 2) + 0.9999 * rmsOfOriginalSignal;
            filteredSamples[i] = (short) detectionBandStopFilter.filter(filteredSamples[i]);
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
                if (filters != null) filteredSamples[i] = filters.apply(filteredSamples[i]);
            }

            return Arrays.copyOfRange(filteredSamples, 0, sampleCount);
        }

        if (amModulationDetected) {
            amModulationDetected = false;
            if (listener != null) listener.onAmModulationEnd();
        }

        return samples;
    }

    // TODO: 29-Jan-18  CODE FOR PROCESSING SAMPLES ONE BY ONE, LEAVE FOR NOW UNTIL WE COMPARE EXECUTION SPEED

    //public short processSingle(short sample) {
    //    return processIncomingSingle(sample);
    //}

    //private short processIncomingSingle(short sample) {
    //    short s;
    //    s = (short) detectionLowPassFilter.filter(sample);
    //    rmsOfOriginalSignal = 0.0001 * Math.pow(s, 2) + 0.9999 * rmsOfOriginalSignal;
    //    s = (short) detectionBandStopFilter.filter(s);
    //    rmsOfNotchedAMSignal = 0.0001 * Math.pow(s, 2) + 0.9999 * rmsOfNotchedAMSignal;
    //
    //    if (Math.sqrt(rmsOfOriginalSignal) / Math.sqrt(rmsOfNotchedAMSignal) > 5) {
    //        if (!amModulationDetected) {
    //            amModulationDetected = true;
    //            if (listener != null) listener.onAmModulationStart();
    //        }
    //
    //        s = (short) amLowPassFilter1.filter(Math.abs(sample));
    //        s = (short) amLowPassFilter2.filter(Math.abs(s));
    //        s = (short) amLowPassFilter3.filter(Math.abs(s));
    //
    //        // calculate average sample
    //        average = 0.00001 * s + 0.99999 * average;
    //        // use average to remove offset
    //        s = (short) (s - average);
    //
    //        // apply additional filtering if necessary
    //        if (filters != null) s = filters.apply(s);
    //
    //        return s;
    //    }
    //
    //    if (amModulationDetected) {
    //        amModulationDetected = false;
    //        if (listener != null) listener.onAmModulationEnd();
    //    }
    //
    //    return sample;
    //}
}
