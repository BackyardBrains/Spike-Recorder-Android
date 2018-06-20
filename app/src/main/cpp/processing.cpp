//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include <string>
#include <android/log.h>

#include "processing.h"

// Whether new frame is started being processed
bool frameStarted = false;
// Whether new sample is started being processed
bool sampleStarted = false;
// Holds currently processed channel
int currentChannel;
// Most significant and least significant bytes
byte msb;
// Number of channels
int channelCount = DEFAULT_CHANNEL_COUNT;
// Whether channel count has changed during processing of the latest chunk of incoming data
bool channelCountChanged = true;
// Average signal which we use to avoid signal offset
double average;
// Holds samples from all channels processed in a single batch
short channels[MAX_CHANNELS][MAX_BYTES];
// Array of sample counters, one for every channel
int sampleCounters[MAX_CHANNELS];
// Whether we are inside an escape sequence or not
bool insideEscapeSequence;
// Index of the byte within start or end of the escape sequence
int tmpIndex;
// Holds currently processed escape sequence
unsigned int escapeSequence[MAX_SEQUENCE_LENGTH];
// Index of the byte within currently processed escape sequence
int escapeSequenceIndex;
// Holds currently processed event message
byte eventMessage[EVENT_MESSAGE_LENGTH];
// Index of the byte within currently processed event message
int eventMessageIndex;
// Holds count of processed events in the current sample batch
int eventCounter;
// Holds event indices processed in a single batch
int eventIndices[MAX_EVENTS];
// Holds event labels processed in a single batch
std::string eventLabels[MAX_EVENTS];

void processIncomingData(const signed char *inData, const int size, short *outSamples, int *outEventIndices,
                         std::string *outEventLabels, int *outCounts) {
    if (channelCountChanged) {
        frameStarted = false;
        sampleStarted = false;
        currentChannel = 0;

//        channels = {};
//        sampleCounters = {};

        channelCountChanged = false;
    }

    int lsb;
    short sample;
    byte b;
    unsigned int byt;

    // max number of samples can be number of incoming bytes divided by 2
    int maxSampleCount = static_cast<int>(size * .5 + 1);
    // init samples (by channels)
    for (int i = 0; i < channelCount; i++) {
//        channels[i] = reinterpret_cast<short>(new short[maxSampleCount]);
        sampleCounters[i] = 0;
    }
//    channels = new short[maxSampleCount];
    // init events
    eventCounter = 0;
//    eventIndices = new int[MAX_EVENTS];
//    eventLabels = new std::string[MAX_EVENTS];

//    __android_log_print(ANDROID_LOG_ERROR, "processing.cpp", "=====================");
//    for (int i = 0; i < size; i++) {
//        __android_log_print(ANDROID_LOG_ERROR, "processing.cpp", "%d: %d", i, (unsigned int) inData[i]);
//    }
    for (int i = 0; i < size; i++) {
        byt = (unsigned int) inData[i];

        // and next byte to custom message sent by SpikerBox
        escapeSequence[escapeSequenceIndex++] = byt;

        if (insideEscapeSequence) { // we are inside escape sequence
            if (eventMessageIndex >= EVENT_MESSAGE_LENGTH) { // event message shouldn't be longer then 64 bytes
                byte *copy = new byte[eventMessageIndex];
                std::copy(eventMessage, eventMessage + eventMessageIndex, copy);
                // let's process incoming message
                processEscapeSequenceMessage(copy,
                                             sampleCounters[currentChannel] == 0 ? 0
                                                                                 : sampleCounters[currentChannel] -
                                                                                   1);
                delete[] copy;

                reset();
            } else if (ESCAPE_SEQUENCE_END[tmpIndex] == byt) {
                tmpIndex++;
                if (tmpIndex == ESCAPE_SEQUENCE_START_END_LENGTH) {
                    byte *copy = new byte[eventMessageIndex];
                    std::copy(eventMessage, eventMessage + eventMessageIndex, copy);
                    // let's process incoming message
                    processEscapeSequenceMessage(copy,
                                                 sampleCounters[currentChannel] == 0 ? 0 :
                                                 sampleCounters[currentChannel] -
                                                 1);
                    delete[] copy;

                    reset();
                }
            } else {
                eventMessage[eventMessageIndex++] = byt;
            }
        } else {
            if (ESCAPE_SEQUENCE_START[tmpIndex] == byt) {
                tmpIndex++;
                if (tmpIndex == ESCAPE_SEQUENCE_START_END_LENGTH) {
                    tmpIndex = 0; // reset index, we need it for sequence end
                    insideEscapeSequence = true;
                }
                continue;
            }

            byte *sequence = new byte[escapeSequenceIndex];
            std::copy(escapeSequence, escapeSequence + escapeSequenceIndex, sequence);
//            Arrays.copyOfRange(escapeSequence, 0, escapeSequenceIndex);
            for (int j = 0; j < escapeSequenceIndex; j++) {
                b = sequence[j];
                // check if we have unfinished frame
                if (frameStarted) {
                    // check if we have unfinished sample
                    if (sampleStarted) {
                        lsb = b & CLEANER;

                        // if less significant byte is also grater then 127 drop whole frame
                        if (lsb > 127) {
//                            __android_log_print(ANDROID_LOG_ERROR, "processing.cpp", "LSB > 127! DROP WHOLE FRAME!");
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

                        // apply additional filtering if necessary
//                        if (filters != null) sample = filters.apply(sample);

                        channels[currentChannel][sampleCounters[currentChannel]++] = sample;

                        sampleStarted = false;
                        if (currentChannel >= channelCount - 1) frameStarted = false;
                    } else {
                        msb = b & CLEANER;
                        // we already started the frame so if msb is greater then 127 drop whole frame
                        if (msb > 127) {
                            __android_log_print(ANDROID_LOG_ERROR, "processing.cpp",
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
                        __android_log_print(ANDROID_LOG_ERROR, "processing.cpp", "MSB < 128 AT FRAME START! DROP!");

                        frameStarted = false;
                        sampleStarted = false;
                        currentChannel = 0;
                    }
                }
            }

            reset();
        }
    }

//    if (sampleCounters[CHANNEL_INDEX] == 0) return new SamplesWithMarkers();

    //LOGD(TAG, "SIZE: " + data.length + ", TOOK: " + (System.currentTimeMillis() - start));

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
//    escapeSequence[MAX_SEQUENCE_LENGTH] = {};
    escapeSequenceIndex = 0;
//    eventMessage[EVENT_MESSAGE_LENGTH] = {};
    eventMessageIndex = 0;
}

// Processes escape sequence message and triggers appropriate listener
void processEscapeSequenceMessage(byte *messageBytes, int sampleIndex) {
    std::string message(reinterpret_cast< char const * >(messageBytes));
    // check if it's board type message
//    if (listener != null) {
//        if (SampleStreamUtils.isHardwareTypeMsg(message)) {
//            listener.onSpikerBoxHardwareTypeDetected(SampleStreamUtils.getBoardType(message));
//        } else if (SampleStreamUtils.isSampleRateAndNumOfChannelsMsg(message)) {
//            listener.onMaxSampleRateAndNumOfChannelsReply(SampleStreamUtils.getMaxSampleRate(message),
//                                                          SampleStreamUtils.getChannelCount(message));
//        } else if (SampleStreamUtils.isEventMsg(message)) {
//            eventIndices[eventCounter] = sampleIndex;
//            eventLabels[eventCounter++] = SampleStreamUtils.getEventNumber(message);
//        }
//    }
}