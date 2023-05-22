//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include "SampleStreamProcessor.h"
#include "SampleStreamUtils.h"

namespace backyardbrains {

    namespace processing {

        const char *SampleStreamProcessor::TAG = "SampleStreamProcessor";

        const unsigned char SampleStreamProcessor::ESCAPE_SEQUENCE_START[] = {0xFF, 0xFF, 0x01, 0x01, 0x80, 0xFF};
        const unsigned char SampleStreamProcessor::ESCAPE_SEQUENCE_END[] = {0xFF, 0xFF, 0x01, 0x01, 0x81, 0xFF};

        SampleStreamProcessor::SampleStreamProcessor(backyardbrains::utils::OnEventListenerListener *listener)
                : Processor(DEFAULT_SAMPLE_RATE, DEFAULT_CHANNEL_COUNT, DEFAULT_BITS_PER_SAMPLE) {
            SampleStreamProcessor::listener = listener;
        }

        SampleStreamProcessor::~SampleStreamProcessor() = default;


        void SampleStreamProcessor::process(const unsigned char *inData, const int length, short **outSamples,
                                       int *outSampleCounts, int *outEventIndices, std::string *outEventLabels,
                                       int &outEventCount, const int channelCount, int hardwareType) {
//            batchCounter++;

            if (prevChannelCount != channelCount) { // number of channels changed during processing of previous batch
                frameStarted = false;
                sampleStarted = false;
                currentChannel = 0;
            }

            // init samples (by channels)
            for (int i = 0; i < channelCount; i++) {
                sampleCounters[i] = 0;
            }
            // init events
            eventCounter = 0;

            short sample;
            int sampleIndex;
            byte lsb; // last significant byte
            byte b; // temp variable to hold currently processed bytes
            unsigned char uc; // temp variable to hold currently processed bytes as unsigned char

            for (int i = 0; i < length; i++) {
                uc = inData[i];

                // and next byte to custom message sent by SpikerBox
                escapeSequence[escapeSequenceIndex++] = uc;

                if (insideEscapeSequence) { // we are inside escape sequence
                    sampleIndex = sampleCounters[currentChannel] == 0 ? 0 : sampleCounters[currentChannel] - 1;
                    if (eventMessageIndex >= EVENT_MESSAGE_LENGTH) { // event message shouldn't be longer then 64 bytes
                        auto *copy = new unsigned char[eventMessageIndex + 1];
                        std::copy(eventMessage, eventMessage + eventMessageIndex, copy);
                        copy[eventMessageIndex] = 0;
                        // let's process incoming message
                        hardwareType=processEscapeSequenceMessage(copy, sampleIndex);

                        delete[] copy;
                        reset();
                    } else if (ESCAPE_SEQUENCE_END[tmpIndex] == uc) {
                        tmpIndex++;
                        if (tmpIndex == ESCAPE_SEQUENCE_START_END_LENGTH) {
                            auto *copy = new unsigned char[eventMessageIndex + 1];
                            std::copy(eventMessage, eventMessage + eventMessageIndex, copy);
                            copy[eventMessageIndex] = 0;
                            // let's process incoming message
                            hardwareType=      processEscapeSequenceMessage(copy, sampleIndex);

                            delete[] copy;
                            reset();
                        }
                    } else {
                        eventMessage[eventMessageIndex++] = uc;
                    }
                }
                else {
                    if (ESCAPE_SEQUENCE_START[tmpIndex] == uc) {
                        tmpIndex++;
                        if (tmpIndex == ESCAPE_SEQUENCE_START_END_LENGTH) {
                            tmpIndex = 0; // reset index, we need it for sequence end
                            insideEscapeSequence = true;
                        }
                        continue;
                    }

                    auto *sequence = new unsigned char[escapeSequenceIndex];
                    std::copy(escapeSequence, escapeSequence + escapeSequenceIndex, sequence);
                    for (int j = 0; j < escapeSequenceIndex; j++) {
                        b = sequence[j];
                        // check if we have unfinished frame
                        if (frameStarted) {
                            // check if we have unfinished sample
                            if (sampleStarted) {
                                lsb = b & CLEANER;

                                // if less significant byte is also grater then 127 drop whole frame
                                if (lsb > 127) {
                                    __android_log_print(ANDROID_LOG_DEBUG, TAG, "LSB > 127! DROP WHOLE FRAME!");
                                    frameStarted = false;
                                    sampleStarted = false;
                                    currentChannel = 0;
                                    continue;
                                }

                                // get sample value from most and least significant bytes
                                msb = msb & REMOVER;
                                msb = msb << 7u;
                                lsb = lsb & REMOVER;
                                if(backyardbrains::utils::SampleStreamUtils::HUMAN_HARDWARE==hardwareType){
                                    sample = (short) (((msb | lsb) - 8192) );
                                }else{
                                    sample = (short) (((msb | lsb) - 512) * 30);
                                }

                                // calculate average sample
                                average = 0.0001 * sample + 0.9999 * average;
                                // use average to remove offset
                                sample = (short) (sample - average);

                                channels[currentChannel][sampleCounters[currentChannel]++] = sample;

                                sampleStarted = false;
                                if (currentChannel >= channelCount - 1) frameStarted = false;
                            } else {
                                msb = b & CLEANER;
                                // we already started the frame so if msb is greater then 127 drop whole frame
                                if (msb > 127) {
                                    __android_log_print(ANDROID_LOG_DEBUG, TAG,
                                                        "MSB > 127 WITHIN THE FRAME! DROP WHOLE FRAME!");

                                    frameStarted = false;
                                    sampleStarted = false;
                                    currentChannel = 0;
                                } else {
                                    currentChannel++;

                                    sampleStarted = true;
                                }
                            }
                        } else {
                            msb = b & CLEANER;
                            if (msb > 127) {
                                currentChannel = 0;

                                frameStarted = true;
                                sampleStarted = true;
                            } else {
                                __android_log_print(ANDROID_LOG_DEBUG, TAG, "MSB < 128 AT FRAME START! DROP!");
//                                if (batchCounter > 100 && !batchPrinted) {
//                                    __android_log_print(ANDROID_LOG_DEBUG, TAG, "PREV BATCH: (%d)", inDataPrevLength);
//                                    char tmp[6];
//                                    char prev[inDataPrevLength * 5 - 2];
//                                    prev[0] = 0;
//                                    for (int r = 0; r < inDataPrevLength; r++) {
//                                        if (r == 0) {
//                                            sprintf(tmp, "%u", inDataPrev[r]);
//                                        } else {
//                                            sprintf(tmp, " ,%u", inDataPrev[r]);
//                                        }
//                                        strcat(prev, tmp);
//                                    }
//                                    __android_log_print(ANDROID_LOG_DEBUG, TAG, "%s", prev);
//                                    __android_log_print(ANDROID_LOG_DEBUG, TAG, "NEW BATCH: (%d)", length);
//                                    char current[inDataPrevLength * 5 - 2];
//                                    current[0] = 0;
//                                    for (int r = 0; r < length; r++) {
//                                        if (r == 0) {
//                                            sprintf(tmp, "%u", inData[r]);
//                                        } else {
//                                            sprintf(tmp, " ,%u", inData[r]);
//                                        }
//                                        strcat(current, tmp);
//                                    }
//                                    __android_log_print(ANDROID_LOG_DEBUG, TAG, "%s", current);
//                                    __android_log_print(ANDROID_LOG_DEBUG, TAG,
//                                                        "------------------------------------- (%d)", i);
//
//                                    batchPrinted = true;
//                                }

                                frameStarted = false;
                                sampleStarted = false;
                                currentChannel = 0;
                            }
                        }
                    }

                    delete[] sequence;

                    reset();
                }
            }

//            std::copy(inData, inData + length, inDataPrev);
//            inDataPrevLength = length;

            bool avoidFilteringOfChannels = stopFilteringAfterChannelIndex >= 0;
            for (int i = 0; i < channelCount; i++) {
                // apply additional filtering if necessary
                if (avoidFilteringOfChannels && i <= stopFilteringAfterChannelIndex)
                    applyFilters(i, channels[i], sampleCounters[i]);
                outSamples[i] = new short[sampleCounters[i]];
                std::copy(channels[i], channels[i] + sampleCounters[i], outSamples[i]);
                outSampleCounts[i] = sampleCounters[i];
            }
            std::copy(eventIndices, eventIndices + eventCounter, outEventIndices);
            std::copy(eventLabels, eventLabels + eventCounter, outEventLabels);
            outEventCount = eventCounter;

            prevChannelCount = channelCount;
        }

