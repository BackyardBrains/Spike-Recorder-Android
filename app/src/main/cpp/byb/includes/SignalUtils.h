//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_SIGNALUTILS_H
#define SPIKE_RECORDER_ANDROID_SIGNALUTILS_H

#include <limits.h>

namespace backyardbrains {

    namespace utils {

        class SignalUtils {
        public:
            static constexpr float ENCODING_16BIT = 16;
            static constexpr float ENCODING_FLOAT = 32;

            static void
            deinterleaveSignal(short **outSamples, const short *inSamples, int sampleCount, int channelCount);

            static void
            deinterleaveSignal1(short **outSamples, const float *inSamples, int sampleCount, int channelCount);

            static short *interleaveSignal(short **samples, int frameCount, int channelCount);
        };
    }
}

#endif //SPIKE_RECORDER_ANDROID_SIGNALUTILS_H
