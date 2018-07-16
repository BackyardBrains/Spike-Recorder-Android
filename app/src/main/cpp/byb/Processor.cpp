//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include "Processor.h"

const char *Processor::TAG = "Processor";

Processor::Processor() {
}

Processor::~Processor() {
}

float Processor::getSampleRate() {
    return sampleRate;
}

void Processor::applyFilters(short *data, int numFrames) {
    if (lowPassFilteringEnabled) lowPassFilter.filter(data, numFrames);
    if (highPassFilteringEnabled) lowPassFilter.filter(data, numFrames);

}

void Processor::setSampleRate(float sampleRate) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "SAMPLE RATE: %1f", sampleRate);
    Processor::sampleRate = sampleRate;
}

void Processor::setFilters(float lowCutOff, float highCutOff) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "LOW: %1f, HIGH: %1f", lowCutOff, highCutOff);
    lowPassFilteringEnabled = highCutOff != -1 && highCutOff != MAX_FILTER_CUT_OFF;
    if (lowPassFilteringEnabled) {
        lowPassFilter.initWithSamplingRate(sampleRate);
        if (highCutOff > sampleRate / 2.0f) highCutOff = sampleRate / 2.0f;
        lowPassFilter.setCornerFrequency(highCutOff);
        lowPassFilter.setQ(0.5f);
    }
    highPassFilteringEnabled = lowCutOff != -1 && lowCutOff != MIN_FILTER_CUT_OFF;
    if (highPassFilteringEnabled) {
        highPassFilter.initWithSamplingRate(sampleRate);
        if (lowCutOff > sampleRate / 2.0f) lowCutOff = sampleRate / 2.0f;
        highPassFilter.setCornerFrequency(lowCutOff);
        highPassFilter.setQ(0.5f);
    }
}