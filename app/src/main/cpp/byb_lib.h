//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_BYB_LIB_H
#define SPIKE_RECORDER_ANDROID_BYB_LIB_H

int envelope(short *output, const short *samples, int start, int end, int size);

int prepareForDrawing(short *output, const short *samples, int fromSample, int toSample, int size);

#endif //SPIKE_RECORDER_ANDROID_BYB_LIB_H
