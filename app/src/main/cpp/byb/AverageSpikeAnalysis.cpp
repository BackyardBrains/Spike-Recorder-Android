//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include <tgmath.h>
#include "AverageSpikeAnalysis.h"

namespace backyardbrains {

    namespace analysis {

        const char *AverageSpikeAnalysis::TAG = "AverageSpikeAnalysis";

        AverageSpikeAnalysis::AverageSpikeAnalysis() = default;

        AverageSpikeAnalysis::~AverageSpikeAnalysis() = default;

        void AverageSpikeAnalysis::process(const char *filePath, int **inSpikeTrains, const int spikeTrainCount,
                                           const int *spikeCounts, float **outAverageSpike, float **outNormAverageSpike,
                                           float **outNormTopSTDLine, float **outNormBottomSTDLine,
                                           int batchSpikeCount) {
            // open audio file we need to analyze
            drwav *wavPtr = drwav_open_file(filePath);
            if (wavPtr == nullptr) {
                __android_log_print(ANDROID_LOG_DEBUG, TAG, "Unable to open file: %s", filePath);
                return;
            }

            // check whether file is long enough for processing
            drwav_uint64 totalSamples = wavPtr->totalSampleCount;
            auto batchSpikeHalfCount = static_cast<drwav_uint64>(batchSpikeCount / 2);
            auto bsc = static_cast<drwav_uint64>(batchSpikeCount);

            auto *tmpAvr = new AverageSpikeData[spikeTrainCount];
            allocateAverageSpikeData(tmpAvr, spikeTrainCount, bsc);

            int sampleIndex;
            drwav_uint64 read;
            drwav_uint64 spikeIndexBatchHead;
            auto *samples = new drwav_int16[batchSpikeCount];
            for (int i = 0; i < spikeTrainCount; i++) {
                for (int j = 0; j < spikeCounts[i]; j++) {
                    sampleIndex = inSpikeTrains[i][j];
                    // if we cannot make a batch of 4ms go to next sample
                    if ((sampleIndex + batchSpikeHalfCount) >= totalSamples ||
                        static_cast<int>(sampleIndex - batchSpikeHalfCount) < 0) {
                        continue;
                    }

                    // add spike to average buffer
                    spikeIndexBatchHead = sampleIndex - batchSpikeHalfCount;
                    drwav_seek_to_sample(wavPtr, spikeIndexBatchHead);
                    if ((read = drwav_read_s16(wavPtr, bsc, samples)) > 0) {
                        for (int k = 0; k < read; k++) {
                            tmpAvr[i].averageSpike[k] += samples[k];
                            tmpAvr[i].topSTDLine[k] += pow(samples[k], 2);
                        }
                    }
                    tmpAvr[i].countOfSpikes++;
                }
            }
            delete[] samples;

            // close audio file
            drwav_close(wavPtr);

            float min;
            float max;
            float divider;
            auto *tmp = new float[batchSpikeCount];
            // divide sum of spikes with number of spikes and find max and min
            for (int i = 0; i < spikeTrainCount; i++) {
                if (tmpAvr[i].countOfSpikes > 1) {
                    divider = (float) tmpAvr[i].countOfSpikes;
                    min = FLT_MAX;
                    max = FLT_MIN;
                    for (int j = 0; j < batchSpikeCount; j++) {
                        tmpAvr[i].averageSpike[j] /= divider;
                        if (tmpAvr[i].averageSpike[j] > max) max = tmpAvr[i].averageSpike[j];
                        if (tmpAvr[i].averageSpike[j] < min) min = tmpAvr[i].averageSpike[j];
                    }
                    tmpAvr[i].maxAverageSpike = max;
                    tmpAvr[i].minAverageSpike = min;

                    for (int j = 0; j < batchSpikeCount; j++) {
                        tmpAvr[i].topSTDLine[j] /= divider;
                        tmp[j] = tmpAvr[i].averageSpike[j] * tmpAvr[i].averageSpike[j];
                    }
                    for (int j = 0; j < batchSpikeCount; j++) {
                        tmp[j] = tmpAvr[i].topSTDLine[j] - tmp[j];
                    }

                    // calculate SD from variance
                    for (int k = 0; k < batchSpikeCount; k++) {
                        tmp[k] = sqrt(tmp[k]);
                    }

                    // Make top line and bottom line around mean that represent one SD deviation from mean
                    for (int j = 0; j < batchSpikeCount; j++) {
                        tmpAvr[i].bottomSTDLine[j] = tmpAvr[i].averageSpike[j] - tmp[j];
                        tmpAvr[i].topSTDLine[j] = tmpAvr[i].averageSpike[j] + tmp[j];
                    }

                    // Find max and min of top and bottom std line respectively
                    tmpAvr[i].minStd = FLT_MAX;
                    tmpAvr[i].maxStd = FLT_MIN;
                    for (int j = 0; j < batchSpikeCount; j++) {
                        if (tmpAvr[i].maxStd < tmpAvr[i].topSTDLine[j]) tmpAvr[i].maxStd = tmpAvr[i].topSTDLine[j];
                        if (tmpAvr[i].minStd > tmpAvr[i].bottomSTDLine[j]) {
                            tmpAvr[i].minStd = tmpAvr[i].bottomSTDLine[j];
                        }
                    }
                }
                min = fmin(tmpAvr[i].minStd, tmpAvr[i].minAverageSpike);
                max = fmax(tmpAvr[i].maxStd, tmpAvr[i].maxAverageSpike);
                backyardbrains::utils::AnalysisUtils::map(tmpAvr[i].averageSpike, tmpAvr[i].normAverageSpike, batchSpikeCount,
                                                          min, max, 0.0f, 1.0f);
                backyardbrains::utils::AnalysisUtils::map(tmpAvr[i].topSTDLine, tmpAvr[i].normTopSTDLine, batchSpikeCount, min,
                                                          max, 0.0f, 1.0f);
                backyardbrains::utils::AnalysisUtils::map(tmpAvr[i].bottomSTDLine, tmpAvr[i].normBottomSTDLine, batchSpikeCount,
                                                          min, max, 0.0f, 1.0f);
            }
            delete[] tmp;

            for (int i = 0; i < spikeTrainCount; i++) {
                std::copy(tmpAvr[i].averageSpike, tmpAvr[i].averageSpike + batchSpikeCount, outAverageSpike[i]);
                std::copy(tmpAvr[i].normAverageSpike, tmpAvr[i].normAverageSpike + batchSpikeCount,
                          outNormAverageSpike[i]);
                std::copy(tmpAvr[i].normTopSTDLine, tmpAvr[i].normTopSTDLine + batchSpikeCount, outNormTopSTDLine[i]);
                std::copy(tmpAvr[i].normBottomSTDLine, tmpAvr[i].normBottomSTDLine + batchSpikeCount,
                          outNormBottomSTDLine[i]);
            }

            deallocateAverageSpikeData(tmpAvr, spikeTrainCount);
            delete[] tmpAvr;
        }

