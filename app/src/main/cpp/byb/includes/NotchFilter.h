//
// Created by Stanislav Mircic  <stanislav at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_NOTCHFILTER_H
#define SPIKE_RECORDER_ANDROID_NOTCHFILTER_H

#include "FilterBase.h"

namespace backyardbrains {

    namespace filters {

        class NotchFilter : public FilterBase {
        public:
            NotchFilter();

            void calculateCoefficients();

            void setCenterFrequency(float newCenterFrequency);

            void setQ(float newQ);

        protected:
            float centerFrequency;
            float Q;
        private:
        };
    }
}

#endif //SPIKE_RECORDER_ANDROID_NOTCHFILTER_H