        int SampleStreamProcessor::processEscapeSequenceMessage(unsigned char *messageBytes, int sampleIndex) {
            // check if it's board type message
            std::string message = reinterpret_cast<char *>(messageBytes);
            __android_log_print(ANDROID_LOG_DEBUG, TAG, "ESCAPE SEQUENCE MESSAGE %s AT %d", message.c_str(),
                                sampleIndex);
            int hardwareType=-1;
            if (backyardbrains::utils::SampleStreamUtils::isHardwareTypeMsg(message)) {
                hardwareType=backyardbrains::utils::SampleStreamUtils::getHardwareType(message);
                listener->onSpikerBoxHardwareTypeDetected(
                        hardwareType);
            } else if (backyardbrains::utils::SampleStreamUtils::isSampleRateAndNumOfChannelsMsg(message)) {
                const int sampleRate = backyardbrains::utils::SampleStreamUtils::getMaxSampleRate(message);
                const int channelCount = backyardbrains::utils::SampleStreamUtils::getChannelCount(message);
                listener->onMaxSampleRateAndNumOfChannelsReply(sampleRate, channelCount);
                setSampleRateAndChannelCount(sampleRate, channelCount);
            } else if (backyardbrains::utils::SampleStreamUtils::isEventMsg(message)) {
                eventIndices[eventCounter] = sampleIndex;
                eventLabels[eventCounter++] = backyardbrains::utils::SampleStreamUtils::getEventNumber(message);
            } else if (backyardbrains::utils::SampleStreamUtils::isExpansionBoardTypeMsg(message)) {
                const int expansionBoardType = backyardbrains::utils::SampleStreamUtils::getExpansionBoardType(message);
                listener->onExpansionBoardTypeDetection(expansionBoardType);
                updateProcessingParameters(expansionBoardType);
            } else if (backyardbrains::utils::SampleStreamUtils::isHumanSpikerBoxType300(message)) {
                const int boardState = backyardbrains::utils::SampleStreamUtils::getHumanSpikerBoxType300(message);
                listener->onHumanSpikerBoardState(boardState);
            } else if (backyardbrains::utils::SampleStreamUtils::isHumanSpikerBoxType300Audio(message)) {
                const int audioState = backyardbrains::utils::SampleStreamUtils::getHumanSpikerBoxType300Audio(message);
                listener->onHumanSpikerBoardAudioState(audioState);
            }
            return hardwareType;
        }

