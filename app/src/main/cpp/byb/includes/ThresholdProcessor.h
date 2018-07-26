//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_THRESHOLDPROCESSOR_H
#define SPIKE_RECORDER_ANDROID_THRESHOLDPROCESSOR_H

#include "Processor.h"
#include <algorithm>
#include <climits>

namespace processing {
    class ThresholdProcessor;
}

class ThresholdProcessor : public Processor {
public:
    ThresholdProcessor();

    ~ThresholdProcessor();

    void process(const short *inSamples, short *outSamples, const int length);

private:
    static const char *TAG;

    // We shouldn't process more than 2.4 seconds of samples in any given moment
    static constexpr float MAX_PROCESSED_SECONDS = 2.4f;
    // When threshold is hit we should have a dead period of 5ms before checking for next threshold hit
    static constexpr float DEAD_PERIOD_SECONDS = 0.005f;
    // Default sample rate we start with
    static constexpr float DEFAULT_SAMPLE_RATE = 44100.0f;
    // Max number of unfinished samples arrays
    static const int UNFINISHED_SAMPLES_COUNT = 200;

    // Resets all the fields used for calculations
    void reset();

    // Number of samples that we collect for one sample stream
    int sampleCount = (int) (DEFAULT_SAMPLE_RATE * MAX_PROCESSED_SECONDS);
    // We need to buffer half of samples total count up to the sample that hit's threshold
    int bufferSampleCount = sampleCount / 2;
    // Holds most recent 1.2 seconds of audio so we can prepend new sample buffers when threshold is hit
    short *buffer;
    // Holds arrays of already populated and averaged samples
    short *samplesForCalculation;
    // Number of arrays of already populated and averaged samples
    int samplesForCalculationCount;
    // Holds arrays of samples that still haven't been fully populated and averaged
    short **unfinishedSamplesForCalculation;
    // Holds counts of samples in unfinished sample arrays
    int *unfinishedSamplesForCalculationCounts;
    // Holds counts of already averaged samples in unfinished sample arrays
    int *unfinishedSamplesForCalculationAveragedCounts;
    // Number of unfinished sample arrays
    int unfinishedSamplesForCalculationCount;

    // Dead period when we don't check for threshold after hitting one
    int deadPeriodCount = (int) (DEFAULT_SAMPLE_RATE * DEAD_PERIOD_SECONDS);
    // Counts samples between two dead periods
    int deadPeriodSampleCounter;
    // Whether we are currently in dead period (not listening for threshold hit)
    bool inDeadPeriod;

    // Threshold value that triggers the averaging
    int triggerValue = INT_MAX;
    // Used to check whether threshold trigger value has changed since the last incoming sample batch
    int lastTriggeredValue;
    // Number of samples that needs to be summed to get the averaged sample
    int averagedSampleCount;
    // Used to check whether number of averages samples has changed since the last incoming sample batch
    int lastAveragedSampleCount;
    // Used to check whether sample rate has changed since the last incoming sample batch
    float lastSampleRate;

    // Holds previously processed sample so we can compare whether we have a threshold hit
    short prevSample;

};


#endif //SPIKE_RECORDER_ANDROID_THRESHOLDPROCESSOR_H
