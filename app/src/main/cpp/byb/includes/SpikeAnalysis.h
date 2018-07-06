//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_SPIKEANALYSIS_H
#define SPIKE_RECORDER_ANDROID_SPIKEANALYSIS_H

#include <climits>
#include <algorithm>
#include <android/log.h>

namespace analysis {
    class SpikeAnalysis;
}

class SpikeAnalysis {
public:
    SpikeAnalysis();

    ~SpikeAnalysis();

    int *findSpikes(const short *samples, int sampleCount, float sampleRate, short *valuesPos, int *indicesPos,
                    float *timesPos, int startIndexPos, int acceptablePos, short *valuesNeg, int *indicesNeg,
                    float *timesNeg, int startIndexNeg, int acceptableNeg);

    int *filterSpikes(short *valuesPos, int *indicesPos, float *timesPos, int positivesCount, short *valuesNeg,
                      int *indicesNeg, float *timesNeg, int negativesCount);

private:
    static const char *TAG;

    static constexpr int SCHMITT_ON = 1;
    static constexpr int SCHMITT_OFF = 2;
    static constexpr float KILL_INTERVAL = 0.005f; // 5ms

    int schmittPosState = SCHMITT_OFF;
    int schmittNegState = SCHMITT_OFF;
    short maxPeakValue = SHRT_MIN;
    int maxPeakIndex = 0;
    short minPeakValue = SHRT_MAX;
    int minPeakIndex = 0;
    float maxPeakTime = 0.0f;
    float minPeakTime = 0.0f;

    float currentTime = 0.0f;
    int index = 0;
    short sample;
};


#endif //SPIKE_RECORDER_ANDROID_SPIKEANALYSIS_H
