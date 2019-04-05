//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_ANALYSISUTILS_H
#define SPIKE_RECORDER_ANDROID_ANALYSISUTILS_H

#include <cmath>

namespace backyardbrains {

    namespace utils {

        class AnalysisUtils {
        public:
            static float SD(short *data, int length);

            static float RMS(short *data, int length);

            /**
             * Generates a logarithmic scale of {@code size} values ranging between {@code min} and {@code max}.
             */
            static float *generateLogSpace(int min, int max, int size);

            static float map(float value, float inMin, float inMax, float outMin, float outMax);
        };
    }
}

#endif //SPIKE_RECORDER_ANDROID_ANALYSISUTILS_H