        void SampleStreamProcessor::updateProcessingParameters(int expansionBoardType) {
            switch (expansionBoardType) {
                default:
                case backyardbrains::utils::SampleStreamUtils::NONE_BOARD_DETACHED:
                    setSampleRateAndChannelCount(DEFAULT_SAMPLE_RATE, DEFAULT_CHANNEL_COUNT);
                    stopFilteringAfterChannelIndex = -1;
                    break;
                case backyardbrains::utils::SampleStreamUtils::ADDITIONAL_INPUTS_EXPANSION_BOARD:
                    setSampleRateAndChannelCount(EXPANSION_BOARDS_SAMPLE_RATE, ADDITIONAL_INPUTS_CHANNEL_COUNT);
                    stopFilteringAfterChannelIndex = 1;
                    break;
                case backyardbrains::utils::SampleStreamUtils::HAMMER_EXPANSION_BOARD:
                case backyardbrains::utils::SampleStreamUtils::JOYSTICK_EXPANSION_BOARD:
                    setSampleRateAndChannelCount(EXPANSION_BOARDS_SAMPLE_RATE, HAMMER_JOYSTICK_CHANNEL_COUNT);
                    stopFilteringAfterChannelIndex = 1;
                    break;
            }
        }

        void SampleStreamProcessor::reset() {
            insideEscapeSequence = false;
            tmpIndex = 0;
            escapeSequenceIndex = 0;
            eventMessageIndex = 0;
        }
    }
}