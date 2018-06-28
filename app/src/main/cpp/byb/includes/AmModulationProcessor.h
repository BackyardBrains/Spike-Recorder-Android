//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_AMMODULATIONPROCESSOR_H
#define SPIKE_RECORDER_ANDROID_AMMODULATIONPROCESSOR_H


#include "NotchFilter.h"
#include "LowPassFilter.h"
#include "HighPassFilter.h"
#include <math.h>
#include <string>
#include <android/log.h>

#define SAMPLE_RATE 44100.0f
#define AM_CARRIER_FREQUENCY 5000.0f
#define AM_DETECTION_CUTOFF  6000.0f
#define AM_DEMODULATION_LOW_PASS_FILTER_COUNT 3
#define AM_DEMODULATION_CUTOFF 500.0f
// Minimum cut-off frequency
#define MIN_FILTER_CUTOFF 0.0f
// Maximum cut-off frequency
#define MAX_FILTER_CUTOFF 5000.0f

class AmModulationProcessor {
public:
    AmModulationProcessor();

    ~AmModulationProcessor();

    void setSampleRate(int sampleRate);

    void setFilters(float lowCutOff, float highCutOff);

    bool isReceivingAmSignal();

    void process(const short *inSamples, short *outSamples, const int length);

private:
    static const char *TAG;

    void init();

    LowPassFilter lowPassFilter;
    HighPassFilter highPassFilter;
    LowPassFilter amDetectionLowPassFilter;
    NotchFilter amDetectionNotchFilter;
    LowPassFilter amDemodulationLowPassFilter[AM_DEMODULATION_LOW_PASS_FILTER_COUNT];

    // Current sample rate
    float sampleRate = SAMPLE_RATE;
    // Temporary buffer
    short *amBuffer;
    // Used to detect whether signal is modulated or not
    float rmsOfOriginalSignal = 0;
    float rmsOfNotchedAMSignal = 0;
    // Average signal which we use to avoid signal offset
    float average;
    // Whether we are currently receiving modulated signal
    bool receivingAmSignal = false;
    // Whether additional low pass filter should be used
    bool lowPassFilteringEnabled = false;
    // Whether additional high pass filter should be used
    bool highPassFilterEnabled = false;
};


#endif //SPIKE_RECORDER_ANDROID_AMMODULATIONPROCESSOR_H
