//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_PROCESSOR_H
#define SPIKE_RECORDER_ANDROID_PROCESSOR_H

#include <typeinfo>
#include <android/log.h>
#include "LowPassFilter.h"
#include "HighPassFilter.h"

class Processor {
public:
    Processor(float sampleRate, int channelCount);

    Processor() : Processor(DEFAULT_SAMPLE_RATE, DEFAULT_CHANNEL_COUNT) {}

    virtual ~Processor();

    float getSampleRate();

    virtual void setSampleRate(float sampleRate);

    int getChannelCount();

    virtual void setChannelCount(int channelCount);

    void setFilters(float lowCutOff, float highCutOff);

protected:
    void applyFilters(int channel, short *data, int numFrames);

private:
    // Minimum cut-off frequency
    static constexpr float MIN_FILTER_CUT_OFF = 0.0f;
    // Maximum cut-off frequency
    static constexpr float MAX_FILTER_CUT_OFF = 5000.0f;
    // Default sample rate we start with
    static constexpr float DEFAULT_SAMPLE_RATE = 44100.0f;
    // Default channel count
    static constexpr int DEFAULT_CHANNEL_COUNT = 1;

    void createFilters();

    void deleteFilters();

    // Flag that is set to true after initialization
    bool initialized = false;
    // Current sample rate
    float sampleRate = DEFAULT_SAMPLE_RATE;
    // Current channel count
    int channelCount = DEFAULT_CHANNEL_COUNT;
    // Current low pass filter high cutoff frequency
    float highCutOff = MAX_FILTER_CUT_OFF;
    // Whether low pass filters should be applied
    bool lowPassFilteringEnabled = false;
    // Low pass filters for all channels
    LowPassFilter *lowPassFilter;
    // Current high pass filter low cutoff frequency
    float lowCutOff = MIN_FILTER_CUT_OFF;
    // Whether high pass filters should be applied
    bool highPassFilteringEnabled = false;
    // High pass filters for all channels
    HighPassFilter *highPassFilter;
};


#endif //SPIKE_RECORDER_ANDROID_PROCESSOR_H