        void AverageSpikeAnalysis::allocateAverageSpikeData(AverageSpikeData *averageSpikeData, const int length,
                                                            drwav_uint64 batchSpikeCount) {
            for (int i = 0; i < length; i++) {
                averageSpikeData[i].averageSpike = new float[batchSpikeCount]{0};
                averageSpikeData[i].topSTDLine = new float[batchSpikeCount]{0};
                averageSpikeData[i].bottomSTDLine = new float[batchSpikeCount]{0};

                averageSpikeData[i].normAverageSpike = new float[batchSpikeCount]{0};
                averageSpikeData[i].normTopSTDLine = new float[batchSpikeCount]{0};
                averageSpikeData[i].normBottomSTDLine = new float[batchSpikeCount]{0};

                averageSpikeData[i].countOfSpikes = 0;
            }
        }

        void AverageSpikeAnalysis::deallocateAverageSpikeData(AverageSpikeData *averageSpikeData, const int length) {
            for (int i = 0; i < length; i++) {
                delete[] averageSpikeData[i].averageSpike;
                delete[] averageSpikeData[i].topSTDLine;
                delete[] averageSpikeData[i].bottomSTDLine;

                delete[] averageSpikeData[i].normAverageSpike;
                delete[] averageSpikeData[i].normTopSTDLine;
                delete[] averageSpikeData[i].normBottomSTDLine;
            }
        }
    }
}