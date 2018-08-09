//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include "ThresholdProcessor.h"

const char *ThresholdProcessor::TAG = "ThresholdProcessor";

ThresholdProcessor::ThresholdProcessor(OnHeartbeatListener *listener) {
    heartbeatHelper = new HeartbeatHelper(getSampleRate(), listener);
}

ThresholdProcessor::~ThresholdProcessor() {
}

int ThresholdProcessor::getAveragedSampleCount() {
    return averagedSampleCount;
}

void ThresholdProcessor::setAveragedSampleCount(int averagedSampleCount) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "setAveragedSampleCount(%d)", averagedSampleCount);

    if (averagedSampleCount <= 0 || ThresholdProcessor::averagedSampleCount == averagedSampleCount) return;

    ThresholdProcessor::averagedSampleCount = averagedSampleCount;
}

void ThresholdProcessor::setThreshold(int threshold) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "setThreshold(%d)", threshold);

    ThresholdProcessor::triggerValue = threshold;
}

void ThresholdProcessor::setBpmProcessing(bool processBpm) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "setBpmProcessing(%s)", processBpm ? "ON" : "OFF");

    if (ThresholdProcessor::processBpm == processBpm) return;

    // reset BPM if we stopped processing heartbeat
    if (!processBpm) resetBpm();

    ThresholdProcessor::processBpm = processBpm;
}

