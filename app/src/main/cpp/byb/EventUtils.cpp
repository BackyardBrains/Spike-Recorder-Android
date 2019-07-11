//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include <EventUtils.h>

namespace backyardbrains {

    namespace utils {

        const char *EventUtils::TAG = "EventUtils";

        void
        EventUtils::parseEvents(const char *filePath, float *outEventTimes, string *outEventNames, int &outEventCount) {
            string line;
            string linePart;
            string prefix("#");
            ifstream eventsFile(filePath);
            vector<string> parts;

            if (eventsFile.is_open()) {
                while (eventsFile.good()) {
                    getline(eventsFile, line);
                    if (line.compare(0, prefix.size(), prefix) != 0) {
                        regex e(",\\s+");
                        regex_token_iterator<string::iterator> i(line.begin(), line.end(), e, -1);
                        regex_token_iterator<string::iterator> end;
                        while (i != end)
                            parts.push_back(*i++);
                        if (parts.size() == 2) {
                            outEventNames[outEventCount] = parts.at(0);
                            outEventTimes[outEventCount] = stof(parts.at(1));
                            outEventCount++;
                        }
                    }
                    parts.clear();
                }
                eventsFile.close();
            } else {
                __android_log_print(ANDROID_LOG_DEBUG, TAG, "Unable to open file: %s", filePath);
            }

            // cleanup
            string().swap(line);
            string().swap(linePart);
            string().swap(prefix);
            vector<string>().swap(parts);
        }

        void EventUtils::checkEvents(const char *filePath, string *outEventNames, int &outEventCount) {
            string line;
            string linePart;
            string prefix("#");
            ifstream eventsFile(filePath);
            vector<string> parts;

            if (eventsFile.is_open()) {
                while (eventsFile.good()) {
                    getline(eventsFile, line);
                    if (line.compare(0, prefix.size(), prefix) != 0) {
                        regex e(",\\s+");
                        regex_token_iterator<string::iterator> i(line.begin(), line.end(), e, -1);
                        regex_token_iterator<string::iterator> end;
                        while (i != end)
                            parts.push_back(*i++);
                        if (parts.size() == 2) {
                            bool exists = false;
                            for (int i = 0; i < outEventCount; i++) {
                                if (outEventNames[i] == parts.at(0)) {
                                    exists = true;
                                    break;
                                }
                            }
                            if (!exists) outEventNames[outEventCount++] = parts.at(0);
                        }
                    }
                    parts.clear();
                }
                eventsFile.close();
                // sort result array
                sort(outEventNames, outEventNames + outEventCount);
            } else {
                __android_log_print(ANDROID_LOG_DEBUG, TAG, "Unable to open file: %s", filePath);
            }

            // cleanup
            string().swap(line);
            string().swap(linePart);
            string().swap(prefix);
            vector<string>().swap(parts);
        }
    }
}