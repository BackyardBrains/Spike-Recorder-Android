//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_EVENTUTILS_H
#define SPIKE_RECORDER_ANDROID_EVENTUTILS_H

#include <iostream>
#include <fstream>
#include <string>
#include <regex>
#include <vector>
#include <android/log.h>

using namespace std;

namespace backyardbrains {

    namespace utils {

        class EventUtils {
        public:
            static void
            parseEvents(const char *filePath, float *outEventTimes, string *outEventNames, int &outEventCount);

            static void checkEvents(const char *filePath, string *outEventNames, int &outEventCount);

        private:
            static const char *TAG;
        };
    }
}


#endif //SPIKE_RECORDER_ANDROID_EVENTUTILS_H
