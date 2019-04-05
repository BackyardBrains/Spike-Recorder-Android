//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_AMMODULATIONPROCESSOR_H
#define SPIKE_RECORDER_ANDROID_AMMODULATIONPROCESSOR_H

#include <cmath>
#include <algorithm>
#include <string>
#include <android/log.h>

#include "Processor.h"
#include "NotchFilter.h"
#include "LowPassFilter.h"
#include "HighPassFilter.h"
#include "SignalUtils.h"

namespace backyardbrains {

    namespace processing {

        class AmModulationProcessor : public Processor {
        public:
            AmModulationProcessor();

            ~AmModulationProcessor() override;

            void setSampleRate(float sampleRate) override;

            void setChannelCount(int channelCount) override;

            bool isReceivingAmSignal();

            void process(const short *inSamples, short **outSamples, int sampleCount, int frameCount);

        private:
            static const char *TAG;

            static constexpr int DEFAULT_CHANNEL_COUNT = 1;
            static constexpr float AM_CARRIER_FREQUENCY = 5000.0f;
            static constexpr float AM_DETECTION_CUTOFF = 6000.0f;
            static constexpr int AM_DEMODULATION_LOW_PASS_FILTER_COUNT = 3;
            static constexpr float AM_DEMODULATION_CUTOFF = 500.0f;

            void createDemodulationFilters();

            void deleteDemodulationFilters();

            // Used for detection of the AM modulation
            LowPassFilter amDetectionLowPassFilter;
            // Used for detection of the AM modulation
            NotchFilter amDetectionNotchFilter;
            // Used for signal demodulation
            LowPassFilter **amDemodulationLowPassFilter;

            // Flag that is set to true after initialization
            bool initialized = false;
            // Used to detect whether signal is modulated or not
            float rmsOfOriginalSignal = 0;
            float rmsOfNotchedAMSignal = 0;
            // Average signal which we use to avoid signal offset
            float average;
            // Whether we are currently receiving modulated signal
            bool receivingAmSignal = false;
        };

    }
}


#endif //SPIKE_RECORDER_ANDROID_AMMODULATIONPROCESSOR_H
