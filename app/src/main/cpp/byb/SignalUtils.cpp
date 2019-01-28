//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include "SignalUtils.h"

short **SignalUtils::deinterleaveSignal(short **outSamples, const short *inSamples, int sampleCount, int channelCount) {
    int frameCount = sampleCount / channelCount;
    for (int ch = 0; ch < channelCount; ch++) {
        for (int i = 0; i < frameCount; i++) {
            outSamples[ch][i] = inSamples[channelCount * i + ch];
        }
    }

    return outSamples;
}

short *SignalUtils::interleaveSignal(short **samples, int frameCount, int channelCount) {
    int sampleCount = frameCount * channelCount;
    auto *result = new short[sampleCount];
    for (int i = 0; i < frameCount; i++) {
        for (int ch = 0; ch < channelCount; ch++) {
            result[channelCount * i + ch] = samples[ch][i];
        }
    }

    return result;
}
