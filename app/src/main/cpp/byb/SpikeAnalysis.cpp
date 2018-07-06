//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include "SpikeAnalysis.h"

const char *SpikeAnalysis::TAG = "SpikeAnalysis";

SpikeAnalysis::SpikeAnalysis() {
}

SpikeAnalysis::~SpikeAnalysis() {
}

int *SpikeAnalysis::findSpikes(const short *inSamples, int sampleCount, float sampleRate, short *outValuesPos,
                               int *outIndicesPos, float *outTimesPos, int startIndexPos, int acceptablePos,
                               short *outValuesNeg, int *outIndicesNeg, float *outTimesNeg, int startIndexNeg,
                               int acceptableNeg) {
    float sampleRateDivider = (float) 1 / sampleRate;
    int peaksCounterPos = 0;
    int peaksCounterNeg = 0;
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < sampleCount; i++) {
        sample = inSamples[i];
        // determine state of positive schmitt trigger
        if (schmittPosState == SCHMITT_OFF) {
            if (sample > acceptablePos) {
                schmittPosState = SCHMITT_ON;
                maxPeakValue = SHRT_MIN;
            }
        } else {
            if (sample < 0) {
                schmittPosState = SCHMITT_OFF;
                outValuesPos[startIndexPos + peaksCounterPos] = maxPeakValue;
                outIndicesPos[startIndexPos + peaksCounterPos] = maxPeakIndex;
                outTimesPos[startIndexPos + peaksCounterPos] = maxPeakTime;
                peaksCounterPos++;
            } else if (sample > maxPeakValue) {
                maxPeakValue = sample;
                maxPeakIndex = index;
                maxPeakTime = currentTime;
            }
        }

        // determine state of negative schmitt trigger
        if (schmittNegState == SCHMITT_OFF) {
            if (sample < acceptableNeg) {
                schmittNegState = SCHMITT_ON;
                minPeakValue = SHRT_MAX;
            }
        } else {
            if (sample > 0) {
                schmittNegState = SCHMITT_OFF;
                outValuesNeg[startIndexNeg + peaksCounterNeg] = minPeakValue;
                outIndicesNeg[startIndexNeg + peaksCounterNeg] = minPeakIndex;
                outTimesNeg[startIndexNeg + peaksCounterNeg] = minPeakTime;
                peaksCounterNeg++;
            } else if (sample < minPeakValue) {
                minPeakValue = sample;
                minPeakIndex = index;
                minPeakTime = currentTime;
            }
        }

        index++;
        currentTime += sampleRateDivider;
    }

    return new int[2]{peaksCounterPos, peaksCounterNeg};
}

int *SpikeAnalysis::filterSpikes(short *outValuesPos, int *outIndicesPos, float *outTimesPos, int positivesCount,
                                 short *outValuesNeg, int *outIndicesNeg, float *outTimesNeg, int negativesCount) {
    int len = positivesCount;
    int removedCounter = 0;
    int removedNegCounter = 0;
    int i;
    if (len > 0) { // Filter positive spikes using kill interval
        for (i = 0; i < len - 1; i++) { // look on the right
            if (outValuesPos[i] < outValuesPos[i + 1]) {
                if ((outTimesPos[i + 1] - outTimesPos[i]) < KILL_INTERVAL) {
                    int numMoved = len - i - 1;
                    if (numMoved > 0) {
                        std::move(outValuesPos + i + 1, outValuesPos + i + numMoved, outValuesPos + i);
                        std::move(outIndicesPos + i + 1, outIndicesPos + i + numMoved, outIndicesPos + i);
                        std::move(outTimesPos + i + 1, outTimesPos + i + numMoved, outTimesPos + i);
                    }
                    len--;
                    removedCounter++;
                    i--;
                }
            }
        }
        len = i;
        for (i = 1; i < len; i++) { // look on the left neighbor
            if (outValuesPos[i] < outValuesPos[i - 1]) {
                if ((outTimesPos[i] - outTimesPos[i - 1]) < KILL_INTERVAL) {
                    int numMoved = len - i - 1;
                    if (numMoved > 0) {
                        std::move(outValuesPos + i + 1, outValuesPos + i + numMoved, outValuesPos + i);
                        std::move(outIndicesPos + i + 1, outIndicesPos + i + numMoved, outIndicesPos + i);
                        std::move(outTimesPos + i + 1, outTimesPos + i + numMoved, outTimesPos + i);
                    }
                    len--;
                    removedCounter++;
                    i--;
                }
            }
        }
    }
    len = negativesCount;
    if (len > 0) { // Filter negative spikes using kill interval
        for (i = 0; i < len - 1; i++) { // look on the right
            if (outValuesNeg[i] > outValuesNeg[i + 1]) {
                if ((outTimesNeg[i + 1] - outTimesNeg[i]) < KILL_INTERVAL) {
                    int numMoved = len - i - 1;
                    if (numMoved > 0) {
                        std::move(outValuesNeg + i + 1, outValuesNeg + i + numMoved, outValuesNeg + i);
                        std::move(outIndicesNeg + i + 1, outIndicesNeg + i + numMoved, outIndicesNeg + i);
                        std::move(outTimesNeg + i + 1, outTimesNeg + i + numMoved, outTimesNeg + i);
                    }
                    len--;
                    removedNegCounter++;
                    i--;
                }
            }
        }
        len = i;
        for (i = 1; i < len; i++) { // look on the left neighbor
            if (outValuesNeg[i] > outValuesNeg[i - 1]) {
                if ((outTimesNeg[i] - outTimesNeg[i - 1]) < KILL_INTERVAL) {
                    int numMoved = len - i - 1;
                    if (numMoved > 0) {
                        std::move(outValuesNeg + i + 1, outValuesNeg + i + numMoved, outValuesNeg + i);
                        std::move(outIndicesNeg + i + 1, outIndicesNeg + i + numMoved, outIndicesNeg + i);
                        std::move(outTimesNeg + i + 1, outTimesNeg + i + numMoved, outTimesNeg + i);
                    }
                    len--;
                    removedNegCounter++;
                    i--;
                }
            }
        }
    }

    return new int[2]{removedCounter, removedNegCounter};
}
