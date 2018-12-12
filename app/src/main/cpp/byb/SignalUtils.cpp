//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include "SignalUtils.h"

short **SignalUtils::deinterleaveSignal(const short *samples, int sampleCount, int channelCount) {
    int frameCount = sampleCount / channelCount;
    short **result = new short *[channelCount];
    for (int ch = 0; ch < channelCount; ch++) {
        result[ch] = new short[frameCount];
        for (int i = 0; i < frameCount; i++) {
            result[ch][i] = samples[channelCount * i + ch];
        }
    }

    return result;
}

short *SignalUtils::interleaveSignal(short **samples, int frameCount, int channelCount) {
    int sampleCount = frameCount * channelCount;
    short *result = new short[sampleCount];
    for (int i = 0; i < frameCount; i++) {
        for (int ch = 0; ch < channelCount; ch++) {
            result[channelCount * i + ch] = samples[ch][i];
        }
    }

    return result;
}
