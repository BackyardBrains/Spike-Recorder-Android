//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_DRAWINGUTILS_H
#define SPIKE_RECORDER_ANDROID_DRAWINGUTILS_H

#include <climits>

class DrawingUtils {
public:
    static void
    prepareForDrawing(short **outSamples, int *outSampleCount, int *outEventIndices, int &outEventCount,
                      short **inSamples, int channelCount, const int *inEventIndices, int inEventCount, int fromSample,
                      int toSample, int drawSurfaceWidth);

private:
    static void
    envelope(short **outSamples, int *outSampleCount, int *outEventIndices, int &outEventCount, short **inSamples,
             int channelCount, const int *inEventIndices, int inEventIndicesCount, int fromSample, int toSample,
             int drawSurfaceWidth);
};


#endif //SPIKE_RECORDER_ANDROID_DRAWINGUTILS_H
