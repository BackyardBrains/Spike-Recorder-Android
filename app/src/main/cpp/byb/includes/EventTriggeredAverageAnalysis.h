//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_EVENTTRIGGEREDAVERAGEANALYSIS_H
#define SPIKE_RECORDER_ANDROID_EVENTTRIGGEREDAVERAGEANALYSIS_H

#include <functional>
#include <string>
#include <android/log.h>
#include <limits>

#include "dr_wav.h"
#include "AnalysisUtils.h"
#include "SignalUtils.h"
#include "EventUtils.h"

using namespace std;

namespace backyardbrains {

    namespace analysis {

        class EventTriggeredAverageAnalysis {
        public:
            EventTriggeredAverageAnalysis();

            ~EventTriggeredAverageAnalysis();

            void process(const char *signalFilePath, const char *eventsFilePath, const string *processedEvents,
                         const int processedEventCount, bool removeNoiseIntervals, float ***averages, float ***normAverages);

        private:
            static const char *TAG;

            static constexpr float EVENT_LEFT_OFFSET_IN_SECS = 0.7f;
            static constexpr float EVENT_RIGHT_OFFSET_IN_SECS = 0.7f;
            static constexpr float NOISE_DETECTION_STD_THRESHOLD = 2.5f;

            long long currentTimeInMilliseconds();
        };
    }
}

#endif //SPIKE_RECORDER_ANDROID_EVENTTRIGGEREDAVERAGEANALYSIS_H
