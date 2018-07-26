//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include "ThresholdProcessor.h"

const char *ThresholdProcessor::TAG = "ThresholdProcessor";

ThresholdProcessor::ThresholdProcessor() {
}

ThresholdProcessor::~ThresholdProcessor() {
}

void ThresholdProcessor::process(const short *inSamples, short *outSamples, const int length) {
    // reset buffers if threshold changed
    if (lastTriggeredValue != triggerValue) {
//        LOGD(TAG, "Resetting because trigger value has changed");
        reset();
        lastTriggeredValue = triggerValue;
    }
    // reset buffers if averages sample count changed
    if (lastAveragedSampleCount != averagedSampleCount) {
//        LOGD(TAG, "Resetting because last averaged sample count has changed");
        reset();
        lastAveragedSampleCount = averagedSampleCount;
    }
    // reset buffers if sample rate changed
    if (lastSampleRate != getSampleRate()) {
//        LOGD(TAG, "Resetting because sample rate has changed");
        reset();
        lastSampleRate = getSampleRate();
    }

    // append unfinished sample buffers whit incoming samples
    int samplesToCopy;
    for (int i = 0; i < unfinishedSamplesForCalculationCount; i++) {
        samplesToCopy = std::min(sampleCount - unfinishedSamplesForCalculationCounts[i], length);
        std::copy(inSamples, inSamples + samplesToCopy,
                  unfinishedSamplesForCalculation[i] + unfinishedSamplesForCalculationCounts[i]);
        unfinishedSamplesForCalculationCounts[i] += samplesToCopy;
    }

    short currentSample;
    int copyFromNew, copyFromExisting;
    // loop through incoming samples and listen for the threshold hit
    for (int i = 0; i < length; i++) {
        currentSample = inSamples[i];

        // heartbeat processing
//        if (processBpm) {
//            sampleCounter++;
//            lastTriggerSampleCounter++;
//
//            // check if minimum BPM reset period passed after last threshold hit and reset if necessary
//            if (lastTriggerSampleCounter > minBpmResetPeriodCount) resetBpm();
//        }
        // end of heartbeat processing

        if (!inDeadPeriod) {
            // check if we hit the threshold
            if ((triggerValue >= 0 && currentSample > triggerValue && prevSample <= triggerValue) || (
                    triggerValue < 0 && currentSample < triggerValue && prevSample >= triggerValue)) {
                // we hit the threshold, turn on dead period of 5ms
                inDeadPeriod = true;

                // create new samples for current threshold
                short *centeredWave = new short[sampleCount];
                copyFromExisting = bufferSampleCount - i;
                copyFromNew = std::min(bufferSampleCount + i, length);
                std::copy(buffer, buffer + copyFromExisting, centeredWave);
                std::copy(inSamples, inSamples + copyFromNew, centeredWave + copyFromExisting);

                unfinishedSamplesForCalculation[unfinishedSamplesForCalculationCount] = centeredWave;
                unfinishedSamplesForCalculationCounts[unfinishedSamplesForCalculationCount++] =
                        copyFromExisting + copyFromNew;

                // heartbeat processing
//                if (processBpm) {
//                    // pass data to heartbeat helper
//                    heartbeatHelper.beat(sampleCounter);
//                    // reset the last triggered sample counter
//                    // and start counting for next heartbeat reset period
//                    lastTriggerSampleCounter = 0;
//                }
                // end of heartbeat processing

                break;
            }
        } else {
            if (++deadPeriodSampleCounter > deadPeriodCount) {
                deadPeriodSampleCounter = 0;
                inDeadPeriod = false;
            }
        }

        prevSample = currentSample;
    }

    // add samples to local buffer
    std::move(buffer + length, buffer + bufferSampleCount, buffer);
    std::copy(inSamples, inSamples + length, buffer + bufferSampleCount - length);

    // add incoming samples to calculation of averages
//    int len = unfinishedSamplesForCalculation.size();
//    for (int i = 0; i < len; i++) {
//        addSamplesToCalculations(unfinishedSamplesForCalculation.get(i), i);
//    }

    // move filled sample buffers from unfinished samples collection to finished samples collection
//    final Iterator<Samples> iterator = unfinishedSamplesForCalculation.iterator();
//    while (iterator.hasNext()) {
//        final Samples samples = iterator.next();
//        if (samples.isPopulated()) {
//            if (samplesForCalculation.size() >= averagedSampleCount) samplesForCalculation.remove(0);
//            samplesForCalculation.add(samples.samples);
//            iterator.remove();
//        }
//    }

    // TODO: 9/20/2017 We should dump unnecessary finished samples to release the memory
}

void ThresholdProcessor::reset() {
    sampleCount = static_cast<int>(getSampleRate() * MAX_PROCESSED_SECONDS);
    bufferSampleCount = static_cast<int>(getSampleRate() / 2);
//    minBpmResetPeriodCount = (int) (sampleRate * minBpmResetPeriodSeconds);

    buffer = new short[bufferSampleCount];
    samplesForCalculation = new short[averagedSampleCount];
//    summedSamples = null;
//    summedSamplesCounts = null;
//    averagedSamples = new short[sampleCount];
    unfinishedSamplesForCalculation = new short *[UNFINISHED_SAMPLES_COUNT];
    unfinishedSamplesForCalculationCounts = new int[UNFINISHED_SAMPLES_COUNT]{0};
    unfinishedSamplesForCalculationAveragedCounts = new int[UNFINISHED_SAMPLES_COUNT]{0};

    deadPeriodCount = static_cast<int>(getSampleRate() * DEAD_PERIOD_SECONDS);
    deadPeriodSampleCounter = 0;
    inDeadPeriod = false;

    prevSample = 0;

//    heartbeatHelper.reset("reset()");
//    heartbeatHelper.setSampleRate(sampleRate);
//    lastTriggerSampleCounter = 0;
//    sampleCounter = 0;
}