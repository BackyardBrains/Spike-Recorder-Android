//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include "AmModulationProcessor.h"

const char *AmModulationProcessor::TAG = "AmModulationProcessor";

AmModulationProcessor::AmModulationProcessor() {
    init();
}

AmModulationProcessor::~AmModulationProcessor() {
}

void AmModulationProcessor::init() {
    // setup AM detection low pass filter
    amDetectionLowPassFilter.initWithSamplingRate(sampleRate);
    amDetectionLowPassFilter.setCornerFrequency(AM_DETECTION_CUTOFF);
    amDetectionLowPassFilter.setQ(0.5f);
    // setup AM detection notch filter
    amDetectionNotchFilter.initWithSamplingRate(SAMPLE_RATE);
    amDetectionNotchFilter.setCenterFrequency(AM_CARRIER_FREQUENCY);
    amDetectionNotchFilter.setQ(1.0f);

    // setup AM demodulation low pass filter
    for (int i = 0; i < AM_DEMODULATION_LOW_PASS_FILTER_COUNT; i++) {
        amDemodulationLowPassFilter[i].initWithSamplingRate(sampleRate);
        amDemodulationLowPassFilter[i].setCornerFrequency(AM_DEMODULATION_CUTOFF);
        amDemodulationLowPassFilter[i].setQ(1.0f);

    }
}

void AmModulationProcessor::setSampleRate(int sampleRate) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "SAMPLE RATE: %d", sampleRate);
    AmModulationProcessor::sampleRate = sampleRate;

    init();
}

void AmModulationProcessor::setFilters(float lowCutOff, float highCutOff) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "LOW: %1f, HIGH: %1f", lowCutOff, highCutOff);
    lowPassFilteringEnabled = highCutOff != -1 && highCutOff != MAX_FILTER_CUTOFF;
    if (lowPassFilteringEnabled) {
        lowPassFilter.initWithSamplingRate(sampleRate);
        if (highCutOff > sampleRate / 2.0f) highCutOff = sampleRate / 2.0f;
        lowPassFilter.setCornerFrequency(highCutOff);
        lowPassFilter.setQ(0.5f);

    }
    highPassFilterEnabled = lowCutOff != -1 && lowCutOff != MIN_FILTER_CUTOFF;
    if (highPassFilterEnabled) {
        highPassFilter.initWithSamplingRate(sampleRate);
        if (lowCutOff > sampleRate / 2.0f) lowCutOff = sampleRate / 2.0f;
        highPassFilter.setCornerFrequency(lowCutOff);
        highPassFilter.setQ(0.5f);
    }
}

bool AmModulationProcessor::isReceivingAmSignal() {
    return receivingAmSignal;
}

void AmModulationProcessor::process(const short *inSamples, short *outSamples, const int length) {
    amBuffer = new short[length];
    std::copy(inSamples, inSamples + length, amBuffer);

    amDetectionLowPassFilter.filter(amBuffer, length);
    for (int i = 0; i < length; i++) {
        rmsOfOriginalSignal = 0.0001f * ((float) (amBuffer[i] * amBuffer[i])) + 0.9999f * rmsOfOriginalSignal;
    }
    amDetectionNotchFilter.filter(amBuffer, length);
    for (int32_t i = 0; i < length; i++) {
        rmsOfNotchedAMSignal = 0.0001f * ((float) (amBuffer[i] * amBuffer[i])) + 0.9999f * rmsOfNotchedAMSignal;
    }

    delete[] amBuffer;

    if (sqrtf(rmsOfOriginalSignal) / sqrtf(rmsOfNotchedAMSignal) > 5) {
        if (!receivingAmSignal) receivingAmSignal = true;


        std::copy(inSamples, inSamples + length, outSamples);

        for (int i = 0; i < length; i++) {
            outSamples[i] = static_cast<short>(abs(outSamples[i]));
        }
        for (int i = 0; i < AM_DEMODULATION_LOW_PASS_FILTER_COUNT; i++) {
            amDemodulationLowPassFilter[i].filter(outSamples, length);
        }
        for (int i = 0; i < length; i++) {
            // calculate average sample
            average = 0.00001f * outSamples[i] + 0.99999f * average;
            // use average to remove offset
            outSamples[i] = static_cast<short>(outSamples[i] - average);
        }

        if (lowPassFilteringEnabled) {
            lowPassFilter.filter(outSamples, length);
        }
        if (highPassFilterEnabled) {
            lowPassFilter.filter(outSamples, length);
        }

        return;
    }

    if (receivingAmSignal) receivingAmSignal = false;

}
