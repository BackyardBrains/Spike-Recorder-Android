//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include "DrawingUtils.h"

void DrawingUtils::prepareForDrawing(short **outSamples, int *outSampleCount, int *outEventIndices, int &outEventCount,
                                     short **inSamples, int channelCount, const int *inEventIndices,
                                     int inEventCount, int fromSample, int toSample, int drawSurfaceWidth) {
    short **envelopedSamples = new short *[channelCount];
    for (int i = 0; i < channelCount; i++) {
        envelopedSamples[i] = new short[drawSurfaceWidth * 5];
    }
    envelope(envelopedSamples, outSampleCount, outEventIndices, outEventCount, inSamples, channelCount, inEventIndices,
             inEventCount, fromSample, toSample, drawSurfaceWidth);

    int sampleIndex = 0;
    short x = 0;
    for (int i = 0; i < channelCount; i++) {
        for (int j = 0; j < outSampleCount[i]; j++) {
            outSamples[i][sampleIndex++] = x++;
            outSamples[i][sampleIndex++] = envelopedSamples[i][j];
        }
        outSampleCount[i] = sampleIndex;
        sampleIndex = 0;
        x = 0;
    }

    for (int i = 0; i < channelCount; i++) {
        delete[] envelopedSamples[i];
    }
    delete[] envelopedSamples;
}

void DrawingUtils::envelope(short **outSamples, int *outSampleCount, int *outEventIndices, int &outEventCount,
                            short **inSamples, int channelCount, const int *inEventIndices, int inEventIndicesCount,
                            int fromSample, int toSample, int drawSurfaceWidth) {
    int drawSamplesCount = toSample - fromSample;
    if (drawSamplesCount < drawSurfaceWidth) drawSurfaceWidth = drawSamplesCount;

    short sample;
    short min = SHRT_MAX, max = SHRT_MIN;
    int samplesPerPixel = drawSamplesCount / drawSurfaceWidth;
    int samplesPerPixelRest = drawSamplesCount % drawSurfaceWidth;
    int samplesPerEnvelope = samplesPerPixel * 2; // multiply by 2 because we save min and max
    int envelopeCounter = 0, sampleIndex = 0, eventIndex = 0;
    bool eventExists = false;
    bool eventsProcessed = false;

    int from = fromSample;
    int to = fromSample + drawSamplesCount;
    for (int i = 0; i < channelCount; i++) {
        for (int j = from; j < to; j++) {
            sample = inSamples[i][j];
            if (!eventsProcessed) {
                for (int k = 0; k < inEventIndicesCount; k++) {
                    if (j == inEventIndices[k]) {
                        eventExists = true;
                        break;
                    }
                }
            }

            if (samplesPerPixel == 1 && samplesPerPixelRest == 0) {
                if (eventExists) outEventIndices[eventIndex++] = sampleIndex;
                outSamples[i][sampleIndex++] = sample;

                eventExists = false;
            } else {
                if (sample > max) max = sample;
                if (sample < min) min = sample;
                if (envelopeCounter == samplesPerEnvelope) {
                    if (eventExists) outEventIndices[eventIndex++] = sampleIndex;
                    outSamples[i][sampleIndex++] = max;
                    outSamples[i][sampleIndex++] = min;

                    envelopeCounter = 0;
                    min = SHRT_MAX;
                    max = SHRT_MIN;
                    eventExists = false;
                }

                envelopeCounter++;
            }
        }

        outSampleCount[i] = sampleIndex;
        if (!eventsProcessed) outEventCount = eventIndex;

        eventsProcessed = true;
        sampleIndex = 0;
        eventIndex = 0;
        envelopeCounter = 0;
    }
}
