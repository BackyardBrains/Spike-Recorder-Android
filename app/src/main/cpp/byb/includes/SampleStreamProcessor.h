//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_SAMPLESTREAMPROCESSOR_H
#define SPIKE_RECORDER_ANDROID_SAMPLESTREAMPROCESSOR_H

#include "Processor.h"
#include "LowPassFilter.h"
#include "HighPassFilter.h"
#include "SampleStreamUtils.h"
#include <algorithm>
#include <string>
#include <android/log.h>

namespace backyardbrains {

    namespace processing {

        class SampleStreamProcessor : public Processor {
        public:
            explicit SampleStreamProcessor(backyardbrains::utils::OnEventListenerListener *listener);

            ~SampleStreamProcessor() override;

            void
            process(const unsigned char *inData, int length, short **outSamples, int *outSampleCounts,
                    int *outEventIndices,
                    std::string *outEventLabels, int &outEventCount, int channelCount,int hardwareType);

        private:
            static const char *TAG;

            static constexpr float DEFAULT_SAMPLE_RATE = 10000.0f;
            static constexpr float EXPANSION_BOARDS_SAMPLE_RATE = 5000.0f;
            static constexpr int DEFAULT_CHANNEL_COUNT = 2;
            static constexpr int HAMMER_JOYSTICK_CHANNEL_COUNT = 3;
            static constexpr int ADDITIONAL_INPUTS_CHANNEL_COUNT = 4;
            static constexpr int DEFAULT_BITS_PER_SAMPLE = 16;

            static constexpr uint CLEANER = 0xFF;
            static constexpr uint REMOVER = 0x7F;

            // Max number of channels is 10
            static constexpr int MAX_CHANNELS = 10;
            // Max number of bytes we can process in one batch
            static constexpr int MAX_SAMPLES = 5000;
            // We can maximally handle 6 seconds of sample data and spike can appear max every 200 ms
            static constexpr int MAX_EVENTS = 100;

            // Length of escape sequence start and sequence end
            static constexpr int ESCAPE_SEQUENCE_START_END_LENGTH = 6;
            // Message cannot be longer than 64 bytes
            static constexpr int EVENT_MESSAGE_LENGTH = 64;
            // Escape sequence cannot be longer than sequence start + 64 + sequence end
            static constexpr int MAX_SEQUENCE_LENGTH =
                    ESCAPE_SEQUENCE_START_END_LENGTH + EVENT_MESSAGE_LENGTH + ESCAPE_SEQUENCE_START_END_LENGTH;
            // Array of bytes which represent start of an escape sequence
            static const unsigned char ESCAPE_SEQUENCE_START[];
            // Array of bytes which represent end of an escape sequence
            static const unsigned char ESCAPE_SEQUENCE_END[];

            // Listener that's being invoked during sample stream processing on different event messages
            backyardbrains::utils::OnEventListenerListener *listener;

            typedef unsigned int byte;

            // Processes escape sequence message and triggers appropriate listener
            int processEscapeSequenceMessage(unsigned char *messageBytes, int sampleIndex);

            // Updates channel count and sample rate depending on the specified board type
            void updateProcessingParameters(int expansionBoardType);

            // Resets all variables used for processing escape sequences
            void reset();

//            uint batchCounter = 0;
//            bool batchPrinted = false;
//            unsigned char *inDataPrev = new unsigned char[10000];
//            int inDataPrevLength;

            // Whether new frame is started being processed
            bool frameStarted = false;
            // Whether new sample is started being processed
            bool sampleStarted = false;
            // Whether channel count has changed during processing of the latest chunk of incoming data
            int prevChannelCount;
            // Holds currently processed channel
            int currentChannel;
            // Beyond this channel index channels should not be filtered
            int stopFilteringAfterChannelIndex = -1;
            // Holds samples from all channels processed in a single batch
            short channels[MAX_CHANNELS][MAX_SAMPLES];
            // Array of sample counters, one for every channel
            int sampleCounters[MAX_CHANNELS];
            // Whether we are inside an escape sequence or not
            bool insideEscapeSequence;
            // Index of the byte within start or end of the escape sequence
            int tmpIndex;
            // Holds currently processed escape sequence
            unsigned char escapeSequence[MAX_SEQUENCE_LENGTH];
            // Index of the byte within currently processed escape sequence
            int escapeSequenceIndex;
            // Holds currently processed event message
            unsigned char eventMessage[EVENT_MESSAGE_LENGTH];
            // Index of the byte within currently processed event message
            int eventMessageIndex;
            // Holds count of processed events in the current sample batch
            int eventCounter;
            // Holds event indices processed in a single batch
            int eventIndices[MAX_EVENTS];
            // Holds event labels processed in a single batch
            std::string eventLabels[MAX_EVENTS];
            // Most significant and least significant bytes
            byte msb;
            // Average signal which we use to avoid signal offset
            double average;
        };
    }
}

#endif //SPIKE_RECORDER_ANDROID_SAMPLESTREAMPROCESSOR_H
