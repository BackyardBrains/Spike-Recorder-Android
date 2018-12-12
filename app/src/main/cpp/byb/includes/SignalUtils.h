//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_SIGNALUTILS_H
#define SPIKE_RECORDER_ANDROID_SIGNALUTILS_H

namespace util {
    class SignalUtils;
}

class SignalUtils {
public:
    static short **deinterleaveSignal(const short *samples, int sampleCount, int channelCount);

    static short *interleaveSignal(short **samples, int frameCount, int channelCount);
};


#endif //SPIKE_RECORDER_ANDROID_SIGNALUTILS_H
