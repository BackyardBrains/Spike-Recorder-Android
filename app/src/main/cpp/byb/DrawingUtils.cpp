//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include "DrawingUtils.h"

namespace backyardbrains {

    namespace utils {

        void DrawingUtils::prepareSignalForDrawing(short **outSamples, int *outSampleCount, int *outEventIndices,
                                                   int &outEventCount, short **inSamples, int channelCount,
                                                   const int *inEventIndices, int inEventCount, int fromSample,
                                                   int toSample, int drawSurfaceWidth) {
            auto **envelopedSamples = new short *[channelCount];
            for (int i = 0; i < channelCount; i++) {
                envelopedSamples[i] = new short[drawSurfaceWidth * 5];
            }
            envelope(envelopedSamples, outSampleCount, outEventIndices, outEventCount, inSamples, channelCount,
                     inEventIndices, inEventCount, fromSample, toSample, drawSurfaceWidth);

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

        void
        DrawingUtils::prepareFftForDrawing(float *outVertices, short *outIndices, float *outColors, int &outVertexCount,
                                           int &outIndexCount, int &outColorCount, float **fft, const int windowCount,
                                           const int windowSize, const float width, const float height) {
            int widthSegments = windowCount - 1;
            int heightSegments = windowSize - 1;

            outVertexCount = (widthSegments + 1) * (heightSegments + 1) * 2;
            outIndexCount = (widthSegments + 1) * (heightSegments + 1) * 6;
            outColorCount = (widthSegments + 1) * (heightSegments + 1) * 4;

            float xOffset = 0;
            float yOffset = 0;
            float xWidth = width / widthSegments;
            float yHeight = height / heightSegments;
            int currentVertex = 0;
            int currentIndex = 0;
            int currentColor = 0;
            auto w = (short) (widthSegments + 1);
            for (int y = 0; y < heightSegments + 1; y++) {
                for (int x = 0; x < widthSegments + 1; x++) {
                    outVertices[currentVertex] = xOffset + x * xWidth;
                    outVertices[currentVertex + 1] = yOffset + y * yHeight;
                    currentVertex += 2;

                    int n = y * (widthSegments + 1) + x;

                    if (y < heightSegments && x < widthSegments) {
                        // Face one
                        outIndices[currentIndex] = (short) n;
                        outIndices[currentIndex + 1] = (short) (n + 1);
                        outIndices[currentIndex + 2] = (short) (n + w);
                        // Face two
                        outIndices[currentIndex + 3] = (short) (n + 1);
                        outIndices[currentIndex + 4] = (short) (n + 1 + w);
                        outIndices[currentIndex + 5] = (short) (n + 1 + w - 1);

                        currentIndex += 6;
                    }

                    float gray = fft[x][y];
                    outColors[currentColor] = red(gray);
                    outColors[currentColor + 1] = green(gray);
                    outColors[currentColor + 2] = blue(gray);
                    outColors[currentColor + 3] = 1.0f;

                    currentColor += 4;
                }
            }
        }

        void DrawingUtils::envelope(short **outSamples, int *outSampleCount, int *outEventIndices, int &outEventCount,
                                    short **inSamples, int channelCount, const int *inEventIndices,
                                    int inEventIndicesCount, int fromSample, int toSample, int drawSurfaceWidth) {
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
                min = SHRT_MAX;
                max = SHRT_MIN;
            }
        }

        float DrawingUtils::red(float gray) {
            return base(gray - .5f);
        }

        float DrawingUtils::green(float gray) {
            return base(gray);
        }

        float DrawingUtils::blue(float gray) {
            return base(gray + .5f);
        }

        float DrawingUtils::base(float val) {
            if (val <= -.75f) {
                return 0.0f;
            } else if (val <= -.25f) {
                return interpolate(val, 0.0f, -.75f, 1.0f, -.25f);
            } else if (val <= .25f) {
                return 1.0f;
            } else if (val <= .75f) {
                return interpolate(val, 1.0f, .25f, 0.0f, .75f);
            } else {
                return 0.0f;
            }
        }

        float DrawingUtils::interpolate(float val, float y0, float x0, float y1, float x1) {
            return (val - x0) * (y1 - y0) / (x1 - x0) + y0;
        }
    }
}