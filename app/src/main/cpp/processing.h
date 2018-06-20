//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_PROCESSING_H
#define SPIKE_RECORDER_ANDROID_PROCESSING_H

using byte = unsigned int;

#define CLEANER 0xFF
#define REMOVER 0x7F

// For now we only process first channel
#define CHANNEL_INDEX 0
// Length of escape sequence start and end
#define ESCAPE_SEQUENCE_START_END_LENGTH 6
// Message cannot be longer than 64 bytes
#define EVENT_MESSAGE_LENGTH 64
// Escape sequence cannot be longer than sequence start + 64 + sequence end
#define MAX_SEQUENCE_LENGTH ESCAPE_SEQUENCE_START_END_LENGTH + EVENT_MESSAGE_LENGTH + ESCAPE_SEQUENCE_START_END_LENGTH
#define DEFAULT_CHANNEL_COUNT 2
#define MAX_CHANNELS 10
#define MAX_BYTES 5000
// We can maximally handle 6 seconds of sample data and spike can appear max every 200 ms
#define MAX_EVENTS 600

// Array of bytes which represent start of an escape sequence
const unsigned int ESCAPE_SEQUENCE_START[ESCAPE_SEQUENCE_START_END_LENGTH] = {255, 255, 1, 1, 128, 255};
// Array of bytes which represent end of an escape sequence
const unsigned int ESCAPE_SEQUENCE_END[ESCAPE_SEQUENCE_START_END_LENGTH] = {255, 255, 1, 1, 129, 255};

void processIncomingData(const signed char *inData, const int size, short *outSamples, int *outEventIndices,
                         std::string *outEventLabels, int *outCounts);

void reset();

void processEscapeSequenceMessage(byte *messageBytes, int sampleIndex);

#endif //SPIKE_RECORDER_ANDROID_PROCESSING_H
