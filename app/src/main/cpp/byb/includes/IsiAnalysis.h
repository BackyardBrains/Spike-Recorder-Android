//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_ISIANALYSIS_H
#define SPIKE_RECORDER_ANDROID_ISIANALYSIS_H

#include <algorithm>

namespace analysis {
    class IsiAnalysis;
}

class IsiAnalysis {
public:
    IsiAnalysis();

    ~IsiAnalysis();

    void process(float **inSpikeTrains, const int spikeTrainCount, const int *spikeCounts,
                 int **outAnalysis, const int analysisBinCount);

private:
    static const char *TAG;

    static constexpr int BIN_COUNT = 100;
};


#endif //SPIKE_RECORDER_ANDROID_ISIANALYSIS_H
