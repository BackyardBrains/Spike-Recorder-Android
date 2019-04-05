//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_PROCESSOR_H
#define SPIKE_RECORDER_ANDROID_PROCESSOR_H

#include <typeinfo>
#include <android/log.h>

#include "LowPassFilter.h"
#include "HighPassFilter.h"
#include "NotchFilter.h"

using namespace backyardbrains::filters;

namespace backyardbrains {

    namespace processing {

        class Processor {
        public:
            Processor(float sampleRate, int channelCount);

            Processor() : Processor(DEFAULT_SAMPLE_RATE, DEFAULT_CHANNEL_COUNT) {}

            virtual ~Processor();

            virtual void setSampleRate(float sampleRate);

            virtual void setChannelCount(int channelCount);

            void setSelectedChannel(int selectedChannel);

            void setBandFilter(float lowCutOffFreq, float highCutOffFreq);

            void setNotchFilter(float centerFreq);

        protected:
            float getSampleRate();

            int getChannelCount();

            int getSelectedChannel();

            void setSampleRateAndChannelCount(float sampleRate, int channelCount);

            void applyFilters(int channel, short *data, int sampleCount);

        private:
            // Minimum cut-off frequency
            static constexpr float MIN_FILTER_CUT_OFF = 0.0f;
            // Maximum cut-off frequency
            static constexpr float MAX_FILTER_CUT_OFF = 5000.0f;
            // Default sample rate we start with
            static constexpr float DEFAULT_SAMPLE_RATE = 44100.0f;
            // Default channel count
            static constexpr int DEFAULT_CHANNEL_COUNT = 1;

            void createFilters(int channelCount);

            void deleteFilters(int channelCount);

            typedef LowPassFilter *LowPassFilterPtr;
            typedef HighPassFilter *HighPassFilterPtr;
            typedef NotchFilter *NotchFilterPtr;

            // Flag that is set to true after initialization
            bool initialized = false;
            // Current sample rate
            float sampleRate = DEFAULT_SAMPLE_RATE;
            // Current channel count
            int channelCount = DEFAULT_CHANNEL_COUNT;
            // Index of currently selected channel
            int selectedChannel = 0;
            // Current low pass filter high cutoff frequency
            float highCutOff = MAX_FILTER_CUT_OFF;
            // Whether low pass filters should be applied
            bool lowPassFilteringEnabled = false;
            // Low pass filters for all channels
            LowPassFilterPtr *lowPassFilter;
            // Current high pass filter low cutoff frequency
            float lowCutOff = MIN_FILTER_CUT_OFF;
            // Whether high pass filters should be applied
            bool highPassFilteringEnabled = false;
            // High pass filters for all channels
            HighPassFilterPtr *highPassFilter;
            // Current notch filter center freq
            float centerFrequency = MIN_FILTER_CUT_OFF;
            // Whether notch filters should be applied
            bool notchFilteringEnabled = false;
            // Notch filters for all channels
            NotchFilterPtr *notchFilter;
        };
    }
}

#endif //SPIKE_RECORDER_ANDROID_PROCESSOR_H
