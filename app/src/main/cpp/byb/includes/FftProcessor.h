//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_FFTPROCESSOR_H
#define SPIKE_RECORDER_ANDROID_FFTPROCESSOR_H

#include <algorithm>
#include <cmath>
#include <vector>
#include <algorithm>

#include "AudioFFT.h"
#include "AnalysisUtils.h"
#include "Processor.h"

namespace backyardbrains {

    namespace processing {

        class FftProcessor : public Processor {
        public:

            FftProcessor();

            ~FftProcessor() override;

            void
            process(float **outData, uint32_t &windowCount, uint32_t &perWindowCount, short **inSamples,
                    uint32_t *inSampleCount);

        private:
            static const char *TAG;

            static constexpr uint8_t WINDOW_OVERLAP_PERCENT = 99;
            static constexpr uint8_t FFT_30HZ_LENGTH = 32;

            void init();

            audiofft::AudioFFT fft;

            std::vector<float> input;
            std::vector<float> outReal;
            std::vector<float> outImaginary;

            // Size of single window of samples - must be 2^N
            uint32_t sampleWindowSize;
            // Size of fft data that will be returned when providing sampleWindowSize of samples
            uint32_t fftDataSize;
            // Size of actual fft data that we take into account when creating output graph
            uint32_t thirtyHzDataSize;
            // Difference between two consecutive frequency values represented in the output graph
            float oneFrequencyStep;
            // Percentage of overlap between to consecutive sample windows
            uint8_t windowOverlapPercent = WINDOW_OVERLAP_PERCENT;
            // Number of samples needed to be collected before starting new sample window
            uint32_t windowSampleDiffCount;
            //
            // Holds all unanalyzed samples left from the latest sample batch
            float *unanalyzedSamples;
            int32_t unanalyzedSampleCount;
            // Holds latest sampleWindowSize number of samples
            float *sampleBuffer;

            float maxMagnitude;
            float halfMaxMagnitude;
            float maxMagnitudeOptimized;
            float halfMaxMagnitudeOptimized;
//            long long currentTimeInMilliseconds();
        };
    }
}

#endif //SPIKE_RECORDER_ANDROID_FFTPROCESSOR_H
