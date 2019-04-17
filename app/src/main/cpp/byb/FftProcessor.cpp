//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include "FftProcessor.h"

namespace backyardbrains {

    namespace processing {

        const char *FftProcessor::TAG = "FftProcessor";

        FftProcessor::FftProcessor() {
            init();
        }

        FftProcessor::~FftProcessor() {
            std::vector<float>().swap(input);
            std::vector<float>().swap(outReal);
            std::vector<float>().swap(outReal);

            delete[] unanalyzedSamples;
            delete[] sampleBuffer;
        }

//long long FftProcessor::currentTimeInMilliseconds() {
//    struct timeval tv{};
//    gettimeofday(&tv, nullptr);
//    return ((tv.tv_sec * 1000) + (tv.tv_usec / 1000));
//}

        void FftProcessor::process(float **outData, uint32_t &windowCount, uint32_t &windowSize, short **inSamples,
                                   uint32_t *inSampleCount) {
            auto selectedChannel = getSelectedChannel();
            auto *samples = inSamples[selectedChannel];
            auto sampleCount = inSampleCount[selectedChannel];

//    long long start = currentTimeInMilliseconds();

//    __android_log_print(ANDROID_LOG_DEBUG, TAG, "========================================");

            // simple downsampling because only low frequencies are required
            const uint8_t dsFactor = FFT_DOWNSAMPLING_FACTOR;
            auto dsLength = sampleCount / dsFactor;
            auto dsSamples = new short[dsLength]{0};
            for (int i = 0; i < dsLength; i++)
                dsSamples[i] = samples[dsFactor * i];

//    __android_log_print(ANDROID_LOG_DEBUG, TAG, "%ld - AFTER DOWNSAMPLING",
//                        static_cast<long>(currentTimeInMilliseconds() - start));

            const uint32_t newUnanalyzedSampleCount = unanalyzedSampleCount + dsLength;
            auto addWindowsCount = static_cast<uint16_t>(newUnanalyzedSampleCount / windowSampleDiffCount);

            if (addWindowsCount == 0) {
                std::copy(dsSamples, dsSamples + dsLength, unanalyzedSamples + unanalyzedSampleCount);
                unanalyzedSampleCount += dsLength;

                windowCount = addWindowsCount;
                windowSize = thirtyHzDataSize;

                return;
            }

            auto *samplesToAnalyze = new float[newUnanalyzedSampleCount];
            if (unanalyzedSampleCount > 0)
                std::copy(unanalyzedSamples, unanalyzedSamples + unanalyzedSampleCount, samplesToAnalyze);
            std::copy(dsSamples, dsSamples + dsLength, samplesToAnalyze + unanalyzedSampleCount);

            uint32_t offset = 0;
            auto *in = new float[sampleWindowSize]{0};
//    __android_log_print(ANDROID_LOG_DEBUG, TAG, "=====");
            for (int i = 0; i < addWindowsCount; i++) {
                // construct next window of data for analysis
                offset = windowSampleDiffCount * i;
                std::copy(sampleBuffer + windowSampleDiffCount, sampleBuffer + sampleWindowSize, in);
                std::copy(samplesToAnalyze + offset, samplesToAnalyze + offset + windowSampleDiffCount,
                          in + sampleWindowSize - windowSampleDiffCount);

                // perform FFT analysis
//                input.resize(sampleWindowSize);
                input.assign(in, in + sampleWindowSize);

                fft.fft(input.data(), outReal.data(), outImaginary.data());

//        __android_log_print(ANDROID_LOG_DEBUG, TAG, "%ld - AFTER FFT ANALYSIS",
//                            static_cast<long>(currentTimeInMilliseconds() - start));

                // calculate DC component
                outData[i][0] = static_cast<float>(sqrtf(outReal[0] * outReal[0]) / halfMaxMagnitude - 1.0);
                // calculate magnitude for all freq.
                for (int j = 1; j < thirtyHzDataSize; j++) {
                    outData[i][j] = sqrtf(outReal[j] * outReal[j] + outImaginary[j] * outImaginary[j]);
                    if (outData[i][j] > maxMagnitude) {
                        maxMagnitude = outData[i][j];
                        halfMaxMagnitude = maxMagnitude * 0.5f;
                        maxMagnitudeOptimized = static_cast<float>(outData[i][j] * 0.001953125);// 1/512
                        halfMaxMagnitudeOptimized = maxMagnitudeOptimized * 0.5f;
                    }
                    outData[i][j] = static_cast<float>(outData[i][j] / halfMaxMagnitude - 1.0);
                }

                std::move(sampleBuffer + windowSampleDiffCount, sampleBuffer + sampleWindowSize, sampleBuffer);
                std::copy(samplesToAnalyze + offset, samplesToAnalyze + offset + windowSampleDiffCount,
                          sampleBuffer + sampleWindowSize - windowSampleDiffCount);
            }
//    __android_log_print(ANDROID_LOG_DEBUG, TAG, "=====");

            std::copy(samplesToAnalyze + windowSampleDiffCount * addWindowsCount,
                      samplesToAnalyze + newUnanalyzedSampleCount,
                      unanalyzedSamples);
            unanalyzedSampleCount = newUnanalyzedSampleCount - windowSampleDiffCount * addWindowsCount;

            windowCount = addWindowsCount;
            windowSize = thirtyHzDataSize;

            delete[] in;
            delete[] samplesToAnalyze;
        }

        void FftProcessor::init() {
            __android_log_print(ANDROID_LOG_DEBUG, TAG, "init()");
            float fftSampleRate = getSampleRate() / FFT_DOWNSAMPLING_FACTOR;

            // try to make under 1Hz resolution if it is too much than limit it to samplingRate/2^11
            auto log2n = static_cast<int>(log2f(fftSampleRate));
            sampleWindowSize = static_cast<uint32_t>(pow(2, log2n + 2));
            // Size of fft data that will be returned when providing sampleWindowSize of samples
            auto fftDataSize = static_cast<uint32_t>(sampleWindowSize * .5f);
            // Difference between two consecutive frequency values represented in the output graph
            float oneFrequencyStep = .5f * fftSampleRate / (float) fftDataSize;
            thirtyHzDataSize = static_cast<uint32_t>(FFT_30HZ_LENGTH / oneFrequencyStep);

            windowSampleDiffCount = static_cast<uint32_t>(sampleWindowSize *
                                                          (1.0f - ((float) windowOverlapPercent / 100.0f)));

            input.resize(sampleWindowSize, 0.0f);
            outReal.resize(audiofft::AudioFFT::ComplexSize(sampleWindowSize));
            outImaginary.resize(audiofft::AudioFFT::ComplexSize(sampleWindowSize));

            // initialize object that's doing actual FFT analysis
            fft.init(sampleWindowSize);

            // cannot hold more then sampleWindowSize - 1 number of samples
            unanalyzedSamples = new float[sampleWindowSize]{0};
            unanalyzedSampleCount = 0;

            // always holds sampleWindowSize number of samples (0s at the begining)
            sampleBuffer = new float[sampleWindowSize]{0};

            maxMagnitude = 20;
            halfMaxMagnitude = 20;
            maxMagnitudeOptimized = 4.83;
            halfMaxMagnitudeOptimized = maxMagnitudeOptimized * .5f;
        }
    }
}