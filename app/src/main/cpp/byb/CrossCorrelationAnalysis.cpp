//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include "CrossCorrelationAnalysis.h"

const char *CrossCorrelationAnalysis::TAG = "CrossCorrelationAnalysis";

CrossCorrelationAnalysis::CrossCorrelationAnalysis() {

}

CrossCorrelationAnalysis::~CrossCorrelationAnalysis() {

}

void
CrossCorrelationAnalysis::process(float **inSpikeTrains, int spikeTrainCount, const int *spikeCounts, int **outAnalysis,
                                  int analysisBinCount) {
    float diff;
    for (int i = 0; i < spikeTrainCount; i++) {
        for (int j = 0; j < spikeTrainCount; j++) {
            int *histogram = new int[analysisBinCount]{0};

            if (spikeCounts[i] > 1 && spikeCounts[j] > 1) {
                bool insideInterval;
                // go through first spike train
                for (int k = 0; k < spikeCounts[i]; k++) {
                    // check on left of spike
                    insideInterval = false;
                    // go through second spike train
                    for (int l = 0; l < spikeCounts[j]; l++) {
                        diff = inSpikeTrains[i][k] - inSpikeTrains[j][l];
                        if (diff > MIN_EDGE && diff < MAX_EDGE) {
                            insideInterval = true;
                            histogram[(int) (((diff - MIN_EDGE) * DIVIDER))]++;
                        } else if (insideInterval) { //we pass last spike that is in interval of interest
                            break;
                        }
                    }
                }
            }

            std::copy(histogram, histogram + analysisBinCount, outAnalysis[i * spikeTrainCount + j]);

            delete[] histogram;
        }
    }
}
