//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include "SignalUtils.h"

short **SignalUtils::deinterleaveSignal(const short *samples, int length, int channelCount) {
    int frameCount = length / channelCount;
    short **result = new short *[channelCount];
    for (int ch = 0; ch < channelCount; ch++) {
        result[ch] = new short[frameCount];
        for (int i = 0; i < frameCount; i++) {
            result[ch][i] = samples[channelCount * i + ch];
        }
    }

    return result;
}
