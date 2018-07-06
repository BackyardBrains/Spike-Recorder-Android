//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_PROCESSOR_H
#define SPIKE_RECORDER_ANDROID_PROCESSOR_H

#include <android/log.h>
#include "LowPassFilter.h"
#include "HighPassFilter.h"

namespace processing {
    class Processor;
}

class Processor {
public:
    Processor();

    virtual ~Processor();

    virtual void setSampleRate(float sampleRate);

    void setFilters(float lowCutOff, float highCutOff);

protected:
    float getSampleRate();

    void applyFilters(short *data, int numFrames);

private:
    const static char *TAG;

    // Minimum cut-off frequency
    static constexpr float MIN_FILTER_CUT_OFF = 0.0f;
    // Maximum cut-off frequency
    static constexpr float MAX_FILTER_CUT_OFF = 5000.0f;

    // Current sample rate
    float sampleRate;
    // Current filters
    bool lowPassFilteringEnabled = false;
    LowPassFilter lowPassFilter;
    bool highPassFilteringEnabled = false;
    HighPassFilter highPassFilter;
};


#endif //SPIKE_RECORDER_ANDROID_PROCESSOR_H
