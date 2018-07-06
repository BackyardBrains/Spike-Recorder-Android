//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_ANALYSISUTILS_H
#define SPIKE_RECORDER_ANDROID_ANALYSISUTILS_H

#include <math.h>

namespace util {
    class AnalysisUtils;
}

class AnalysisUtils {
public:
    static float SD(short *data, int length);

    static float RMS(short *data, int length);
};

#endif //SPIKE_RECORDER_ANDROID_ANALYSISUTILS_H
