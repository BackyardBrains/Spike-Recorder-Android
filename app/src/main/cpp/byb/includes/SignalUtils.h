//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_SIGNALUTILS_H
#define SPIKE_RECORDER_ANDROID_SIGNALUTILS_H

class SignalUtils {
public:
    static short **deinterleaveSignal(short **outSamples, const short *inSamples, int sampleCount, int channelCount);

    static short *interleaveSignal(short **samples, int frameCount, int channelCount);
};


#endif //SPIKE_RECORDER_ANDROID_SIGNALUTILS_H
