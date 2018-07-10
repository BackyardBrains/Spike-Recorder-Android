//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_SPIKEANALYSIS_H
#define SPIKE_RECORDER_ANDROID_SPIKEANALYSIS_H

#include <functional>
#include <climits>
#include <algorithm>
#include <math.h>
#include <android/log.h>
#include <sys/time.h>

#include "dr_wav.h"
#include "AnalysisUtils.h"

namespace analysis {
    class SpikeAnalysis;
}

class SpikeAnalysis {
public:
    SpikeAnalysis();

    ~SpikeAnalysis();

    int *findSpikes(const char *filePath, short *outValuesPos, int *outIndicesPos, float *outTimesPos,
                    short *outValuesNeg, int *outIndicesNeg, float *outTimesNeg);

private:
    static const char *TAG;

    static constexpr float BUFFER_SIZE_IN_SECS = 12.0f;
    static constexpr float MIN_VALID_FILE_LENGTH_IN_SECS = 0.2f;
    static constexpr float BIN_COUNT = 200.0f;

    static constexpr int SCHMITT_ON = 1;
    static constexpr int SCHMITT_OFF = 2;
    static constexpr float KILL_INTERVAL = 0.005f; // 5ms

    long long currentTimeInMilliseconds();
};


#endif //SPIKE_RECORDER_ANDROID_SPIKEANALYSIS_H
