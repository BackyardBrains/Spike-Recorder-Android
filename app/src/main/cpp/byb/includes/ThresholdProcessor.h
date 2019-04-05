//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_THRESHOLDPROCESSOR_H
#define SPIKE_RECORDER_ANDROID_THRESHOLDPROCESSOR_H

#include <algorithm>
#include <climits>

#include "Processor.h"
#include "HeartbeatHelper.h"

namespace backyardbrains {

    namespace processing {

        class ThresholdProcessor : public Processor {
        public:
            static constexpr int DEFAULT_SAMPLE_COUNT = static_cast<const int>(2.4f * 44100.0f);

            explicit ThresholdProcessor(backyardbrains::utils::OnHeartbeatListener *listener);

            ~ThresholdProcessor() override;

            // Returns the number of sample sequences that should be summed to get the average spike value.
            int getAveragedSampleCount();

            // Sets the number of sample sequences that should be summed to get the average spike value.
            void setAveragedSampleCount(int averagedSampleCount);

            // Sets the sample frequency threshold.
            void setThreshold(float threshold);

            // Resets all the fields used for calculations when next batch comes
            void resetThreshold();

            void setPaused(bool paused);

            // Returns current averaging trigger type
            int getTriggerType();

            // Sets current averaging trigger type
            void setTriggerType(int triggerType);

            // Starts/stops processing heartbeat
            void setBpmProcessing(bool processBpm);

            void process(short **outSamples, int *outSamplesCounts, short **inSamples, const int *inSampleCounts,
                         const int *inEventIndices, const int *inEvents, int inEventCount);

            void appendIncomingSamples(short **inSamples, int *inSampleCounts);

        private:
            static const char *TAG;

            // We shouldn't process more than 2.4 seconds of samples in any given moment
            static constexpr float MAX_PROCESSED_SECONDS = 2.4f;
            // When threshold is hit we should have a dead period of 5ms before checking for next threshold hit
            static constexpr float DEAD_PERIOD_SECONDS = 0.005f;
            // Default number of samples that needs to be summed to get the averaged sample
            static constexpr int DEFAULT_AVERAGED_SAMPLE_COUNT = 1;
            // Minimum number of seconds without a heartbeat before resetting the heartbeat helper
            static constexpr double DEFAULT_MIN_BPM_RESET_PERIOD_SECONDS = 3;

            // Constants that define we are currently averaging when threshold is hit
            static constexpr int TRIGGER_ON_THRESHOLD = -1;
            // Constants that define we are currently averaging on all events
            static constexpr int TRIGGER_ON_EVENTS = 0;

            // Prepares new sample collection for averaging
            void prepareNewSamples(const short *inSamples, int length, int channelIndex, int sampleIndex);

            // Creates and initializes all the fields used for calculations
            void init(bool resetLocalBuffer);

            // Deletes all array fields used for calculations
            void clean(int channelCount, bool resetLocalBuffer);

            // Resets all local variables used for the heartbeat processing
            void resetBpm();

            // Number of samples that we collect for one sample stream
            int sampleCount = 0;
            // Used to check whether channel has changed since the last incoming sample batch
            int lastSelectedChannel = 0;
            // Threshold value that triggers the averaging
            float *triggerValue;
            // Used to check whether threshold trigger value has changed since the last incoming sample batch
            float *lastTriggeredValue;
            // Number of samples that needs to be summed to get the averaged sample
            int averagedSampleCount = DEFAULT_AVERAGED_SAMPLE_COUNT;
            // Used to check whether number of averages samples has changed since the last incoming sample batch
            int lastAveragedSampleCount = 0;
            // Used to check whether sample rate has changed since the last incoming sample batch
            float lastSampleRate = 0;
            // Used to check whether chanel count has changed since the last incoming sample batch
            int lastChannelCount = 0;
            // Whether buffers need to be reset before processing next batch
            bool resetOnNextBatch = false;

            // We need to buffer half of samples total count up to the sample that hit's threshold
            int bufferSampleCount = sampleCount / 2;
            // Buffer that holds most recent 1.2 ms of audio so we can prepend new sample buffers when threshold is hit
            short **buffer;
            // Holds number of sample rows that have been averaged
            int *samplesForCalculationCount;
            // Holds number of samples in each sample row
            int **samplesForCalculationCounts;
            // Holds sample rows
            short ***samplesForCalculation;
            // Holds number of samples that have been summed at specified position
            int **summedSamplesCounts;
            // Holds sums of all the samples at specified position
            int **summedSamples;
            // Holds averages of all the samples at specified position
            short **averagedSamples;

            // Dead period when we don't check for threshold after hitting one
            int deadPeriodCount = 0;
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
            backyardbrains::utils::HeartbeatHelper *heartbeatHelper;
            // Period without heartbeat that we wait for before resetting the heartbeat helper
            int minBpmResetPeriodCount = 0;
            // Index of the sample that triggered the threshold hit
            int lastTriggerSampleCounter;
            // Counts samples between two resets that need to be passed to heartbeat helper
            int sampleCounter;
            // Whether BPM should be processed or not
            bool processBpm;
        };
    }
}

#endif //SPIKE_RECORDER_ANDROID_THRESHOLDPROCESSOR_H
