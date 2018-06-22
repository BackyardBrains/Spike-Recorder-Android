//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_BYB_LIB_H
#define SPIKE_RECORDER_ANDROID_BYB_LIB_H

int *envelope(short *outSamples, const short *samples, int *outEventIndices, const int *eventIndices, int eventIndicesCount,
              int start, int end, int size);

int *
prepareForDrawing(short *outSamples, const short *samples, int *outEventIndices, const int *eventIndices,
                  int eventIndicesCount, int fromSample, int toSample, int size);

#endif //SPIKE_RECORDER_ANDROID_BYB_LIB_H
