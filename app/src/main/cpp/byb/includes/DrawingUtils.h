//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_DRAWINGUTILS_H
#define SPIKE_RECORDER_ANDROID_DRAWINGUTILS_H

#include <climits>

namespace backyardbrains {

    namespace utils {

        class DrawingUtils {
        public:
            static void
            prepareSignalForDrawing(short **outSamples, int *outSampleCount, int *outEventIndices, int &outEventCount,
                                    short **inSamples, int channelCount, const int *inEventIndices, int inEventCount,
                                    int fromSample, int toSample, int drawSurfaceWidth);

            static void
            prepareFftForDrawing(float *outVertices, short *outIndices, float *outColors, int &outVertexCount,
                                 int &outIndexCount, int &outColorCount, float **fft, int windowCount, int windowSize,
                                 float width, float height);

        private:
            static void
            envelope(short **outSamples, int *outSampleCount, int *outEventIndices, int &outEventCount,
                     short **inSamples, int channelCount, const int *inEventIndices, int inEventIndicesCount,
                     int fromSample, int toSample, int drawSurfaceWidth);

            static float red(float gray);

            static float green(float gray);

            static float blue(float gray);

            static float base(float val);

            static float interpolate(float val, float y0, float x0, float y1, float x1);
        };
    }
}

#endif //SPIKE_RECORDER_ANDROID_DRAWINGUTILS_H
