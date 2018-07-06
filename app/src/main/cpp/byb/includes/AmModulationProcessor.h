//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_AMMODULATIONPROCESSOR_H
#define SPIKE_RECORDER_ANDROID_AMMODULATIONPROCESSOR_H

#include "Processor.h"
#include "NotchFilter.h"
#include "LowPassFilter.h"
#include "HighPassFilter.h"
#include <math.h>
#include <algorithm>
#include <string>
#include <android/log.h>

namespace processing {
    class AmModulationProcessor;
}


class AmModulationProcessor : public Processor {
public:
    AmModulationProcessor();

    ~AmModulationProcessor();

    void setSampleRate(float sampleRate);

    bool isReceivingAmSignal();

    void process(const short *inSamples, short *outSamples, const int length);

private:
    static const char *TAG;

    static constexpr float SAMPLE_RATE = 44100.0f;
    static constexpr float AM_CARRIER_FREQUENCY = 5000.0f;
    static constexpr float AM_DETECTION_CUTOFF = 6000.0f;
    static constexpr int AM_DEMODULATION_LOW_PASS_FILTER_COUNT = 3;
    static constexpr float AM_DEMODULATION_CUTOFF = 500.0f;

    void init();

    // Used for detection of the AM modulation
    LowPassFilter amDetectionLowPassFilter;
    // Used for detection of the AM modulation
    NotchFilter amDetectionNotchFilter;
    // Used for signal demodulation
    LowPassFilter amDemodulationLowPassFilter[AM_DEMODULATION_LOW_PASS_FILTER_COUNT];

    // Temporary buffer
    short *amBuffer;
    // Used to detect whether signal is modulated or not
    float rmsOfOriginalSignal = 0;
    float rmsOfNotchedAMSignal = 0;
    // Average signal which we use to avoid signal offset
    float average;
    // Whether we are currently receiving modulated signal
    bool receivingAmSignal = false;
};


#endif //SPIKE_RECORDER_ANDROID_AMMODULATIONPROCESSOR_H
