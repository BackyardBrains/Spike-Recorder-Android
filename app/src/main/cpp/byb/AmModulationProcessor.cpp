//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include "AmModulationProcessor.h"

const char *AmModulationProcessor::TAG = "AmModulationProcessor";

AmModulationProcessor::AmModulationProcessor() {
    setSampleRate(SAMPLE_RATE);
}

AmModulationProcessor::~AmModulationProcessor() {
}

void AmModulationProcessor::init() {
    // setup AM detection low pass filter
    amDetectionLowPassFilter.initWithSamplingRate(getSampleRate());
    amDetectionLowPassFilter.setCornerFrequency(AM_DETECTION_CUTOFF);
    amDetectionLowPassFilter.setQ(0.5f);
    // setup AM detection notch filter
    amDetectionNotchFilter.initWithSamplingRate(SAMPLE_RATE);
    amDetectionNotchFilter.setCenterFrequency(AM_CARRIER_FREQUENCY);
    amDetectionNotchFilter.setQ(1.0f);

    // setup AM demodulation low pass filter
    for (int i = 0; i < AM_DEMODULATION_LOW_PASS_FILTER_COUNT; i++) {
        amDemodulationLowPassFilter[i].initWithSamplingRate(getSampleRate());
        amDemodulationLowPassFilter[i].setCornerFrequency(AM_DEMODULATION_CUTOFF);
        amDemodulationLowPassFilter[i].setQ(1.0f);

    }
}

void AmModulationProcessor::setSampleRate(float sampleRate) {
    Processor::setSampleRate(sampleRate);

    init();
}

bool AmModulationProcessor::isReceivingAmSignal() {
    return receivingAmSignal;
}

void AmModulationProcessor::process(const short *inSamples, short *outSamples, const int length) {
    short *amBuffer = new short[length];
    std::copy(inSamples, inSamples + length, amBuffer);

    amDetectionLowPassFilter.filter(amBuffer, length);
    for (int i = 0; i < length; i++) {
        rmsOfOriginalSignal = static_cast<float>(0.0001f * pow(amBuffer[i], 2.0f) + 0.9999f * rmsOfOriginalSignal);
    }
    amDetectionNotchFilter.filter(amBuffer, length);
    for (int i = 0; i < length; i++) {
        rmsOfNotchedAMSignal = static_cast<float>(0.0001f * pow(amBuffer[i], 2.0f) + 0.9999f * rmsOfNotchedAMSignal);
    }

    delete[] amBuffer;

    if (sqrtf(rmsOfOriginalSignal) / sqrtf(rmsOfNotchedAMSignal) > 5) {
        if (!receivingAmSignal) receivingAmSignal = true;

        for (int i = 0; i < length; i++) {
            outSamples[i] = static_cast<short>(abs(inSamples[i]));
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

        // apply additional filtering if necessary
        applyFilters(outSamples, length);

        return;
    }

    if (receivingAmSignal) receivingAmSignal = false;

}
