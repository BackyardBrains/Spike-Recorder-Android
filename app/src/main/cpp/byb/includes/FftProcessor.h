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

            void setSampleRate(float sampleRate) override;

            void resetFft();

            void
            process(float **outData, int windowCount, int &windowCounter, int &frequencyCounter, int channelCount,
                    short **inSamples, const int *inSampleCount);

            void
            processSeek(float **outData, int windowCount, int &windowCounter, int &frequencyCounter, int channelCount,
                        short **inSamples, const int *inSampleCount);

        private:
            static const char *TAG;

            // Percentage of overlap between to consecutive FFT windows
            static constexpr uint8_t FFT_WINDOW_OVERLAP_PERCENT = 99;
            // We only look at approx. 30% of the fft data cause we only want to analyze low frequency
            static constexpr uint8_t FFT_30HZ_LENGTH = 32; // ~30%
            //
            static constexpr int FFT_WINDOW_TIME_LENGTH = 4; // 2^2
            //
            static constexpr float FFT_SAMPLE_RATE = 128; // 2^7
            //
            static constexpr uint32_t FFT_WINDOW_SAMPLE_COUNT = static_cast<const uint32_t>(FFT_WINDOW_TIME_LENGTH *
                                                                                            FFT_SAMPLE_RATE); // 2^9
            //
            static constexpr uint32_t FFT_WINDOW_30HZ_DATA_SIZE = static_cast<uint32_t>(FFT_30HZ_LENGTH /
                                                                                        (FFT_SAMPLE_RATE /
                                                                                         (float) FFT_WINDOW_SAMPLE_COUNT));

//            long long currentTimeInMilliseconds();

            void init(float d);

            void clean();

            audiofft::AudioFFT fft;

            std::vector<float> input;
            std::vector<float> outReal;
            std::vector<float> outImaginary;

            // Size of single window of samples - must be 2^N
            uint32_t oWindowSampleCount;
            // Number of samples needed to be collected before starting new sample window
            uint32_t oWindowSampleDiffCount;

            // Whether buffers and normalization needs to be reset before processing
            bool resetOnNextCycle = false;
            //
            bool lastSeeking = false;

            // Holds all unanalyzed samples left from the latest sample batch
            float *unanalyzedSamples;
            int32_t unanalyzedSampleCount;
            // Holds latest oWindowSampleCount number of samples
            float *sampleBuffer;

            // Holds indices for the downsampled samples so we don't have to calculate them with every new batch
            int *dsIndices;
            int dsIndexCount;


            float maxMagnitude = 4.83;
            float halfMaxMagnitude = maxMagnitude * .5f;
        };
    }
}

#endif //SPIKE_RECORDER_ANDROID_FFTPROCESSOR_H
