//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include "AnalysisUtils.h"

namespace backyardbrains {

    namespace utils {

        float AnalysisUtils::SD(short *data, int length) {
            float mean = 0.0f;
            float squares = 0.0f;
            float number;
            for (int i = 0; i < length; i++) {
                number = data[i];
                mean += number;
                squares += number * number;
            }
            mean /= length;
            squares /= length;
            return sqrt(squares - mean * mean);
        }

        float AnalysisUtils::RMS(short *data, int length) {
            float squares = 0.0f;
            float number;
            for (int i = 0; i < length; i++) {
                number = data[i];
                squares += number * number;
            }
            return sqrt(squares / length);
        }

        float *AnalysisUtils::generateLogSpace(int min, int max, int size) {
            double logarithmicBase = M_E;
            double minimums = pow(10.0f, min);
            double maximums = pow(10.0f, max);
            double logMin = log(minimums);
            double logMax = log(maximums);
            double delta = (logMax - logMin) / size;

            double accDelta = 0;
            auto *logSpace = new float[size + 1];
            for (int i = 0; i <= size; ++i) {
                logSpace[i] = static_cast<float>(pow(logarithmicBase, logMin + accDelta));
                accDelta += delta;
            }
            return logSpace;
        }

        float AnalysisUtils::map(float value, float inMin, float inMax, float outMin, float outMax) {
            return (value - inMin) * (outMax - outMin) / (inMax - inMin) + outMin;
        }
    }
}