package com.backyardbrains.analysis;

import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.JniUtils;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class RmsHelper {

    // Root mean square quantifier used when analyzing selected spikes
    private static final float RMS_QUANTIFIER = 0.005f;

    private final short[] rmsSamples = new short[AudioUtils.DEFAULT_SAMPLE_RATE * 6];

    private int measureSampleCount;

    /**
     * Returns number of measured samples. Should be called after {@link #cal}
     * to have a valid number.
     */
    public int getMeasureSampleCount() {
        return measureSampleCount;
    }

    public float calculateRms(short[] samples, int drawStartIndex, int measureStartIndex, int measureEndIndex) {
        measureSampleCount = Math.abs(measureEndIndex - measureStartIndex);

        // calculate index for the first sample we take for measurement
        final int measureFirstSampleIndex = drawStartIndex + Math.min(measureStartIndex, measureEndIndex);

        // we need to check number of samples we're copying cause converting indices might have not been that precise
        if (measureFirstSampleIndex + measureSampleCount > samples.length) {
            measureSampleCount = samples.length - measureFirstSampleIndex;
        }
        System.arraycopy(samples, measureFirstSampleIndex, rmsSamples, 0, measureSampleCount);

        // calculate RMS
        float rms = JniUtils.rms(rmsSamples, measureSampleCount) * RMS_QUANTIFIER;
        if (Float.isInfinite(rms) || Float.isNaN(rms)) rms = 0f;

        return rms;
    }
}
