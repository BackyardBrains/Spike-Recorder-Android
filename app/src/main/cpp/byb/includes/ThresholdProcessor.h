//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_THRESHOLDPROCESSOR_H
#define SPIKE_RECORDER_ANDROID_THRESHOLDPROCESSOR_H

#include <algorithm>
#include <climits>

#include "Processor.h"
#include "HeartbeatHelper.h"

namespace processing {
    class ThresholdProcessor;
}

class ThresholdProcessor : public Processor {
public:
    ThresholdProcessor(OnHeartbeatListener *listener);

    ~ThresholdProcessor();

    void setSampleRate(float sampleRate);

    int getSampleCount();

    // Returns the number of sample sequences that should be summed to get the average spike value.
    int getAveragedSampleCount();

    // Sets the number of sample sequences that should be summed to get the average spike value.
    void setAveragedSampleCount(int averagedSampleCount);

    // Set's the sample frequency threshold.
    void setThreshold(int threshold);

    // Resets all the fields used for calculations when next batch comes
    void resetThreshold();

    void setPaused(bool paused);

    // Sets current averaging trigger type
    void setTriggerType(int triggerType);

    // Starts/stops processing heartbeat
    void setBpmProcessing(bool processBpm);

    void process(short *outSamples, const short *inSamples, const int inSampleCount, const int *inEventIndices,
                 const int *inEvents, const int inEventCount);

private:
    static const char *TAG;

    // We shouldn't process more than 2.4 seconds of samples in any given moment
    static constexpr float MAX_PROCESSED_SECONDS = 2.4f;
    // When threshold is hit we should have a dead period of 5ms before checking for next threshold hit
    static constexpr float DEAD_PERIOD_SECONDS = 0.005f;
    // Default sample rate we start with
    static constexpr float DEFAULT_SAMPLE_RATE = 44100.0f;
    // Default number of samples that needs to be summed to get the averaged sample
    static constexpr int DEFAULT_AVERAGED_SAMPLE_COUNT = 1;
    // Max number of unfinished samples arrays
    static const int UNFINISHED_SAMPLES_COUNT = 200;
    // Minimum number of seconds without a heartbeat before resetting the heartbeat helper
    static constexpr double DEFAULT_MIN_BPM_RESET_PERIOD_SECONDS = 3;

    // Constants that define we are currently averaging when threshold is hit
    static constexpr int TRIGGER_ON_THRESHOLD = -1;
    // Constants that define we are currently averaging on all events
    static constexpr int TRIGGER_ON_EVENTS = 0;

    // Resets all the fields used for calculations
    void reset();

    // Prepares new sample collection for averaging
    void prepareNewSamples(const short *inSamples, int length, int index);

    // Resets all local variables used for the heartbeat processing
    void resetBpm();

    // Number of samples that we collect for one sample stream
    int sampleCount = (int) (DEFAULT_SAMPLE_RATE * MAX_PROCESSED_SECONDS);
    // Threshold value that triggers the averaging
    int triggerValue = INT_MAX;
    // Used to check whether threshold trigger value has changed since the last incoming sample batch
    int lastTriggeredValue = 0;
    // Number of samples that needs to be summed to get the averaged sample
    int averagedSampleCount = DEFAULT_AVERAGED_SAMPLE_COUNT;
    // Used to check whether number of averages samples has changed since the last incoming sample batch
    int lastAveragedSampleCount = 0;
    // Used to check whether sample rate has changed since the last incoming sample batch
    float lastSampleRate = 0;
    // Whether buffers need to be reset before processing next batch
    bool resetOnNextBatch = false;

    // We need to buffer half of samples total count up to the sample that hit's threshold
    int bufferSampleCount = sampleCount / 2;
    // Buffer that holds most recent 1.2 ms of audio so we can prepend new sample buffers when threshold is hit
    short *buffer = new short[bufferSampleCount];
    // Holds arrays of already populated and averaged samples
    short **samplesForCalculation = new short *[averagedSampleCount];
    // Number of arrays of already populated and averaged samples
    int samplesForCalculationCount = 0;
    // Holds arrays of samples that still haven't been fully populated and averaged
    short **unfinishedSamplesForCalculation = new short *[UNFINISHED_SAMPLES_COUNT];
    // Holds counts of samples in unfinished sample arrays
    int *unfinishedSamplesForCalculationCounts = new int[UNFINISHED_SAMPLES_COUNT]{0};
    // Holds counts of already averaged samples in unfinished sample arrays
    int *unfinishedSamplesForCalculationAveragedCounts = new int[UNFINISHED_SAMPLES_COUNT]{0};
    // Number of unfinished sample arrays
    int unfinishedSamplesForCalculationCount = 0;
    // Holds averages of all the saved samples by index
    short *averagedSamples = new short[sampleCount]{0};
    // Holds sums of all the saved samples by index
    int *summedSamples = new int[sampleCount]{0};
    // Holds samples counts summed at specified position
    int *summedSamplesCounts = new int[sampleCount]{0};

    // Dead period when we don't check for threshold after hitting one
    int deadPeriodCount = (int) (DEFAULT_SAMPLE_RATE * DEAD_PERIOD_SECONDS);
    // Counts samples between two dead periods
    int deadPeriodSampleCounter;
    // Whether we are currently in dead period (not listening for threshold hit)
    bool inDeadPeriod;

    // Holds previously processed sample so we can compare whether we have a threshold hit
    short prevSample;

    // Whether threshold is currently paused or not. If paused, processing returns values as if the threshold is always reset.
    bool paused = false;

    // Current type of trigger we're averaging on
    int triggerType = TRIGGER_ON_THRESHOLD;

    // Holds reference to HeartbeatHelper that processes threshold hits as heart beats
    HeartbeatHelper *heartbeatHelper;
    // Period without heartbeat that we wait for before resetting the heartbeat helper
    int minBpmResetPeriodCount = (int) (DEFAULT_SAMPLE_RATE * DEFAULT_MIN_BPM_RESET_PERIOD_SECONDS);
    // Index of the sample that triggered the threshold hit
    int lastTriggerSampleCounter;
    // Counts samples between two resets that need to be passed to heartbeat helper
    int sampleCounter;
    // Whether BPM should be processed or not
    bool processBpm;
};


#endif //SPIKE_RECORDER_ANDROID_THRESHOLDPROCESSOR_H
