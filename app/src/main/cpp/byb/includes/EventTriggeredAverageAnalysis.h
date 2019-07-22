//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_EVENTTRIGGEREDAVERAGEANALYSIS_H
#define SPIKE_RECORDER_ANDROID_EVENTTRIGGEREDAVERAGEANALYSIS_H

#include <functional>
#include <string>
#include <android/log.h>
#include <limits>
#include <random>
#include <algorithm>

#include "dr_wav.h"
#include "AnalysisUtils.h"
#include "SignalUtils.h"
#include "EventUtils.h"

using namespace std;

namespace backyardbrains {

    namespace analysis {

        class EventTriggeredAverageAnalysis {
        public:
            EventTriggeredAverageAnalysis();

            ~EventTriggeredAverageAnalysis();

            void process(const char *signalFilePath, const char *eventsFilePath, const string *processedEvents,
                         const int processedEventCount, const bool removeNoiseIntervals,
                         const char *confidenceIntervalsEvent, float ***averages, float ***normAverages,
                         float **normMcAverages, float **normMcTop, float **normMcBottom, float *min, float *max);

        private:
            static const char *TAG;

            static constexpr float EVENT_LEFT_OFFSET_IN_SECS = 0.7f;
            static constexpr float EVENT_RIGHT_OFFSET_IN_SECS = 0.7f;
            static constexpr float NOISE_DETECTION_STD_THRESHOLD = 2.5f;
            static constexpr float SIGNAL_STD_MULTIPLYER = 2.0f;
            static constexpr int CONFIDENCE_INTERVALS_COMPUTATION_REPETITION = 100;

            // total number of samples
            drwav_uint64 totalSamples;
            // sample rate
            drwav_uint32 sampleRate;
            // channel count
            drwav_uint16 channelCount;
            // number of samples on the left that should go in the buffer
            drwav_uint64 leftOffsetSampleCount;
            // number of samples on the right that should go in the buffer
            drwav_uint64 rightOffsetSampleCount;
            // size of the currently processed sample batch
            drwav_uint64 bufferSize;
            // buffer size cast to avoid multiple casting
            int sampleCount;
            // number of frames in the currently processed sample batch
            int frameCount;


            long long currentTimeInMilliseconds();

            // Returns start index of the interval that surrounds sample with the specified sampleIndex
            // if interval has valid bounds, or -1 if it's not
            bool validateIntervalBounds(drwav_uint64 &start, drwav_uint64 sampleIndex);

            bool
            addInterval(drwav *wavPtr, float **sums, drwav_uint64 start, drwav_uint64 size, bool checkNoise, double std,
                        double mean);

            void averageInterval(float **averages, float **sums, int divider, float *min, float *max);

            void findStdAndMean(drwav *wavPtr, double &std, double &mean);
        };
    }
}

#endif //SPIKE_RECORDER_ANDROID_EVENTTRIGGEREDAVERAGEANALYSIS_H