void ThresholdProcessor::process(const short *inSamples, short *outSamples, const int length) {
    // reset buffers if threshold changed
    bool shouldReset = false;
    if (lastTriggeredValue != triggerValue) {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "Resetting because trigger value has changed");
        lastTriggeredValue = triggerValue;
        shouldReset = true;
    }
    // reset buffers if averages sample count changed
    if (lastAveragedSampleCount != averagedSampleCount) {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "Resetting because last averaged sample count has changed");
        lastAveragedSampleCount = averagedSampleCount;
        shouldReset = true;
    }
    // reset buffers if sample rate changed
    if (lastSampleRate != getSampleRate()) {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "Resetting because sample rate has changed");
        lastSampleRate = getSampleRate();
        shouldReset = true;
    }
    if (shouldReset) reset();

    // append unfinished sample buffers with incoming samples
    int samplesToCopy;
    for (int i = 0; i < unfinishedSamplesForCalculationCount; i++) {
        samplesToCopy = std::min(sampleCount - unfinishedSamplesForCalculationCounts[i], length);
        std::copy(inSamples, inSamples + samplesToCopy,
                  unfinishedSamplesForCalculation[i] + unfinishedSamplesForCalculationCounts[i]);
        unfinishedSamplesForCalculationCounts[i] += samplesToCopy;
    }

    short currentSample;
    int copyFromIncoming, copyFromBuffer;
    // loop through incoming samples and listen for the threshold hit
    for (int i = 0; i < length; i++) {
        currentSample = inSamples[i];

        // heartbeat processing
        if (processBpm) {
            sampleCounter++;
            lastTriggerSampleCounter++;

            // check if minimum BPM reset period passed after last threshold hit and reset if necessary
            if (lastTriggerSampleCounter > minBpmResetPeriodCount) resetBpm();
        }
        // end of heartbeat processing

        if (!inDeadPeriod) {
            // check if we hit the threshold
            if ((triggerValue >= 0 && currentSample > triggerValue && prevSample <= triggerValue) || (
                    triggerValue < 0 && currentSample < triggerValue && prevSample >= triggerValue)) {
                // we hit the threshold, turn on dead period of 5ms
                inDeadPeriod = true;

                // create new samples for current threshold
                unfinishedSamplesForCalculation[unfinishedSamplesForCalculationCount] = new short[sampleCount]{0};
                copyFromBuffer = bufferSampleCount - i;
                copyFromIncoming = std::min(sampleCount - copyFromBuffer, length);
                std::copy(buffer + i, buffer + bufferSampleCount,
                          unfinishedSamplesForCalculation[unfinishedSamplesForCalculationCount]);
                std::copy(inSamples, inSamples + copyFromIncoming,
                          unfinishedSamplesForCalculation[unfinishedSamplesForCalculationCount] + copyFromBuffer);

                unfinishedSamplesForCalculationCounts[unfinishedSamplesForCalculationCount] =
                        copyFromBuffer + copyFromIncoming;
                unfinishedSamplesForCalculationAveragedCounts[unfinishedSamplesForCalculationCount++] = 0;

                // heartbeat processingA
                if (processBpm) {
                    // pass data to heartbeat helper
                    heartbeatHelper->beat(sampleCounter);
                    // reset the last triggered sample counter
                    // and start counting for next heartbeat reset period
                    lastTriggerSampleCounter = 0;
                }
                // end of heartbeat processing
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
    std::copy(buffer + length, buffer + bufferSampleCount, buffer);
    std::copy(inSamples, inSamples + length, buffer + bufferSampleCount - length);

    // add incoming samples to calculation of averages
    for (int i = 0; i < unfinishedSamplesForCalculationCount; i++) {
        for (int j = unfinishedSamplesForCalculationAveragedCounts[i];
             j < unfinishedSamplesForCalculationCounts[i]; j++) {
            // if we are calculating averagedSampleCount + 1. sample we should subtract the oldest one in the sum
            if (summedSamplesCounts[j] >= averagedSampleCount) {
                // subtract the value and decrease summed samples count for current position
                if (averagedSampleCount <= i) { // we look for the oldest one in the unfinished samples
                    summedSamples[j] -=
                            unfinishedSamplesForCalculation[i - averagedSampleCount][j];
                } else { // we look for the oldest one in the already collected and calculated samples
                    summedSamples[j] -=
                            samplesForCalculation[samplesForCalculationCount - averagedSampleCount + i][j];
                }
                summedSamplesCounts[j]--;
            }
            // add new value and increase summed samples count for current position
            summedSamples[j] += unfinishedSamplesForCalculation[i][j];
            summedSamplesCounts[j]++;
            // calculate the average
            averagedSamples[j] = (short) (summedSamples[j] / summedSamplesCounts[j]);
        }
        unfinishedSamplesForCalculationAveragedCounts[i] = unfinishedSamplesForCalculationCounts[i];
    }

    // move filled sample buffers from unfinished samples collection to finished samples collection
    for (int i = 0; i < unfinishedSamplesForCalculationCount; i++) {
        if (unfinishedSamplesForCalculationCounts[i] == sampleCount) {
            if (samplesForCalculationCount >= averagedSampleCount) {
                delete[] samplesForCalculation[0];
                if (samplesForCalculationCount > 1) {
                    std::move(samplesForCalculation + 1, samplesForCalculation + samplesForCalculationCount,
                              samplesForCalculation);
                }
                samplesForCalculationCount--;
            }
            samplesForCalculation[samplesForCalculationCount++] = unfinishedSamplesForCalculation[i];
            if (unfinishedSamplesForCalculationCount > i + 1) {
                std::move(unfinishedSamplesForCalculation + i + 1,
                          unfinishedSamplesForCalculation + unfinishedSamplesForCalculationCount,
                          unfinishedSamplesForCalculation + i);
                std::move(unfinishedSamplesForCalculationCounts + i + 1,
                          unfinishedSamplesForCalculationCounts + unfinishedSamplesForCalculationCount,
                          unfinishedSamplesForCalculationCounts + i);
                std::move(unfinishedSamplesForCalculationAveragedCounts + i + 1,
                          unfinishedSamplesForCalculationAveragedCounts + unfinishedSamplesForCalculationCount,
                          unfinishedSamplesForCalculationAveragedCounts + i);
            }
            unfinishedSamplesForCalculationCount--;
            i--;
        }
    }

    std::copy(averagedSamples, averagedSamples + sampleCount, outSamples);
}

void ThresholdProcessor::reset() {
    sampleCount = static_cast<int>(getSampleRate() * MAX_PROCESSED_SECONDS);
    bufferSampleCount = sampleCount / 2;

    delete[] buffer;
    buffer = new short[bufferSampleCount];
    // free memory before creating new arrays
    if (samplesForCalculationCount > 0) {
        for (int i = 0; i < samplesForCalculationCount; i++) {
            delete[] samplesForCalculation[i];
        }
    }
    delete[] samplesForCalculation;
    samplesForCalculation = new short *[averagedSampleCount];
    samplesForCalculationCount = 0;
    // free memory before creating new arrays
    delete[] summedSamples;
    summedSamples = new int[sampleCount]{0};
    // free memory before creating new arrays
    delete[] summedSamplesCounts;
    summedSamplesCounts = new int[sampleCount]{0};
    // free memory before creating new arrays
    delete[] averagedSamples;
    averagedSamples = new short[sampleCount]{0};
    // free memory before creating new arrays
    if (unfinishedSamplesForCalculationCount > 0) {
        for (int i = 0; i < unfinishedSamplesForCalculationCount; i++) {
            delete[] unfinishedSamplesForCalculation[i];
        }
    }
    delete[] unfinishedSamplesForCalculation;
    unfinishedSamplesForCalculation = new short *[UNFINISHED_SAMPLES_COUNT];
    unfinishedSamplesForCalculationCount = 0;
    // free memory before creating new arrays
    delete[] unfinishedSamplesForCalculationCounts;
    unfinishedSamplesForCalculationCounts = new int[UNFINISHED_SAMPLES_COUNT]{0};
    // free memory before creating new arrays
    delete[] unfinishedSamplesForCalculationAveragedCounts;
    unfinishedSamplesForCalculationAveragedCounts = new int[UNFINISHED_SAMPLES_COUNT]{0};

    deadPeriodCount = static_cast<int>(getSampleRate() * DEAD_PERIOD_SECONDS);
    deadPeriodSampleCounter = 0;
    inDeadPeriod = false;

    prevSample = 0;

    heartbeatHelper->reset();
    heartbeatHelper->setSampleRate(getSampleRate());
    minBpmResetPeriodCount = (int) (getSampleRate() * DEFAULT_MIN_BPM_RESET_PERIOD_SECONDS);
    lastTriggerSampleCounter = 0;
    sampleCounter = 0;
}

void ThresholdProcessor::resetBpm() {
    heartbeatHelper->reset();
    sampleCounter = 0;
    lastTriggerSampleCounter = 0;
}
