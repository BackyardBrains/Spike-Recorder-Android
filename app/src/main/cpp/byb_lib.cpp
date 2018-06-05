//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include <climits>
#include <jni.h>

#include "byb_lib.h"

/**
 *
 * @param output
 * @param samples
 * @param fromSample
 * @param toSample
 * @param size
 * @return
 */
int envelope(short *output, const short *samples, int fromSample, int toSample, int size) {
    int drawSamplesCount = toSample - fromSample;
    if (drawSamplesCount < size) size = drawSamplesCount;

    short sample;
    short min = SHRT_MAX, max = SHRT_MIN;
    int samplesPerPixel = drawSamplesCount / size;
    int samplesPerPixelRest = drawSamplesCount % size;
    int samplesPerEnvelope = samplesPerPixel * 2; // multiply by 2 because we save min and max
    int envelopeCounter = 0, index = 0;

    int from = fromSample;
    int to = fromSample + drawSamplesCount;
    for (int i = from; i < to; i++) {
        sample = samples[i];

        if (samplesPerPixel == 1 && samplesPerPixelRest == 0) {
            output[index++] = sample;
        } else {
            if (sample > max) max = sample;
            if (sample < min) min = sample;
            if (envelopeCounter == samplesPerEnvelope) {
                output[index++] = max;
                output[index++] = min;

                envelopeCounter = 0;
                min = SHRT_MAX;
                max = SHRT_MIN;
            }

            envelopeCounter++;
        }
    }

    return index;
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
int prepareForDrawing(short *output, const short *samples, int fromSample, int toSample, int size) {
    short *envelopedSamples = new short[size * 5];
    int returned = envelope(envelopedSamples, samples, fromSample, toSample, size);

    int index = 0;
    short x = 0;
    for (int i = 0; i < returned; i++) {
        output[index++] = x++;
        output[index++] = envelopedSamples[i];
    }
    delete[] envelopedSamples;

    return index;
}