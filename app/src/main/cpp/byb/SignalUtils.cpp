//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include "SignalUtils.h"

namespace backyardbrains {

    namespace utils {

        void
        SignalUtils::deinterleaveSignal(short **outSamples, const short *inSamples, int sampleCount, int channelCount) {
            int frameCount = sampleCount / channelCount;
            for (int ch = 0; ch < channelCount; ch++) {
                for (int i = 0; i < frameCount; i++) {
                    outSamples[ch][i] = inSamples[channelCount * i + ch];
                }
            }
        }

        void
        SignalUtils::deinterleaveSignal1(short **outSamples, const float *inSamples, int sampleCount,
                                         int channelCount) {
            int frameCount = sampleCount / channelCount;
            for (int ch = 0; ch < channelCount; ch++) {
                for (int i = 0; i < frameCount; i++) {
                    outSamples[ch][i] = static_cast<short>(inSamples[channelCount * i + ch] * SHRT_MAX);
                }
            }
        }

        void SignalUtils::interleaveSignal(short *outSamples, short **inSamples, int frameCount, int channelCount) {
            for (int i = 0; i < frameCount; i++) {
                for (int ch = 0; ch < channelCount; ch++) {
                    outSamples[channelCount * i + ch] = inSamples[ch][i];
                }
            }
        }

        void SignalUtils::normalizeSignalToFloat(float *outSamples, short *inSamples, int sampleCount) {
            auto max = (float) SHRT_MAX;
            for (int i = 0; i < sampleCount; i++) {
                outSamples[i] = inSamples[i] / max;
            }
        }
    }
}