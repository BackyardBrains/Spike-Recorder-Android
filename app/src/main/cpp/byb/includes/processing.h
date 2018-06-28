//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_PROCESSING_H
#define SPIKE_RECORDER_ANDROID_PROCESSING_H

#include <string>
#include <android/log.h>

using byte = unsigned int;

static const char *TAG = "processing.cpp";

static const int CLEANER = 0xFF;
static const int REMOVER = 0x7F;

// For now we only process first channel
static const int CHANNEL_INDEX = 0;
// Length of escape sequence start and sequence end
static const int ESCAPE_SEQUENCE_START_END_LENGTH = 6;
// Message cannot be longer than 64 bytes
static const int EVENT_MESSAGE_LENGTH = 64;
// Escape sequence cannot be longer than sequence start + 64 + sequence end
static const int MAX_SEQUENCE_LENGTH =
        ESCAPE_SEQUENCE_START_END_LENGTH + EVENT_MESSAGE_LENGTH + ESCAPE_SEQUENCE_START_END_LENGTH;
// By default we have 2 channels
static const int DEFAULT_CHANNEL_COUNT = 2;
// Max number of channels is 10
static const int MAX_CHANNELS = 10;
// Max number of bytes we can process in one batch
static const int MAX_BYTES = 5000;
// We can maximally handle 6 seconds of sample data and spike can appear max every 200 ms
static const int MAX_EVENTS = 600;
// Minimum cut-off frequency
static const float MIN_FILTER_CUT_OFF = 0.0f;
// Maximum cut-off frequency
static const float MAX_FILTER_CUT_OFF = 5000.0f;

// Array of bytes which represent start of an escape sequence
const unsigned char ESCAPE_SEQUENCE_START[ESCAPE_SEQUENCE_START_END_LENGTH] = {0xFF, 0xFF, 0x01, 0x01, 0x80, 0xFF};
// Array of bytes which represent end of an escape sequence
const unsigned char ESCAPE_SEQUENCE_END[ESCAPE_SEQUENCE_START_END_LENGTH] = {0xFF, 0xFF, 0x01, 0x01, 0x81, 0xFF};

void setSampleRate(int sampleRate);

void setFilters(float lowCutOff, float highCutOff);

void processIncomingBytes(const unsigned char *inData, const int size, short *outSamples, int *outEventIndices,
                          std::string *outEventLabels, int *outCounts);

void reset();

void processEscapeSequenceMessage(unsigned char *messageBytes, int sampleIndex);

bool isEventMsg(std::string message);

std::string getEventNumber(std::string);

#endif //SPIKE_RECORDER_ANDROID_PROCESSING_H
