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
            static constexpr float MIN_VALID_FILE_LENGTH_IN_SECS = 0.2f;
            static constexpr float BIN_COUNT = 200.0f;
            static constexpr float BUFFER_SIZE_IN_SECS = 12.0f;

            static float mean(float *data, int length);

            static float SD(short *data, int length);

            static float SD(float *data, int length);

            static float RMS(short *data, int length);

            /**
             * Generates a logarithmic scale of {@code size} values ranging between {@code min} and {@code max}.
             */
            static float *generateLogSpace(int min, int max, int size);

            static float map(float value, float inMin, float inMax, float outMin, float outMax);

            static void map(float *in, float *out, int length, float inMin, float inMax, float outMin, float outMax);

            static void minMax(float *data, int length, float &min, float &max);
        };
    }
}

#endif //SPIKE_RECORDER_ANDROID_ANALYSISUTILS_H
