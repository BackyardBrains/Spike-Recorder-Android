//
// Created by  Tihomir Leka <tihomir at backyardbrains.com>
//

#include "Processor.h"

Processor::Processor(float sampleRate, int channelCount) {
    Processor::sampleRate = sampleRate;
    Processor::channelCount = channelCount;

    createFilters();

    initialized = true;
}

Processor::~Processor() {
}

float Processor::getSampleRate() {
    return sampleRate;
}

void Processor::setSampleRate(float sampleRate) {
    __android_log_print(ANDROID_LOG_DEBUG, typeid(*this).name(), "SAMPLE RATE: %1f", sampleRate);

    if (initialized) deleteFilters();
    Processor::sampleRate = sampleRate;

    createFilters();
}

int Processor::getChannelCount() {
    return channelCount;
}

void Processor::setChannelCount(int channelCount) {
    __android_log_print(ANDROID_LOG_DEBUG, typeid(*this).name(), "CHANNEL COUNT: %1d", channelCount);

    if (initialized) deleteFilters();
    Processor::channelCount = channelCount;

    createFilters();
}

void Processor::applyFilters(int channel, short *data, int numFrames) {
    if (lowPassFilteringEnabled) lowPassFilter[channel].filter(data, numFrames);
    if (highPassFilteringEnabled) highPassFilter[channel].filter(data, numFrames);
}

void Processor::setFilters(float lowCutOff, float highCutOff) {
    __android_log_print(ANDROID_LOG_DEBUG, typeid(*this).name(), "LOW: %1f, HIGH: %1f", lowCutOff, highCutOff);
    lowPassFilteringEnabled = highCutOff != -1 && highCutOff != MAX_FILTER_CUT_OFF;
    highPassFilteringEnabled = lowCutOff != -1 && lowCutOff != MIN_FILTER_CUT_OFF;

    Processor::lowCutOff = lowCutOff;
    Processor::highCutOff = highCutOff;

    if (initialized) deleteFilters();
    createFilters();
}

void Processor::createFilters() {
    lowPassFilter = new LowPassFilter[getChannelCount()];
    highPassFilter = new HighPassFilter[getChannelCount()];
    for (int i = 0; i < getChannelCount(); i++) {
        // low pass filters
        lowPassFilter[i].initWithSamplingRate(sampleRate);
        if (highCutOff > sampleRate / 2.0f) highCutOff = sampleRate / 2.0f;
        lowPassFilter[i].setCornerFrequency(highCutOff);
        lowPassFilter[i].setQ(0.5f);
        // high pass filters
        highPassFilter[i].initWithSamplingRate(sampleRate);
        if (lowCutOff > sampleRate / 2.0f) lowCutOff = sampleRate / 2.0f;
        highPassFilter[i].setCornerFrequency(lowCutOff);
        highPassFilter[i].setQ(0.5f);
    }
}

void Processor::deleteFilters() {
    delete[] lowPassFilter;
    delete[] highPassFilter;
}
