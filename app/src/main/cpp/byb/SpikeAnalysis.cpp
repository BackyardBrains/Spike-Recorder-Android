//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include "SpikeAnalysis.h"

const char *SpikeAnalysis::TAG = "SpikeAnalysis";

SpikeAnalysis::SpikeAnalysis() {
}

SpikeAnalysis::~SpikeAnalysis() {
}

int *
SpikeAnalysis::findSpikes(const short *samples, int sampleCount, float sampleRate, short *valuesPos, int *indicesPos,
                          float *timesPos, int startIndexPos, int acceptablePos, short *valuesNeg, int *indicesNeg,
                          float *timesNeg, int startIndexNeg, int acceptableNeg) {
    float sampleRateDivider = (float) 1 / sampleRate;
    int peaksCounterPos = 0;
    int peaksCounterNeg = 0;
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < sampleCount; i++) {
        sample = samples[i];
        // determine state of positive schmitt trigger
        if (schmittPosState == SCHMITT_OFF) {
            if (sample > acceptablePos) {
                schmittPosState = SCHMITT_ON;
                maxPeakValue = SHRT_MIN;
            }
        } else {
            if (sample < 0) {
                schmittPosState = SCHMITT_OFF;
                valuesPos[startIndexPos + peaksCounterPos] = maxPeakValue;
                indicesPos[startIndexPos + peaksCounterPos] = maxPeakIndex;
                timesPos[startIndexPos + peaksCounterPos] = maxPeakTime;
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
                valuesNeg[startIndexNeg + peaksCounterNeg] = minPeakValue;
                indicesNeg[startIndexNeg + peaksCounterNeg] = minPeakIndex;
                timesNeg[startIndexNeg + peaksCounterNeg] = minPeakTime;
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

int *
SpikeAnalysis::filterSpikes(short *valuesPos, int *indicesPos, float *timesPos, int positivesCount, short *valuesNeg,
                            int *indicesNeg, float *timesNeg, int negativesCount) {
    int len = positivesCount;
    int removedCounter = 0;
    int removedNegCounter = 0;
    int i;
    if (len > 0) { // Filter positive spikes using kill interval
        for (i = 0; i < len - 1; i++) { // look on the right
            if (valuesPos[i] < valuesPos[i + 1]) {
                if ((timesPos[i + 1] - timesPos[i]) < KILL_INTERVAL) {
                    int numMoved = len - i - 1;
                    if (numMoved > 0) {
                        std::move(valuesPos + i + 1, valuesPos + i + numMoved, valuesPos + i);
                        std::move(indicesPos + i + 1, indicesPos + i + numMoved, indicesPos + i);
                        std::move(timesPos + i + 1, timesPos + i + numMoved, timesPos + i);
                    }
                    len--;
                    removedCounter++;
                    i--;
                }
            }
        }
        len = i;
        for (i = 1; i < len; i++) { // look on the left neighbor
            if (valuesPos[i] < valuesPos[i - 1]) {
                if ((timesPos[i] - timesPos[i - 1]) < KILL_INTERVAL) {
                    int numMoved = len - i - 1;
                    if (numMoved > 0) {
                        std::move(valuesPos + i + 1, valuesPos + i + numMoved, valuesPos + i);
                        std::move(indicesPos + i + 1, indicesPos + i + numMoved, indicesPos + i);
                        std::move(timesPos + i + 1, timesPos + i + numMoved, timesPos + i);
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
            if (valuesNeg[i] > valuesNeg[i + 1]) {
                if ((timesNeg[i + 1] - timesNeg[i]) < KILL_INTERVAL) {
                    int numMoved = len - i - 1;
                    if (numMoved > 0) {
                        std::move(valuesNeg + i + 1, valuesNeg + i + numMoved, valuesNeg + i);
                        std::move(indicesNeg + i + 1, indicesNeg + i + numMoved, indicesNeg + i);
                        std::move(timesNeg + i + 1, timesNeg + i + numMoved, timesNeg + i);
                    }
                    len--;
                    removedNegCounter++;
                    i--;
                }
            }
        }
        len = i;
        for (i = 1; i < len; i++) { // look on the left neighbor
            if (valuesNeg[i] > valuesNeg[i - 1]) {
                if ((timesNeg[i] - timesNeg[i - 1]) < KILL_INTERVAL) {
                    int numMoved = len - i - 1;
                    if (numMoved > 0) {
                        std::move(valuesNeg + i + 1, valuesNeg + i + numMoved, valuesNeg + i);
                        std::move(indicesNeg + i + 1, indicesNeg + i + numMoved, indicesNeg + i);
                        std::move(timesNeg + i + 1, timesNeg + i + numMoved, timesNeg + i);
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
