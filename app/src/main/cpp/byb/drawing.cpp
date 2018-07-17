//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include <climits>

#include "includes/drawing.h"

int *envelope(short *outSamples, const short *samples, int *outEventIndices, const int *eventIndices,
              int eventIndicesCount, int fromSample, int toSample, int size) {
    int drawSamplesCount = toSample - fromSample;
    if (drawSamplesCount < size) size = drawSamplesCount;

    short sample;
    short min = SHRT_MAX, max = SHRT_MIN;
    int samplesPerPixel = drawSamplesCount / size;
    int samplesPerPixelRest = drawSamplesCount % size;
    int samplesPerEnvelope = samplesPerPixel * 2; // multiply by 2 because we save min and max
    int envelopeCounter = 0, sampleIndex = 0, eventIndex = 0;
    bool eventExists = false;

    int from = fromSample;
    int to = fromSample + drawSamplesCount;
    for (int i = from; i < to; i++) {
        sample = samples[i];
        for (int j = 0; j < eventIndicesCount; j++) {
            if (i == eventIndices[j]) {
                eventExists = true;
                break;
            }
        }

        if (samplesPerPixel == 1 && samplesPerPixelRest == 0) {
            if (eventExists) outEventIndices[eventIndex++] = sampleIndex;
            outSamples[sampleIndex++] = sample;

            eventExists = false;
        } else {
            if (sample > max) max = sample;
            if (sample < min) min = sample;
            if (envelopeCounter == samplesPerEnvelope) {
                if (eventExists) outEventIndices[eventIndex++] = sampleIndex;
                outSamples[sampleIndex++] = max;
                outSamples[sampleIndex++] = min;

                envelopeCounter = 0;
                min = SHRT_MAX;
                max = SHRT_MIN;
                eventExists = false;
            }

            envelopeCounter++;
        }
    }

    return new int[2]{sampleIndex, eventIndex};
}

/**
 *
 * @param output
 * @param samples
 * @param fromSample
 * @param toSample
 * @param size
 * @return
 */
int *prepareForDrawing(short *outSamples, const short *samples, int *outEventIndices, const int *eventIndices,
                       int eventIndicesCount, int fromSample, int toSample, int size) {
    short *envelopedSamples = new short[size * 5];
    int *returned = envelope(envelopedSamples, samples, outEventIndices, eventIndices, eventIndicesCount,
                             fromSample, toSample,
                             size);

    int sampleIndex = 0, sampleCount = returned[0];
    short x = 0;
    for (int i = 0; i < sampleCount; i++) {
        outSamples[sampleIndex++] = x++;
        outSamples[sampleIndex++] = envelopedSamples[i];
    }

    delete[] envelopedSamples;
    delete[] returned;

    return new int[2]{sampleIndex, returned[1]};
}