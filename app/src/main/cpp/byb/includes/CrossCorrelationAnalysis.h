//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_CROSSCORRELATIONANALYSIS_H
#define SPIKE_RECORDER_ANDROID_CROSSCORRELATIONANALYSIS_H

#include <algorithm>

namespace backyardbrains {

    namespace analysis {

        class CrossCorrelationAnalysis {
        public:
            CrossCorrelationAnalysis();

            ~CrossCorrelationAnalysis();

            void process(float **inSpikeTrains, int spikeTrainCount, const int *spikeCounts,
                         int **outAnalysis, int analysisBinCount);

        private:
            static const char *TAG;

            static constexpr float MAX_TIME = 0.1f;
            static constexpr float BIN_SIZE = 0.001f;
            static constexpr float MIN_EDGE = -MAX_TIME - BIN_SIZE * 0.5f;
            static constexpr float MAX_EDGE = MAX_TIME + BIN_SIZE * 0.5f;
            static constexpr float DIVIDER = 1 / BIN_SIZE;

        };
    }
}

#endif //SPIKE_RECORDER_ANDROID_CROSSCORRELATIONANALYSIS_H
