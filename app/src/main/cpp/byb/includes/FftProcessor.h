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

        private:
            static const char *TAG;

            //
            static constexpr float FFT_PROCESSING_TIME = 6.0f;
            // Percentage of overlap between to consecutive FFT windows
            static constexpr int FFT_WINDOW_OVERLAP_PERCENT = 99;
            // We only look at approx. 30% of the fft data cause we only want to analyze low frequency
            static constexpr int FFT_30HZ_LENGTH = 32; // ~30%
            //
            static constexpr int FFT_WINDOW_TIME_LENGTH = 4; // 2^2
            //
            static constexpr float FFT_SAMPLE_RATE = 128; // 2^7
            //
            static constexpr uint32_t FFT_WINDOW_SAMPLE_COUNT = static_cast<const uint32_t>(FFT_WINDOW_TIME_LENGTH *
                                                                                            FFT_SAMPLE_RATE); // 2^9
            //
            static constexpr int FFT_WINDOW_30HZ_DATA_SIZE = static_cast<const int>(FFT_30HZ_LENGTH /
                                                                                    (FFT_SAMPLE_RATE /
                                                                                     (float) FFT_WINDOW_SAMPLE_COUNT));
            // Number of samples between two FFT windows
            static constexpr int FFT_WINDOW_SAMPLE_DIFF_COUNT =
                    (int) (FFT_WINDOW_SAMPLE_COUNT * (1.0f - (FFT_WINDOW_OVERLAP_PERCENT / 100.0f)));
            // Number of FFT windows needed to render 6s of signal
            static constexpr int FFT_WINDOW_COUNT =
                    (int) ((FFT_PROCESSING_TIME * FFT_SAMPLE_RATE) / FFT_WINDOW_SAMPLE_DIFF_COUNT);

            void
            processSeek(float **outData, int windowCount, int &windowCounter, int &frequencyCounter, int channelCount,
                        short **inSamples, const int *inSampleCount);

//            long long currentTimeInMilliseconds();

            void init(float sampleRate);

            void clean();

            audiofft::AudioFFT fft;

            std::vector<float> input;
            std::vector<float> outReal;
            std::vector<float> outImaginary;

            // Size of single window of samples - must be 2^N
            int oWindowSampleCount;
            // Number of samples needed to be collected before starting new sample window
            int oWindowSampleDiffCount;
            //
            int oMaxWindowsSampleCount;

            // Whether buffers and normalization needs to be reset before processing
            bool resetOnNextCycle = false;

            // Holds all unanalyzed samples left from the latest sample batch
            float *unanalyzedSamples;
            int unanalyzedSampleCount;
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
