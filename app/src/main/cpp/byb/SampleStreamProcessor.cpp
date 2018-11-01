//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include "SampleStreamProcessor.h"

const char *SampleStreamProcessor::TAG = "SampleStreamProcessor";

const unsigned char SampleStreamProcessor::ESCAPE_SEQUENCE_START[] = {0xFF, 0xFF, 0x01, 0x01, 0x80, 0xFF};
const unsigned char SampleStreamProcessor::ESCAPE_SEQUENCE_END[] = {0xFF, 0xFF, 0x01, 0x01, 0x81, 0xFF};

SampleStreamProcessor::SampleStreamProcessor(OnEventListenerListener *listener) : Processor(SAMPLE_RATE,
                                                                                            DEFAULT_CHANNEL_COUNT) {
    SampleStreamProcessor::listener = listener;
}

SampleStreamProcessor::~SampleStreamProcessor() {
}

void SampleStreamProcessor::setChannelCount(int channelCount) {
    Processor::setChannelCount(channelCount);

    channelCountChanged = true;
}

void
SampleStreamProcessor::process(const unsigned char *inData, const int length, short **outSamples, int *outSampleCounts,
                               int *outEventIndices, std::string *outEventLabels, int &outEventCount) {
    if (channelCountChanged) { // number of channels changed during processing of previous batch
        frameStarted = false;
        sampleStarted = false;
        currentChannel = 0;

        channelCountChanged = false;
    }

    // init samples (by channels)
    for (int i = 0; i < getChannelCount(); i++) {
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
                unsigned char *copy = new unsigned char[eventMessageIndex + 1];
                std::copy(eventMessage, eventMessage + eventMessageIndex, copy);
                copy[eventMessageIndex] = 0;
                // let's process incoming message
                processEscapeSequenceMessage(copy, sampleIndex);

                delete[] copy;
                reset();
            } else if (ESCAPE_SEQUENCE_END[tmpIndex] == uc) {
                tmpIndex++;
                if (tmpIndex == ESCAPE_SEQUENCE_START_END_LENGTH) {
                    unsigned char *copy = new unsigned char[eventMessageIndex + 1];
                    std::copy(eventMessage, eventMessage + eventMessageIndex, copy);
                    copy[eventMessageIndex] = 0;
                    // let's process incoming message
                    processEscapeSequenceMessage(copy, sampleIndex);

                    delete[] copy;
                    reset();
                }
            } else {
                eventMessage[eventMessageIndex++] = uc;
            }
        } else {
            if (ESCAPE_SEQUENCE_START[tmpIndex] == uc) {
                tmpIndex++;
                if (tmpIndex == ESCAPE_SEQUENCE_START_END_LENGTH) {
                    tmpIndex = 0; // reset index, we need it for sequence end
                    insideEscapeSequence = true;
                }
                continue;
            }

            unsigned char *sequence = new unsigned char[escapeSequenceIndex];
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
                        msb = msb << 7;
                        lsb = lsb & REMOVER;
                        sample = (short) (((msb | lsb) - 512) * 30);

                        // calculate average sample
                        average = 0.0001 * sample + 0.9999 * average;
                        // use average to remove offset
                        sample = (short) (sample - average);

                        channels[currentChannel][sampleCounters[currentChannel]++] = sample;

                        sampleStarted = false;
                        if (currentChannel >= getChannelCount() - 1) frameStarted = false;
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

    for (int i = 0; i < getChannelCount(); i++) {
        // apply additional filtering if necessary
        applyFilters(i, channels[i], sampleCounters[i]);
        outSamples[i] = new short[sampleCounters[i]];
        std::copy(channels[i], channels[i] + sampleCounters[i], outSamples[i]);
        outSampleCounts[i] = sampleCounters[i];
    }
    std::copy(eventIndices, eventIndices + eventCounter, outEventIndices);
    std::copy(eventLabels, eventLabels + eventCounter, outEventLabels);
    outEventCount = eventCounter;
}

void SampleStreamProcessor::processEscapeSequenceMessage(unsigned char *messageBytes, int sampleIndex) {
    // check if it's board type message
    std::string message = reinterpret_cast<char *>(messageBytes);
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "EVENT MESSAGE: %s", message.c_str());
    if (SampleStreamUtils::isHardwareTypeMsg(message)) {
        listener->onSpikerBoxHardwareTypeDetected(SampleStreamUtils::getBoardType(message));
    } else if (SampleStreamUtils::isSampleRateAndNumOfChannelsMsg(message)) {
        listener->onMaxSampleRateAndNumOfChannelsReply(SampleStreamUtils::getMaxSampleRate(message),
                                                       SampleStreamUtils::getChannelCount(message));
    } else if (SampleStreamUtils::isEventMsg(message)) {
        eventIndices[eventCounter] = sampleIndex;
        eventLabels[eventCounter++] = SampleStreamUtils::getEventNumber(message);
    }
}

void SampleStreamProcessor::reset() {
    insideEscapeSequence = false;
    tmpIndex = 0;
    escapeSequenceIndex = 0;
    eventMessageIndex = 0;
}
