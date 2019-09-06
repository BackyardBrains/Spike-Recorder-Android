//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_SPIKEANALYSIS_H
#define SPIKE_RECORDER_ANDROID_SPIKEANALYSIS_H

#include <functional>
#include <algorithm>
#include <android/log.h>

#include "dr_wav.h"
#include "AnalysisUtils.h"
#include "SignalUtils.h"

namespace backyardbrains {

    namespace analysis {

        class SpikeAnalysis {
        public:
            SpikeAnalysis();

            ~SpikeAnalysis();

            void findSpikes(const char *filePath, short **outValuesPos, int **outIndicesPos, float **outTimesPos,
                            short **outValuesNeg, int **outIndicesNeg, float **outTimesNeg, int *outPosCounts,
                            int *outNegCounts);

        private:
            static const char *TAG;

            static constexpr int SCHMITT_ON = 1;
            static constexpr int SCHMITT_OFF = 2;
            static constexpr float KILL_INTERVAL = 0.005f; // 5ms

            long long currentTimeInMilliseconds();
        };
    }
}

#endif //SPIKE_RECORDER_ANDROID_SPIKEANALYSIS_H
