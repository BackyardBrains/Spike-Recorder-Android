//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include <FftProcessor.h>

namespace backyardbrains {

    namespace processing {

        const char *FftProcessor::TAG = "FftProcessor";

        FftProcessor::FftProcessor() {
            input.resize(FFT_WINDOW_SAMPLE_COUNT, 0.0f);
            outReal.resize(audiofft::AudioFFT::ComplexSize(FFT_WINDOW_SAMPLE_COUNT));
            outImaginary.resize(audiofft::AudioFFT::ComplexSize(FFT_WINDOW_SAMPLE_COUNT));

            // initialize object that's doing actual FFT analysis
            fft.init(FFT_WINDOW_SAMPLE_COUNT);

            init(getSampleRate());
        }

        FftProcessor::~FftProcessor() {
            std::vector<float>().swap(input);
            std::vector<float>().swap(outReal);
            std::vector<float>().swap(outReal);

            delete[] unanalyzedSamples;
            delete[] sampleBuffer;
        }

        void FftProcessor::setSampleRate(float sampleRate) {
            __android_log_print(ANDROID_LOG_DEBUG, TAG, "setSampleRate(%f)", sampleRate);
            Processor::setSampleRate(sampleRate);

            resetOnNextCycle = true;
        }

        void
        FftProcessor::process(float **outData, int windowCount, int &windowCounter, int &frequencyCounter,
                              int channelCount, short **inSamples, const int *inSampleCount) {
//            long long start = currentTimeInMilliseconds();
//            __android_log_print(ANDROID_LOG_DEBUG, TAG, "%ld - AFTER CLEAN AND INIT -> %d",
//                                static_cast<long>(currentTimeInMilliseconds() - start), oWindowSampleCount);

            float sampleRate = getSampleRate();
            auto selectedChannel = getSelectedChannel();
            // check if data for existing channel exists
            if (selectedChannel >= channelCount) {
                windowCounter = 0;
                frequencyCounter = 0;
                return;
            }
            auto sampleCount = inSampleCount[selectedChannel];

            // if max number of samples is sent it means we are seeking
            if (sampleCount == oMaxWindowsSampleCount) {
                processSeek(outData, windowCount, windowCounter, frequencyCounter, channelCount, inSamples,
                            inSampleCount);
                return;
            }

            if (resetOnNextCycle) {
                clean();
                init(sampleRate);
                resetOnNextCycle = false;
            }

            auto *samples = inSamples[selectedChannel];

            const int newUnanalyzedSampleCount = unanalyzedSampleCount + sampleCount;
            auto addWindowsCount = static_cast<uint16_t>(newUnanalyzedSampleCount / oWindowSampleDiffCount);

            if (addWindowsCount == 0) {
                std::copy(samples, samples + sampleCount, unanalyzedSamples + unanalyzedSampleCount);
                unanalyzedSampleCount += sampleCount;

                windowCounter = addWindowsCount;
                frequencyCounter = FFT_WINDOW_30HZ_DATA_SIZE;

                return;
            } else if (addWindowsCount >= windowCount) {
                processSeek(outData, windowCount, windowCounter, frequencyCounter, channelCount, inSamples,
                            inSampleCount);
                return;
            }

            auto *samplesToAnalyze = new float[newUnanalyzedSampleCount];
            if (unanalyzedSampleCount > 0)
                std::copy(unanalyzedSamples, unanalyzedSamples + unanalyzedSampleCount, samplesToAnalyze);
            std::copy(samples, samples + sampleCount, samplesToAnalyze + unanalyzedSampleCount);


            int offset = 0;
            auto *in = new short[oWindowSampleCount]{0};
            auto dsSamples = new float[dsIndexCount]{0};
            int counter = 0;
            for (int i = 0; i < addWindowsCount; i++) {
                // construct next window of data for analysis
                offset = oWindowSampleDiffCount * i;
                std::copy(sampleBuffer + oWindowSampleDiffCount, sampleBuffer + oWindowSampleCount, in);
                std::copy(samplesToAnalyze + offset, samplesToAnalyze + offset + oWindowSampleDiffCount,
                          in + oWindowSampleCount - oWindowSampleDiffCount);

                // simple downsampling because only low frequencies are required
                for (int j = 0; j < dsIndexCount; j++)
                    dsSamples[j] = in[dsIndices[j]];

                // perform FFT analysis
                input.assign(dsSamples, dsSamples + dsIndexCount);

                fft.fft(input.data(), outReal.data(), outImaginary.data());

                // calculate DC component
                outData[counter][0] = static_cast<float>(sqrtf(outReal[0] * outReal[0]) / halfMaxMagnitude - 1.0);
                // calculate magnitude for all freq.
                for (int j = 1; j < FFT_WINDOW_30HZ_DATA_SIZE; j++) {
                    outData[counter][j] = sqrtf(outReal[j] * outReal[j] + outImaginary[j] * outImaginary[j]);
                    if (outData[counter][j] > maxMagnitude) {
                        maxMagnitude = outData[counter][j];
                        halfMaxMagnitude = maxMagnitude * 0.5f;
                    }
                    outData[counter][j] = static_cast<float>(outData[counter][j] / halfMaxMagnitude - 1.0);
                }
                counter++;

                std::move(sampleBuffer + oWindowSampleDiffCount, sampleBuffer + oWindowSampleCount, sampleBuffer);
                std::copy(samplesToAnalyze + offset, samplesToAnalyze + offset + oWindowSampleDiffCount,
                          sampleBuffer + oWindowSampleCount - oWindowSampleDiffCount);
            }

            std::copy(samplesToAnalyze + oWindowSampleDiffCount * addWindowsCount,
                      samplesToAnalyze + newUnanalyzedSampleCount, unanalyzedSamples);
            unanalyzedSampleCount = newUnanalyzedSampleCount - oWindowSampleDiffCount * addWindowsCount;

            windowCounter = counter;
            frequencyCounter = FFT_WINDOW_30HZ_DATA_SIZE;

            delete[] dsSamples;
            delete[] in;
            delete[] samplesToAnalyze;
        }

