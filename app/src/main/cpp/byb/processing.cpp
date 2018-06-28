//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include <FilterBase.h>
#include <LowPassFilter.h>
#include <HighPassFilter.h>
#include "includes/processing.h"

// Whether new frame is started being processed
bool frameStarted = false;
// Whether new sample is started being processed
bool sampleStarted = false;
// Number of channels
int channelCount = DEFAULT_CHANNEL_COUNT;
// Whether channel count has changed during processing of the latest chunk of incoming data
bool channelCountChanged = true;
// Holds currently processed channel
int currentChannel;
// Holds samples from all channels processed in a single batch
short channels[MAX_CHANNELS][MAX_BYTES];
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
// Current filters
bool lowPassEnabled = false;
bool highPassEnabled = false;
LowPassFilter lowPass;
HighPassFilter highPass;
// Determines what filter to use
int sampleRate = 10000;

void setSampleRate(int sampleRate) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "SAMPLE RATE: %d", sampleRate);
    ::sampleRate = sampleRate;
}

void setFilters(float lowCutOff, float highCutOff) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "LOW: %1f, HIGH: %1f", lowCutOff, highCutOff);
    lowPassEnabled = highCutOff != -1 && highCutOff != MAX_FILTER_CUT_OFF;
    if (lowPassEnabled) {
        lowPass.initWithSamplingRate(sampleRate);
        if (highCutOff > sampleRate / 2.0f) highCutOff = sampleRate / 2.0f;
        lowPass.setCornerFrequency(highCutOff);
        lowPass.setQ(0.5f);

    }
    highPassEnabled = lowCutOff != -1 && lowCutOff != MIN_FILTER_CUT_OFF;
    if (highPassEnabled) {
        highPass.initWithSamplingRate(sampleRate);
        if (lowCutOff > sampleRate / 2.0f) lowCutOff = sampleRate / 2.0f;
        highPass.setCornerFrequency(lowCutOff);
        highPass.setQ(0.5f);
    }
}

void processIncomingBytes(const unsigned char *inData, const int size, short *outSamples, int *outEventIndices,
                          std::string *outEventLabels, int *outCounts) {
    if (channelCountChanged) { // number of channels changed during processing of previous batch
        frameStarted = false;
        sampleStarted = false;
        currentChannel = 0;

        channelCountChanged = false;
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

    for (int i = 0; i < size; i++) {
        uc = inData[i];

        // and next byte to custom message sent by SpikerBox
        escapeSequence[escapeSequenceIndex++] = uc;

        if (insideEscapeSequence) { // we are inside escape sequence
            sampleIndex = sampleCounters[currentChannel] == 0 ? 0 : sampleCounters[currentChannel] - 1;
            if (eventMessageIndex >= EVENT_MESSAGE_LENGTH) { // event message shouldn't be longer then 64 bytes
                unsigned char *copy = new unsigned char[eventMessageIndex];
                std::copy(eventMessage, eventMessage + eventMessageIndex, copy);
                // let's process incoming message
                processEscapeSequenceMessage(copy, sampleIndex);

                delete[] copy;
                reset();
            } else if (ESCAPE_SEQUENCE_END[tmpIndex] == uc) {
                tmpIndex++;
                if (tmpIndex == ESCAPE_SEQUENCE_START_END_LENGTH) {
                    unsigned char *copy = new unsigned char[eventMessageIndex];
                    std::copy(eventMessage, eventMessage + eventMessageIndex, copy);
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


    // apply additional filtering if necessary
    if (lowPassEnabled) {
        lowPass.filter(channels[CHANNEL_INDEX], sampleCounters[CHANNEL_INDEX]);
    }
    if (highPassEnabled) {
        highPass.filter(channels[CHANNEL_INDEX], sampleCounters[CHANNEL_INDEX]);
    }

    std::copy(channels[CHANNEL_INDEX], channels[CHANNEL_INDEX] + sampleCounters[CHANNEL_INDEX], outSamples);
    std::copy(eventIndices, eventIndices + eventCounter, outEventIndices);
    std::copy(eventLabels, eventLabels + eventCounter, outEventLabels);
    outCounts[0] = sampleCounters[CHANNEL_INDEX];
    outCounts[1] = eventCounter;
}

// Resets all variables used for processing escape sequences
void reset() {
    insideEscapeSequence = false;
    tmpIndex = 0;
    escapeSequenceIndex = 0;
    eventMessageIndex = 0;
}

// Processes escape sequence message and triggers appropriate listener
void processEscapeSequenceMessage(unsigned char *messageBytes, int sampleIndex) {
    // check if it's board type message
    /*if (listener != null) {
        if (SampleStreamUtils.isHardwareTypeMsg(message)) {
            listener.onSpikerBoxHardwareTypeDetected(SampleStreamUtils.getBoardType(message));
        } else if (SampleStreamUtils.isSampleRateAndNumOfChannelsMsg(message)) {
            listener.onMaxSampleRateAndNumOfChannelsReply(SampleStreamUtils.getMaxSampleRate(message),
                                                          SampleStreamUtils.getChannelCount(message));
        } else */
    std::string message = reinterpret_cast<char *>(messageBytes);
    if (isEventMsg(message)) {
        eventIndices[eventCounter] = sampleIndex;
        eventLabels[eventCounter++] = getEventNumber(message);
    }
    /*}*/
}

const std::string eventPrefix = "EVNT:";

bool isEventMsg(std::string message) {
    return message.compare(0, eventPrefix.length(), eventPrefix) == 0;
}

std::string getEventNumber(std::string message) {
    message = message.replace(0, eventPrefix.length(), "");
    std::size_t found = message.find(";");
    if (found == std::string::npos) return message;
    return message.replace(found, message.length() - found, "");
}