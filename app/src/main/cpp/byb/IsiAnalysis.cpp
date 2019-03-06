//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include <AnalysisUtils.h>
#include "IsiAnalysis.h"

namespace backyardbrains {

    namespace analysis {

        const char *IsiAnalysis::TAG = "IsiAnalysis";

        IsiAnalysis::IsiAnalysis() = default;

        IsiAnalysis::~IsiAnalysis() = default;

        void IsiAnalysis::process(float **inSpikeTrains, const int spikeTrainCount, const int *spikeCounts,
                                  int **outAnalysis,
                                  const int analysisBinCount) {
            const float *logSpace = backyardbrains::utils::AnalysisUtils::generateLogSpace(-3, 1, BIN_COUNT - 1);

            float diff;
            for (int i = 0; i < spikeTrainCount; i++) {
                int *histogram = new int[analysisBinCount]{0};

                for (int j = 1; j < spikeCounts[i]; j++) {
                    diff = inSpikeTrains[i][j] - inSpikeTrains[i][j - 1];
                    for (int k = 1; k < analysisBinCount; k++) {
                        if (diff >= logSpace[k - 1] && diff < logSpace[k]) {
                            histogram[k - 1]++;
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