        void FftProcessor::processSeek(float **outData, int windowCount, int &windowCounter, int &frequencyCounter,
                                       int channelCount, short **inSamples, const int *inSampleCount) {
            float sampleRate = getSampleRate();
            auto selectedChannel = getSelectedChannel();
            // check if data for existing channel exists
            if (selectedChannel >= channelCount) {
                windowCounter = 0;
                frequencyCounter = 0;
                return;
            }

            if (resetOnNextCycle) {
                clean();
                init(sampleRate);
                resetOnNextCycle = false;
            }

            auto sampleCount = inSampleCount[selectedChannel];
            auto *samples = inSamples[selectedChannel];

            // just take exact number of samples that we need not all that came in
            auto *tmpSamples = new short[oMaxWindowsSampleCount]{0};
            int start1 = std::max(0, sampleCount - oMaxWindowsSampleCount);
            int start2 = std::max(0, oMaxWindowsSampleCount - sampleCount);
            std::copy(samples + start1, samples + sampleCount, tmpSamples + start2);


            int offset = 0;
            auto *in = new short[oWindowSampleCount]{0};
            auto dsSamples = new float[dsIndexCount]{0};
            int counter = 0;
            for (int i = 0; i < windowCount; i++) {
                // construct next window of data for analysis
                offset = oWindowSampleDiffCount * i;
                std::copy(tmpSamples + offset, tmpSamples + offset + oWindowSampleCount, in);

                // simple downsampling because only low frequencies are required
                for (int j = 0; j < dsIndexCount; j++)
                    dsSamples[j] = in[dsIndices[j]];

                // perform FFT analysis
                input.assign(dsSamples, dsSamples + dsIndexCount);
                fft.fft(input.data(), outReal.data(), outImaginary.data());

                // calculate DC component
                outData[counter][0] = static_cast<float>(sqrtf(outReal[0] * outReal[0]) / halfMaxMagnitude -
                                                         1.0);
                // calculate magnitude for all freq.
                for (int j = 1; j < FFT_WINDOW_30HZ_DATA_SIZE; j++) {
                    outData[counter][j] = sqrtf(outReal[j] * outReal[j] + outImaginary[j] * outImaginary[j]);
                    if (outData[counter][j] > maxMagnitude) {
                        maxMagnitude = outData[counter][j];
                        halfMaxMagnitude = maxMagnitude * 0.5f;
                    }
                    outData[counter][j] = static_cast<float>(outData[counter][j] / halfMaxMagnitude - 1.0);
                }
                counter++;

            }

            std::copy(tmpSamples + oMaxWindowsSampleCount - oWindowSampleCount, tmpSamples + oMaxWindowsSampleCount,
                      sampleBuffer);

            windowCounter = counter;
            frequencyCounter = FFT_WINDOW_30HZ_DATA_SIZE;

            delete[] dsSamples;
            delete[] tmpSamples;
            delete[] in;
        }

//        long long FftProcessor::currentTimeInMilliseconds() {
//            struct timeval tv{};
//            gettimeofday(&tv, nullptr);
//            return ((tv.tv_sec * 1000) + (tv.tv_usec / 1000));
//        }

        void FftProcessor::init(float sampleRate) {
            __android_log_print(ANDROID_LOG_DEBUG, TAG, "init(%f)", sampleRate);

            // calculate number of samples that fit in single FFT window before downsampling
            oWindowSampleCount = static_cast<uint32_t>(FFT_WINDOW_TIME_LENGTH * sampleRate);
            // calculate difference between two consecutive FFT windows represented in number of samples
            oWindowSampleDiffCount = static_cast<uint32_t>(oWindowSampleCount *
                                                           (1.0f - (float) FFT_WINDOW_OVERLAP_PERCENT / 100.0f));
            oMaxWindowsSampleCount =
                    FFT_WINDOW_COUNT * oWindowSampleDiffCount + oWindowSampleCount - oWindowSampleDiffCount;

            // cannot hold more then oWindowSampleCount - 1 number of samples
            unanalyzedSamples = new float[oWindowSampleCount]{0};
            unanalyzedSampleCount = 0;

            // always holds oWindowSampleCount number of samples (0s at the begining)
            sampleBuffer = new float[oWindowSampleCount]{0};

            auto dsFactor = static_cast<int>(FFT_WINDOW_TIME_LENGTH * sampleRate / FFT_WINDOW_SAMPLE_COUNT);
            dsIndexCount = oWindowSampleCount / dsFactor;
            dsIndices = new int[dsIndexCount];
            for (int j = 0; j < dsIndexCount; j++)
                dsIndices[j] = dsFactor * j;
        }

        void FftProcessor::clean() {
            delete[] unanalyzedSamples;
            unanalyzedSampleCount = 0;

            delete[] sampleBuffer;

            delete[] dsIndices;
        }
    }
}