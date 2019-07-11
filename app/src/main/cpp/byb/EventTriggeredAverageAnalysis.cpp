//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include <EventTriggeredAverageAnalysis.h>

namespace backyardbrains {

    namespace analysis {

        const char *EventTriggeredAverageAnalysis::TAG = "EventTriggeredAverageAnalysis";

        EventTriggeredAverageAnalysis::EventTriggeredAverageAnalysis() = default;

        EventTriggeredAverageAnalysis::~EventTriggeredAverageAnalysis() = default;

        long long EventTriggeredAverageAnalysis::currentTimeInMilliseconds() {
            struct timeval tv{};
            gettimeofday(&tv, nullptr);
            return ((tv.tv_sec * 1000) + (tv.tv_usec / 1000));
        }

        void EventTriggeredAverageAnalysis::process(const char *signalFilePath, const char *eventsFilePath,
                                                    const string *processedEvents, const int processedEventCount,
                                                    bool removeNoiseIntervals, float ***averages,
                                                    float ***normAverages) {
            long long milliseconds = currentTimeInMilliseconds();

            // open audio file we need to analyze
            drwav *wavPtr = drwav_open_file(signalFilePath);
            if (wavPtr == nullptr) {
                __android_log_print(ANDROID_LOG_DEBUG, TAG, "Unable to open file: %s", signalFilePath);
                return;
            }

            drwav_uint64 totalSamples = wavPtr->totalSampleCount;
            __android_log_print(ANDROID_LOG_DEBUG, TAG, "Audio file sample count is: %ld", totalSamples);
            drwav_uint32 sampleRate = wavPtr->sampleRate;
            __android_log_print(ANDROID_LOG_DEBUG, TAG, "Audio file sample rate is: %i", sampleRate);
            drwav_uint16 channelCount = wavPtr->channels;
            __android_log_print(ANDROID_LOG_DEBUG, TAG, "Audio file channel count is: %hu", channelCount);

            // check whether file is long enough for processing
            if (totalSamples < sampleRate * channelCount *
                               backyardbrains::utils::AnalysisUtils::MIN_VALID_FILE_LENGTH_IN_SECS) {
                __android_log_print(ANDROID_LOG_DEBUG, TAG, "File too short! Don't process!");
                return;
            }

            // calculate approx. number of events (we can have max 5 events/s)
            int maxEventCount = static_cast<const int>(totalSamples / (sampleRate * channelCount * 0.2F));
            auto *eventTimes = new float[maxEventCount];
            auto *eventNames = new std::string[maxEventCount];
            int eventCount = 0;
            // get events from the events file
            backyardbrains::utils::EventUtils::parseEvents(eventsFilePath, eventTimes, eventNames, eventCount);

            // holds standard deviation for the complete file
            double std = 0.0F;
            // holds mean for the complete file
            double mean = 0.0F;

            // if intervals that contain noise need to be removed we should find standard deviation of the complete file
            // because none of the samples in the processed batches should go above or below 2.5 STDs
            if (removeNoiseIntervals) {
                // determine sample batch size
                auto batchSize = static_cast<drwav_uint64>(ceil(
                        totalSamples / backyardbrains::utils::AnalysisUtils::BIN_COUNT));
                // max size of the currently processed sample batch
                auto maxBatchSize = static_cast<drwav_uint64>(ceil(
                        (sampleRate * backyardbrains::utils::AnalysisUtils::BUFFER_SIZE_IN_SECS) / channelCount));
                if (batchSize > maxBatchSize) batchSize = maxBatchSize;
                // holds batch of currently processed samples
                auto *samples = new drwav_int16[batchSize];

                // number of sample batches we'll have after going through the complete file
                int batchCount = static_cast<int>(ceil(static_cast<float>(totalSamples) / batchSize));

                // actual sample batch count
                int batchCounter = 0;
                // cast batch size to avoid multiple casting
                int sampleCount = static_cast<int>(batchSize);
                // number of read samples in single read loop
                drwav_uint64 read;
                // used for calculation of std and mean
                double squares = 0.0F;
                double number;

                // run through file and find standard deviation
                while ((read = drwav_read_s16(wavPtr, batchSize, samples)) > 0) {
                    if (batchSize != read) sampleCount = static_cast<int>(read);

                    auto *normalized = new float[sampleCount];
                    // divide all samples with SHRT_MAX so we get values between -1.0 and 1.0
                    backyardbrains::utils::SignalUtils::normalizeSignalToFloat(normalized, samples, sampleCount);

                    for (int i = 0; i < sampleCount; i++) {
                        number = normalized[i];
                        mean += number;
                        squares += number * number;
                    }

                    delete[] normalized;
                }

                // save standard deviation and mean for the complete file
                mean /= totalSamples;
                squares /= totalSamples;
                std = sqrt(squares - mean * mean);
                __android_log_print(ANDROID_LOG_DEBUG, TAG, "STD: %lf", std);
                __android_log_print(ANDROID_LOG_DEBUG, TAG, "MEAN: %lf", mean);
                std *= NOISE_DETECTION_STD_THRESHOLD; // multiply by 2.5 to set the threshold
                __android_log_print(ANDROID_LOG_DEBUG, TAG, "STD: %lf", std);

                // release resources
                delete[] samples;
            }

            // determine buffer size
            auto leftOffsetSampleCount = static_cast<drwav_uint64>(sampleRate * channelCount *
                                                                   EVENT_LEFT_OFFSET_IN_SECS);
            auto rightOffsetSampleCount = static_cast<drwav_uint64>(sampleRate * channelCount *
                                                                    EVENT_RIGHT_OFFSET_IN_SECS);
            // size of the currently processed sample batch
            drwav_uint64 bufferSize = leftOffsetSampleCount + rightOffsetSampleCount;
            // cast buffer size to avoid multiple casting
            int sampleCount = static_cast<int>(bufferSize);
            // number of frames in the currently processed sample batch
            int frameCount = sampleCount / channelCount;

            // holds the sums of samples by channels and processed events from all batches by their respectful index
            auto ***sums = new float **[channelCount];
            for (int i = 0; i < channelCount; i++) {
                sums[i] = new float *[processedEventCount];
                for (int j = 0; j < processedEventCount; j++) {
                    sums[i][j] = new float[frameCount]{0};
                }
            }

            // holds batch of currently processed samples
            auto *samples = new drwav_int16[bufferSize];
            // holds deinterleaved samples of the currently processed batch
            auto **deinterleavedSamples = new drwav_int16 *[channelCount];
            for (int i = 0; i < channelCount; i++) {
                deinterleavedSamples[i] = new drwav_int16[frameCount]{0};
            }
            // holds event counts per processed event
            auto *eventCounts = new int[processedEventCount]{0};
            auto *removeCounts = new int[processedEventCount]{0};

            drwav_uint64 sampleIndex;
            drwav_uint64 start;
            drwav_uint64 end;
            int processedEventIndex = 0;
            // loop through all the events and collect sample batches surrounding those events
            for (int i = 0; i < eventCount; i++) {
                bool shouldProcess = false;
                for (int j = 0; j < processedEventCount; j++) {
                    if (processedEvents[j] == eventNames[i]) { // current event should be processed
                        shouldProcess = true;
                        processedEventIndex = j;
                    }
                }
                if (!shouldProcess) continue; // if we don't have to process current event move to next

                sampleIndex = static_cast<drwav_uint64>(eventTimes[i] * sampleRate * channelCount);
                start = sampleIndex - leftOffsetSampleCount;
                end = sampleIndex + rightOffsetSampleCount;

                // validation
                if (sampleIndex < leftOffsetSampleCount) {
                    __android_log_print(ANDROID_LOG_DEBUG, TAG, "SKIPPING %d BATCH FOR %s ",
                                        eventCounts[processedEventIndex], processedEvents[i].c_str());
                    continue; // discard event if we can't fill batch on the left
                }
                if (end > totalSamples) {
                    __android_log_print(ANDROID_LOG_DEBUG, TAG, "SKIPPING %d BATCH FOR %s ",
                                        eventCounts[processedEventIndex], processedEvents[i].c_str());
                    continue; // discard event if we can't fill batch on the right
                }

                // number of read samples in single read loop
                drwav_uint64 read;
                if (drwav_seek_to_sample(wavPtr, start)) { // seek to the batch start
                    read = drwav_read_s16(wavPtr, bufferSize, samples); // read the whole batch
                    if (read == bufferSize) {
                        // check for noise and skip this batch if necessary
                        if (removeNoiseIntervals) {
                            auto *normalized = new float[sampleCount]{0};
                            backyardbrains::utils::SignalUtils::normalizeSignalToFloat(normalized, samples,
                                                                                       sampleCount);
                            for (int j = 0; j < sampleCount; j++) {
                                if (normalized[j] < mean - std || normalized[j] > mean + std) {
                                    removeCounts[processedEventIndex]++;
                                    goto cnt;
                                }
                            }
                            delete[] normalized;
                        }

                        // we don't skip this batch add it to the sums
                        backyardbrains::utils::SignalUtils::deinterleaveSignal(deinterleavedSamples, samples,
                                                                               sampleCount, channelCount);
                        for (int j = 0; j < channelCount; j++) {
                            auto *normalized = new float[frameCount]{0};
                            backyardbrains::utils::SignalUtils::normalizeSignalToFloat(normalized,
                                                                                       deinterleavedSamples[j],
                                                                                       frameCount);
                            for (int k = 0; k < frameCount; k++) {
                                sums[j][processedEventIndex][k] += normalized[k];
                            }
                            delete[] normalized;
                        }
                    }
                } else continue;

                eventCounts[processedEventIndex]++;

                cnt:;
            }

            for (int i = 0; i < processedEventCount; i++) {
                __android_log_print(ANDROID_LOG_DEBUG, TAG, "REMOVED %d BATCHES FOR %s ", removeCounts[i],
                                    processedEvents[i].c_str());
            }

            float min = FLT_MAX;
            float max = FLT_MIN;
            float divider;
            for (int i = 0; i < channelCount; i++) {
                for (int j = 0; j < processedEventCount; j++) {
                    if (eventCounts[i] > 0) {
                        divider = (float) eventCounts[j];
                        for (int k = 0; k < frameCount; k++) {
                            averages[i][j][k] = sums[i][j][k] / divider;
                            if (averages[i][j][k] > max) max = averages[i][j][k];
                            if (averages[i][j][k] < min) min = averages[i][j][k];
                        }
                    }
                }
            }

            // map all averages between -1 and 1
            for (int i = 0; i < channelCount; i++) {
                for (int j = 0; j < processedEventCount; j++) {
                    backyardbrains::utils::AnalysisUtils::map(averages[i][j], normAverages[i][j], frameCount, min, max,
                                                              -1.0F, 1.0F);
                }
            }

            delete[] eventTimes;
            delete[] eventNames;
            for (int i = 0; i < channelCount; i++) {
                for (int j = 0; j < processedEventCount; j++) {
                    delete[] sums[i][j];
                }
                delete[] sums[i];
            }
            delete[] sums;
            delete[] samples;
            for (int i = 0; i < channelCount; i++) {
                delete[] deinterleavedSamples;
            }
            delete[] deinterleavedSamples;
            delete[] eventCounts;
            delete[] removeCounts;
        }
    }
}