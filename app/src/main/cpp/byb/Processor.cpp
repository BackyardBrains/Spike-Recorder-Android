//
// Created by  Tihomir Leka <tihomir at backyardbrains.com>
//

#include "Processor.h"

namespace backyardbrains {

    namespace processing {

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
            if (initialized) deleteFilters(channelCount);
            Processor::sampleRate = sampleRate;

            createFilters(channelCount);
        }

        int Processor::getChannelCount() {
            return channelCount;
        }

        void Processor::setChannelCount(int channelCount) {
            if (initialized) deleteFilters(Processor::channelCount);
            Processor::channelCount = channelCount;

            createFilters(channelCount);
        }

        int Processor::getSelectedChannel() {
            return selectedChannel;
        }

        void Processor::setSelectedChannel(int selectedChannel) {
            Processor::selectedChannel = selectedChannel;
        }

        void Processor::setSampleRateAndChannelCount(float sampleRate, int channelCount) {
            __android_log_print(ANDROID_LOG_DEBUG, typeid(*this).name(), "SAMPLE RATE: %1f, CHANNEL COUNT: %1d",
                                sampleRate, channelCount);

            if (initialized) deleteFilters(Processor::channelCount);
            Processor::channelCount = channelCount;

            createFilters(channelCount);
        }

        void Processor::applyFilters(int channel, short *data, int sampleCount) {
            if (lowPassFilteringEnabled) lowPassFilter[channel]->filter(data, sampleCount);
            if (highPassFilteringEnabled) highPassFilter[channel]->filter(data, sampleCount);
            if (notchFilteringEnabled) notchFilter[channel]->filter(data, sampleCount);

        }

        void Processor::setBandFilter(float lowCutOffFreq, float highCutOffFreq) {
            lowPassFilteringEnabled = highCutOffFreq != -1 && highCutOffFreq != MAX_FILTER_CUT_OFF;
            highPassFilteringEnabled = lowCutOffFreq != -1 && lowCutOffFreq != MIN_FILTER_CUT_OFF;

            Processor::lowCutOff = lowCutOffFreq;
            Processor::highCutOff = highCutOffFreq;

            if (initialized) deleteFilters(channelCount);
            createFilters(channelCount);
        }

        void Processor::setNotchFilter(float centerFreq) {
            notchFilteringEnabled = centerFreq != -1 && centerFreq != MIN_FILTER_CUT_OFF;

            Processor::centerFrequency = centerFreq;

            if (initialized) deleteFilters(channelCount);
            createFilters(channelCount);
        }

        void Processor::createFilters(int channelCount) {
            lowPassFilter = new LowPassFilterPtr[channelCount];
            highPassFilter = new HighPassFilterPtr[channelCount];
            notchFilter = new NotchFilterPtr[channelCount];
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
                // notch filter
                notchFilter[i] = new NotchFilter();
                notchFilter[i]->initWithSamplingRate(sampleRate);
                notchFilter[i]->setCenterFrequency(centerFrequency);
                notchFilter[i]->setQ(1.0);
            }
        }

        void Processor::deleteFilters(int channelCount) {
            for (int i = 0; i < channelCount; i++) {
                delete lowPassFilter[i];
                delete highPassFilter[i];
                delete notchFilter[i];
            }
            delete[] lowPassFilter;
            delete[] highPassFilter;
            delete[] notchFilter;
        }
    }
}