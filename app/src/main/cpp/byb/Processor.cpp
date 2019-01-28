//
// Created by  Tihomir Leka <tihomir at backyardbrains.com>
//

#include "Processor.h"

Processor::Processor(float sampleRate, int channelCount) {
    Processor::sampleRate = sampleRate;
    Processor::channelCount = channelCount;

    createFilters(channelCount);

    initialized = true;
}

Processor::~Processor() = default;

float Processor::getSampleRate() {
    return sampleRate;
}

void Processor::setSampleRate(float sampleRate) {
    __android_log_print(ANDROID_LOG_DEBUG, typeid(*this).name(), "SAMPLE RATE: %1f", sampleRate);

    if (initialized) deleteFilters(channelCount);
    Processor::sampleRate = sampleRate;

    createFilters(channelCount);
}

int Processor::getChannelCount() {
    return channelCount;
}

void Processor::setChannelCount(int channelCount) {
    __android_log_print(ANDROID_LOG_DEBUG, typeid(*this).name(), "CHANNEL COUNT: %1d", channelCount);

    if (initialized) deleteFilters(Processor::channelCount);
    Processor::channelCount = channelCount;

    createFilters(channelCount);
}

void Processor::setSampleRateAndChannelCount(float sampleRate, int channelCount) {
    __android_log_print(ANDROID_LOG_DEBUG, typeid(*this).name(), "SAMPLE RATE: %1f, CHANNEL COUNT: %1d", sampleRate,
                        channelCount);

    if (initialized) deleteFilters(Processor::channelCount);
    Processor::channelCount = channelCount;

    createFilters(channelCount);
}

void Processor::applyFilters(int channel, short *data, int sampleCount) {
    if (lowPassFilteringEnabled) lowPassFilter[channel]->filter(data, sampleCount);
    if (highPassFilteringEnabled) highPassFilter[channel]->filter(data, sampleCount);

}

void Processor::setFilters(float lowCutOff, float highCutOff) {
    __android_log_print(ANDROID_LOG_DEBUG, typeid(*this).name(), "LOW: %1f, HIGH: %1f", lowCutOff, highCutOff);
    lowPassFilteringEnabled = highCutOff != -1 && highCutOff != MAX_FILTER_CUT_OFF;
    highPassFilteringEnabled = lowCutOff != -1 && lowCutOff != MIN_FILTER_CUT_OFF;

    Processor::lowCutOff = lowCutOff;
    Processor::highCutOff = highCutOff;

    if (initialized) deleteFilters(channelCount);
    createFilters(channelCount);
}

void Processor::createFilters(int channelCount) {
    lowPassFilter = new LowPassFilterPtr[channelCount];
    highPassFilter = new HighPassFilterPtr[channelCount];
    for (int i = 0; i < channelCount; i++) {
        // low pass filters
        lowPassFilter[i] = new LowPassFilter();
        lowPassFilter[i]->initWithSamplingRate(sampleRate);
        if (highCutOff > sampleRate / 2.0f) highCutOff = sampleRate / 2.0f;
        lowPassFilter[i]->setCornerFrequency(highCutOff);
        lowPassFilter[i]->setQ(0.5f);
        // high pass filters
        highPassFilter[i] = new HighPassFilter();
        highPassFilter[i]->initWithSamplingRate(sampleRate);
        if (lowCutOff < 0) lowCutOff = 0;
        highPassFilter[i]->setCornerFrequency(lowCutOff);
        highPassFilter[i]->setQ(0.5f);
    }
}

void Processor::deleteFilters(int channelCount) {
    for (int i = 0; i < channelCount; i++) {
        delete lowPassFilter[i];
        delete highPassFilter[i];
    }
    delete[] lowPassFilter;
    delete[] highPassFilter;
}
