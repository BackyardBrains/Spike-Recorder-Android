//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include "AutocorrelationAnalysis.h"

const char *AutocorrelationAnalysis::TAG = "AutocorrelationAnalysis";

AutocorrelationAnalysis::AutocorrelationAnalysis() {

}

AutocorrelationAnalysis::~AutocorrelationAnalysis() {

}

void AutocorrelationAnalysis::process(float **inSpikeTrains, int spikeTrainCount, const int *spikeCounts,
                                      int **outAnalysis, int analysisBinCount) {
    float diff;
    int mainIndex, secIndex;
    int spikeCount;

    for (int i = 0; i < spikeTrainCount; i++) {
        spikeCount = spikeCounts[i];

        int *histogram = new int[analysisBinCount + 1]{0};

        for (mainIndex = 0; mainIndex < spikeCount; mainIndex++) {
            // check on left of spike
            for (secIndex = mainIndex; secIndex >= 0; secIndex--) {
                diff = inSpikeTrains[i][mainIndex] - inSpikeTrains[i][secIndex];
                if (diff > MIN_EDGE && diff < MAX_EDGE) {
                    histogram[(int) (((diff - MIN_EDGE) / BIN_SIZE))]++;
                } else {
                    break;
                }
            }
            // check on right of spike
            for (secIndex = mainIndex + 1; secIndex < spikeCount; secIndex++) {
                diff = inSpikeTrains[i][mainIndex] - inSpikeTrains[i][secIndex];
                if (diff > MIN_EDGE && diff < MAX_EDGE) {
                    histogram[(int) (((diff - MIN_EDGE) / BIN_SIZE))]++;
                } else {
                    break;
                }
            }

        }

        std::copy(histogram + 1, histogram + 1 + analysisBinCount, outAnalysis[i]);

        delete[] histogram;
    }
}
