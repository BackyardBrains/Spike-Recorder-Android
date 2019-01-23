//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_AVERAGESPIKEANALYSIS_H
#define SPIKE_RECORDER_ANDROID_AVERAGESPIKEANALYSIS_H

#include <dr_wav.h>
#include <algorithm>
#include <android/log.h>

#include "AnalysisUtils.h"

struct AverageSpikeData {
    float *averageSpike;
    float *normAverageSpike;
    float maxAverageSpike;
    float minAverageSpike;

    float *topSTDLine;
    float *bottomSTDLine;
    float *normTopSTDLine;
    float *normBottomSTDLine;
    float maxStd;
    float minStd;

    int countOfSpikes;

};

class AverageSpikeAnalysis {
public:
    AverageSpikeAnalysis();

    ~AverageSpikeAnalysis();

    void process(const char *filePath, int **inSpikeTrains, int spikeTrainCount, const int *spikeCounts,
                 float **outAverageSpike, float **outNormAverageSpike, float **outNormTopSTDLine,
                 float **outNormBottomSTDLine, int batchSpikeCount);

private:
    static const char *TAG;

    void allocateAverageSpikeData(AverageSpikeData *averageSpikeData, int length, drwav_uint64 batchSpikeCount);

    void deallocateAverageSpikeData(AverageSpikeData *averageSpikeData, int length);
};


#endif //SPIKE_RECORDER_ANDROID_AVERAGESPIKEANALYSIS_H
