//
// Created by Stanislav Mircic  <stanislav at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_LOWPASSFILTER_H
#define SPIKE_RECORDER_ANDROID_LOWPASSFILTER_H

#include "FilterBase.h"

class LowPassFilter : public FilterBase {
public:
    LowPassFilter();

    void calculateCoefficients();

    void setCornerFrequency(float newCornerFrequency);

    void setQ(float newQ);

protected:
    float cornerFrequency;
    float Q;
private:
};


#endif //SPIKE_RECORDER_ANDROID_LOWPASSFILTER_H
