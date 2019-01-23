//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_AUTOCORRELATIONANALYSIS_H
#define SPIKE_RECORDER_ANDROID_AUTOCORRELATIONANALYSIS_H

#include <algorithm>

class AutocorrelationAnalysis {
public:
    AutocorrelationAnalysis();

    ~AutocorrelationAnalysis();

    void process(float **inSpikeTrains, int spikeTrainCount, const int *spikeCounts,
                 int **outAnalysis, int analysisBinCount);

private:
    static const char *TAG;

    static constexpr float MAX_TIME = 0.1f; // 100ms
    static constexpr float BIN_SIZE = 0.001f; // 1ms
    static constexpr float MIN_EDGE = -BIN_SIZE * 0.5f; // -.5ms
    static constexpr float MAX_EDGE = MAX_TIME + BIN_SIZE * 0.5f; // 100.5ms
};


#endif //SPIKE_RECORDER_ANDROID_AUTOCORRELATIONANALYSIS_H
