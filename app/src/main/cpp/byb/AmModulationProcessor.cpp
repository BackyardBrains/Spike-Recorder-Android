//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include "AmModulationProcessor.h"

namespace backyardbrains {

    namespace processing {

        const char *AmModulationProcessor::TAG = "AmModulationProcessor";

        AmModulationProcessor::AmModulationProcessor() {
            createDemodulationFilters();

            initialized = true;
        }

        AmModulationProcessor::~AmModulationProcessor() {
            if (initialized) deleteDemodulationFilters();
        }

        void AmModulationProcessor::createDemodulationFilters() {
            // setup AM detection low pass filter
            amDetectionLowPassFilter.initWithSamplingRate(getSampleRate());
            amDetectionLowPassFilter.setCornerFrequency(AM_DETECTION_CUTOFF);
            amDetectionLowPassFilter.setQ(0.5f);
            // setup AM detection notch filter
            amDetectionNotchFilter.initWithSamplingRate(getSampleRate());
            amDetectionNotchFilter.setCenterFrequency(AM_CARRIER_FREQUENCY);
            amDetectionNotchFilter.setQ(1.0f);

            // setup AM demodulation low pass filter
            amDemodulationLowPassFilter = new LowPassFilter *[getChannelCount()];
            for (int i = 0; i < getChannelCount(); i++) {
                amDemodulationLowPassFilter[i] = new LowPassFilter[AM_DEMODULATION_LOW_PASS_FILTER_COUNT];
                for (int j = 0; j < AM_DEMODULATION_LOW_PASS_FILTER_COUNT; j++) {
                    amDemodulationLowPassFilter[i][j].initWithSamplingRate(getSampleRate());
                    amDemodulationLowPassFilter[i][j].setCornerFrequency(AM_DEMODULATION_CUTOFF);
                    amDemodulationLowPassFilter[i][j].setQ(1.0f);

                }
            }
        }

        void AmModulationProcessor::deleteDemodulationFilters() {
            // delete AM demodulation low pass filters
            for (int i = 0; i < getChannelCount(); i++) {
                delete[] amDemodulationLowPassFilter[i];
            }
            delete[] amDemodulationLowPassFilter;
        }

        void AmModulationProcessor::setSampleRate(float sampleRate) {
            // delete AM demodulation low pass filter only on initialization
            if (initialized) deleteDemodulationFilters();
            Processor::setSampleRate(sampleRate);

            createDemodulationFilters();
        }

        void AmModulationProcessor::setChannelCount(int channelCount) {
            // delete AM demodulation low pass filter only on initialization
            if (initialized) deleteDemodulationFilters();
            Processor::setChannelCount(channelCount);

            createDemodulationFilters();
        }

        bool AmModulationProcessor::isReceivingAmSignal() {
            return receivingAmSignal;
        }

        void
        AmModulationProcessor::process(const short *inSamples, short **outSamples, const int sampleCount,
                                       const int frameCount) {
            auto channelCount = getChannelCount();
            auto **deinterleavedSignal = new short *[channelCount];
            for (int i = 0; i < channelCount; i++) {
                deinterleavedSignal[i] = new short[frameCount];
            }
            backyardbrains::utils::SignalUtils::deinterleaveSignal(deinterleavedSignal, inSamples, sampleCount,
                                                                   channelCount);

            auto *amBuffer = new short[frameCount];
            // always use only first channel for detection
            std::copy(deinterleavedSignal[0], deinterleavedSignal[0] + frameCount, amBuffer);

            amDetectionLowPassFilter.filter(amBuffer, frameCount);
            for (int i = 0; i < frameCount; i++) {
                rmsOfOriginalSignal = static_cast<float>(0.0001f * pow(amBuffer[i], 2.0f) +
                                                         0.9999f * rmsOfOriginalSignal);
            }
            amDetectionNotchFilter.filter(amBuffer, frameCount);
            for (int i = 0; i < frameCount; i++) {
                rmsOfNotchedAMSignal = static_cast<float>(0.0001f * pow(amBuffer[i], 2.0f) +
                                                          0.9999f * rmsOfNotchedAMSignal);
            }

            delete[] amBuffer;

            if (sqrtf(rmsOfOriginalSignal) / sqrtf(rmsOfNotchedAMSignal) > 5) {
                if (!receivingAmSignal) receivingAmSignal = true;

                for (int i = 0; i < channelCount; i++) {
                    for (int j = 0; j < frameCount; j++) {
                        outSamples[i][j] = static_cast<short>(abs(deinterleavedSignal[i][j]));
                    }
                    for (int j = 0; j < AM_DEMODULATION_LOW_PASS_FILTER_COUNT; j++) {
                        amDemodulationLowPassFilter[i][j].filter(outSamples[i], frameCount);
                    }
                    for (int j = 0; j < frameCount; j++) {
                        // calculate average sample
                        average = 0.00001f * outSamples[i][j] + 0.99999f * average;
                        // use average to remove offset
                        outSamples[i][j] = static_cast<short>(outSamples[i][j] - average);
                    }

                    // apply additional filtering if necessary
                    applyFilters(i, outSamples[i], frameCount);
                }

                // free memory
                for (int i = 0; i < channelCount; i++) {
                    delete[] deinterleavedSignal[i];
                }
                delete[] deinterleavedSignal;

                return;
            } else {
                for (int i = 0; i < channelCount; i++) {
                    std::copy(deinterleavedSignal[i], deinterleavedSignal[i] + frameCount, outSamples[i]);

                    // apply additional filtering if necessary
                    applyFilters(i, outSamples[i], frameCount);
                }

                // free memory
                for (int i = 0; i < channelCount; i++) {
                    delete[] deinterleavedSignal[i];
                }
                delete[] deinterleavedSignal;
            }

            if (receivingAmSignal) receivingAmSignal = false;
        }
    }
}