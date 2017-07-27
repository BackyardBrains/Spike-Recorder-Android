/*
 * Backyard Brains Android App
 * Copyright (C) 2011 Backyard Brains
 * by Nathan Dotz <nate (at) backyardbrains.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.backyardbrains.audio;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.data.DataProcessor;
import com.backyardbrains.utils.AudioUtils;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Iterator;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class ThresholdProcessor implements DataProcessor {

    private static final String TAG = makeLogTag(ThresholdProcessor.class);

    private static final int DEFAULT_SIZE = 1;

    public static final int SAMPLE_COUNT = (int) (AudioUtils.SAMPLE_RATE * 0.68 * 2); // 680 ms
    private static final int DEAD_PERIOD = (int) (AudioUtils.SAMPLE_RATE * 0.005 * 2); // 5 ms
    private static final int BUFFER_SAMPLE_COUNT = SAMPLE_COUNT / 2; // 340 ms

    // Buffer that holds most recent 680 ms of audio
    private RingBuffer buffer;
    // Number of samples
    private int averagedSampleCount = DEFAULT_SIZE;
    // Holds sums of all the saved samples by index
    // summedSamples[0] = sampleBuffersInAverage.get(0)[0] + sampleBuffersInAverage.get(1)[0] + ...
    //           + sampleBuffersInAverage.get(sampleBuffersInAverage.size() - 1)[0]
    private int[] summedSamples;
    // Holds averages of all the saved samples by index
    // averagedSamples[0] = (sampleBuffersInAverage.get(0)[0] + sampleBuffersInAverage.get(1)[0] + ...
    //           + sampleBuffersInAverage.get(sampleBuffersInAverage.size() - 1)[0]) / sampleBuffersInAverage.size()
    private short[] averagedSamples;
    // Holds samples counts summed at specified position
    private int[] summedSamplesCounts;

    private ArrayList<short[]> samplesForCalculation;
    private ArrayList<Samples> unfinishedSamplesForCalculation;
    private Handler handler;
    private int triggerValue = Integer.MAX_VALUE;
    private int lastTriggeredValue;
    private int lastIncomingBufferSize;
    private short prevSample;
    private int deadPeriodSampleCounter;
    private boolean deadPeriod;

    /**
     * Creates new {@link ThresholdProcessor} that uses {@code size} number of sample sequences for average spike
     * calculation.
     */
    public ThresholdProcessor(int size) {
        // set initial number of chunks to use for calculating average
        setAveragedSampleCount(size);
        // init buffers
        reset();
        // handler used for setting threshold
        handler = new Handler();
    }

    @Nullable @Override public short[] processData(@NonNull ByteBuffer data) {
        if (data.capacity() >= 1) {
            data.clear();
            processIncomingData(data.asShortBuffer());

            return averagedSamples;
        }

        return new short[0];
    }

    /**
     * Clears all data.
     */
    public void close() {
        reset();
    }

    /**
     * Sets the number of sample sequences that should be summed to get the average spike value.
     */
    public void setAveragedSampleCount(int averagedSampleCount) {
        if (averagedSampleCount > 0) this.averagedSampleCount = averagedSampleCount;
    }

    /**
     * Returns the number of sample sequences that should be summed to get the average spike value.
     */
    public int getAveragedSampleCount() {
        return averagedSampleCount;
    }

    /**
     * Set's the sample frequency threshold.
     */
    public void setThreshold(final float threshold) {
        handler.post(new Runnable() {
            @Override public void run() {
                LOGD(TAG, "setThreshold: " + threshold);
                triggerValue = (int) threshold;
            }
        });
    }

    // Resets all the fields used for calculations
    private void reset() {
        buffer = new RingBuffer(BUFFER_SAMPLE_COUNT);
        samplesForCalculation = new ArrayList<>(averagedSampleCount * 2);
        summedSamples = null;
        summedSamplesCounts = null;
        averagedSamples = new short[SAMPLE_COUNT];
        unfinishedSamplesForCalculation = new ArrayList<>();
        prevSample = 0;
        deadPeriodSampleCounter = 0;
        deadPeriod = false;
    }

    // Processes the incoming data and triggers all necessary calculations.
    private void processIncomingData(ShortBuffer sb) {
        //long start = System.currentTimeMillis();
        //LOGD(TAG, "==========================================");
        //LOGD(TAG, "START - " + samplesForCalculation.size());

        // reset buffers if size  of buffer changed
        if (sb.capacity() != lastIncomingBufferSize) {
            reset();
            lastIncomingBufferSize = sb.capacity();
        }
        // reset buffers if threshold changed
        if (lastTriggeredValue != triggerValue) {
            reset();
            lastTriggeredValue = triggerValue;
        }
        //LOGD(TAG, "1. AFTER resetting buffers:" + (System.currentTimeMillis() - start));

        // initialize incoming array
        short[] incomingAsArray = new short[sb.capacity()];
        sb.get(incomingAsArray, 0, incomingAsArray.length);

        for (Samples samples : unfinishedSamplesForCalculation) {
            samples.append(incomingAsArray);
        }
        //LOGD(TAG, "2. AFTER appending samples:" + (System.currentTimeMillis() - start));

        short currentSample;
        // check if we hit the threshold
        for (int i = 0; i < incomingAsArray.length; i++) {
            currentSample = incomingAsArray[i];

            if (!deadPeriod) {
                if ((triggerValue >= 0 && currentSample > triggerValue && prevSample <= triggerValue) || (
                    triggerValue < 0 && currentSample < triggerValue && prevSample >= triggerValue)) {
                    //LOGD(TAG, "Threshold: " + triggerValue + ", prev: " + prevSample + ", current: " + currentSample);
                    deadPeriod = true;

                    // create new samples for current threshold
                    final short[] centeredWave = new short[SAMPLE_COUNT];
                    final int copyLength = Math.min(BUFFER_SAMPLE_COUNT, incomingAsArray.length);
                    System.arraycopy(buffer.getArray(), i, centeredWave, 0, buffer.getArray().length - i);
                    System.arraycopy(incomingAsArray, 0, centeredWave, buffer.getArray().length - i, copyLength);
                    final Samples samples = new Samples(centeredWave, buffer.getArray().length - i + copyLength);

                    unfinishedSamplesForCalculation.add(samples);

                    break;
                }
            } else {
                //LOGD(TAG, "Dead period");
                if (++deadPeriodSampleCounter > DEAD_PERIOD) {
                    deadPeriodSampleCounter = 0;
                    deadPeriod = false;
                }
            }

            prevSample = currentSample;
        }
        //LOGD(TAG, "3. AFTER adding current samples:" + (System.currentTimeMillis() - start));

        buffer.add(sb);
        //LOGD(TAG, "4. AFTER adding to buffer:" + (System.currentTimeMillis() - start));

        int len = unfinishedSamplesForCalculation.size();
        //if (len > 0) LOGD(TAG, "     ==========================================");
        for (int i = 0; i < len; i++) {
            addSamplesToCalculations(unfinishedSamplesForCalculation.get(i), i);
        }
        //if (len > 0) LOGD(TAG, "     ==========================================");
        //LOGD(TAG, "5. AFTER adding samples to calculation:" + (System.currentTimeMillis() - start));

        final Iterator<Samples> iterator = unfinishedSamplesForCalculation.iterator();
        while (iterator.hasNext()) {
            final Samples samples = iterator.next();
            if (samples.isPopulated()) {
                if (samplesForCalculation.size() >= averagedSampleCount) samplesForCalculation.remove(0);
                samplesForCalculation.add(samples.samples);
                iterator.remove();
            }
        }
        //LOGD(TAG, "6. AFTER removing finished samples:" + (System.currentTimeMillis() - start));

        //if (summedSamples == null) summedSamples = new int[SAMPLE_COUNT];
        //if (summedSamplesCounts == null) summedSamplesCounts = new int[SAMPLE_COUNT];
        //// save averages only if we have samples to read from
        //final int sLen = summedSamples.length;
        //if (draw) {
        //    for (int i = 0; i < sLen; i++) {
        //        if (summedSamplesCounts[i] > 0) {
        //            averagedSamples[i] = (short) (summedSamples[i] / summedSamplesCounts[i]);
        //        }
        //    }
        //}
        //LOGD(TAG, "7. AFTER calculating averages:" + (System.currentTimeMillis() - start));
    }

    private void addSamplesToCalculations(@NonNull Samples samples, int samplesIndex) {
        long start = System.currentTimeMillis();
        // init summed samples array
        if (summedSamples == null || summedSamplesCounts == null) {
            summedSamples = new int[SAMPLE_COUNT];
            summedSamplesCounts = new int[SAMPLE_COUNT];

            for (int i = samples.lastAveragedIndex + 1; i < samples.nextSampleIndex; i++) {
                summedSamples[i] = samples.samples[i];
                summedSamplesCounts[i]++;
                averagedSamples[i] = samples.samples[i];
            }
            samples.lastAveragedIndex = samples.nextSampleIndex;

            //LOGD(TAG, "     5.1. AFTER looping through all the samples:" + (System.currentTimeMillis() - start));

            return;
        }

        for (int i = samples.lastAveragedIndex + 1; i < samples.nextSampleIndex; i++) {
            if (summedSamplesCounts[i] >= averagedSampleCount) {
                final short toSubtract;
                if (averagedSampleCount < unfinishedSamplesForCalculation.size()) {
                    toSubtract = unfinishedSamplesForCalculation.get(samplesIndex - averagedSampleCount).samples[i];
                } else {
                    toSubtract = samplesForCalculation.get(samplesIndex)[i];
                }

                summedSamples[i] -= toSubtract;
                summedSamplesCounts[i]--;
            }
            summedSamples[i] += samples.samples[i];
            summedSamplesCounts[i]++;
            averagedSamples[i] = (short) (summedSamples[i] / summedSamplesCounts[i]);
        }

        samples.lastAveragedIndex = samples.nextSampleIndex;

        //LOGD(TAG, "     5.1. AFTER looping through all the samples:" + (System.currentTimeMillis() - start));
    }

    private class Samples {
        private short[] samples;
        private int lastAveragedIndex;
        private int nextSampleIndex;

        private Samples(@NonNull short[] samples, int nextSampleIndex) {
            this.samples = samples;
            this.lastAveragedIndex = 0;
            this.nextSampleIndex = nextSampleIndex;
        }

        boolean isPopulated() {
            return nextSampleIndex == samples.length;
        }

        boolean append(short[] samples) {
            final int samplesToCopy = Math.min(this.samples.length - nextSampleIndex, samples.length);
            System.arraycopy(samples, 0, this.samples, nextSampleIndex, samplesToCopy);
            nextSampleIndex += samplesToCopy;
            return isPopulated();
        }
    }
}
