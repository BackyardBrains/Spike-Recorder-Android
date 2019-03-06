//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include "AutocorrelationAnalysis.h"

namespace backyardbrains {

    namespace analysis {

        const char *AutocorrelationAnalysis::TAG = "AutocorrelationAnalysis";

        AutocorrelationAnalysis::AutocorrelationAnalysis() = default;

        AutocorrelationAnalysis::~AutocorrelationAnalysis() = default;

        void AutocorrelationAnalysis::process(float **inSpikeTrains, const int spikeTrainCount, const int *spikeCounts,
                                              int **outAnalysis, const int analysisBinCount) {
            float diff;
            for (int i = 0; i < spikeTrainCount; i++) {
                int *histogram = new int[analysisBinCount]{0};

                for (int j = 0; j < spikeCounts[i]; j++) {
                    // check on left of spike
                    for (int k = j; k >= 0; k--) {
                        diff = inSpikeTrains[i][j] - inSpikeTrains[i][k];
                        if (diff > MIN_EDGE && diff < MAX_EDGE) {
                            histogram[(int) (((diff - MIN_EDGE) / BIN_SIZE))]++;
                        } else {
                            break;
                        }
                    }
                    // check on right of spike
                    for (int k = j + 1; k < spikeCounts[i]; k++) {
                        diff = inSpikeTrains[i][j] - inSpikeTrains[i][k];
                        if (diff > MIN_EDGE && diff < MAX_EDGE) {
                            histogram[(int) (((diff - MIN_EDGE) / BIN_SIZE))]++;
                        } else {
                            break;
                        }
                    }

                }

                std::copy(histogram, histogram + analysisBinCount, outAnalysis[i]);

                delete[] histogram;
            }
        }
    }
}