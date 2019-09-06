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
                                                    const bool removeNoiseIntervals,
                                                    const char *confidenceIntervalsEvent, float ***averages,
                                                    float ***normAverages, float **normMcAverages, float **normMcTop,
                                                    float **normMcBottom, float *min, float *max) {
            long long milliseconds = currentTimeInMilliseconds();

            // we need to process at least one event
            if (processedEventCount < 1) {
                __android_log_print(ANDROID_LOG_DEBUG, TAG, "There needs to be at least one event for processing");
                return;
            }

            // open audio file we need to analyze
            drwav *wavPtr = drwav_open_file(signalFilePath);
            if (wavPtr == nullptr) {
                __android_log_print(ANDROID_LOG_DEBUG, TAG, "Unable to open file: %s", signalFilePath);
                return;
            }

            totalSamples = wavPtr->totalSampleCount;
            __android_log_print(ANDROID_LOG_DEBUG, TAG, "Audio file sample count is: %ld", totalSamples);
            sampleRate = wavPtr->sampleRate;
            __android_log_print(ANDROID_LOG_DEBUG, TAG, "Audio file sample rate is: %i", sampleRate);
            channelCount = wavPtr->channels;
            __android_log_print(ANDROID_LOG_DEBUG, TAG, "Audio file channel count is: %hu", channelCount);

            // check whether file is long enough for processing
            if (totalSamples < sampleRate * channelCount *
                               backyardbrains::utils::AnalysisUtils::MIN_VALID_FILE_LENGTH_IN_SECS) {
                __android_log_print(ANDROID_LOG_DEBUG, TAG, "File too short! Don't process!");
                return;
            }

            // determine buffer size
            leftOffsetSampleCount = static_cast<drwav_uint64>(sampleRate * channelCount * EVENT_LEFT_OFFSET_IN_SECS);
            rightOffsetSampleCount = static_cast<drwav_uint64>(sampleRate * channelCount * EVENT_RIGHT_OFFSET_IN_SECS);
            bufferSize = leftOffsetSampleCount + rightOffsetSampleCount;
            sampleCount = static_cast<int>(bufferSize);
            frameCount = sampleCount / channelCount;

            // 1. FIND STD AND MEAN OF FULL SIGNAL IF WE NEED TO REMOVE INTERVALS THAT CONTAIN NOISE

            // holds standard deviation for the complete file
            double std = 0.0F;
            // holds mean for the complete file
            double mean = 0.0F;
            // holds index of first interval sample
            drwav_uint64 start;
            // index of the currently processed event
            int processedEventIndex = 0;
            // min and max values of the computed intervals that will be used
            // for normalization after all computations by channels
            for (int i = 0; i < channelCount; i++) {
                min[i] = FLT_MAX;
                max[i] = FLT_MIN;
            }

            // if intervals that contain noise need to be removed we should find standard deviation and mean
            // of the complete file because none of the samples in the processed batches should go above or below
            // mean + 2.5 STDs
            if (removeNoiseIntervals) findStdAndMean(wavPtr, std, mean);

            // 2. FIND ALL INTERVALS SURROUNDING THE EVENTS NEEDED TO BE PROCESSED
            // AND SUM THEM BY INDEX SO WE CAN AVERAGE LATER

            // holds the sums of samples by processed events and channels from all intervals by their respectful index
            auto ***sums = new float **[processedEventCount];
            for (int i = 0; i < processedEventCount; i++) {
                sums[i] = new float *[channelCount];
                for (int j = 0; j < channelCount; j++) {
                    sums[i][j] = new float[frameCount]{0};
                }
            }
            // holds event counts per processed event
            auto *eventCounts = new int[processedEventCount]{0};

            // calculate approx. number of events (we can have max 5 events/s)
            int maxEventCount = static_cast<const int>(totalSamples / (sampleRate * channelCount * 0.2F));
            auto *eventTimes = new float[maxEventCount];
            auto *eventNames = new std::string[maxEventCount];
            int eventCount = 0;
            // get events from the events file
            backyardbrains::utils::EventUtils::parseEvents(eventsFilePath, eventTimes, eventNames, eventCount);

            // loop through all the events and collect sample intervals surrounding those events
            for (int i = 0; i < eventCount; i++) {
                bool shouldProcess = false;
                for (int j = 0; j < processedEventCount; j++) {
                    if (processedEvents[j] == eventNames[i]) { // current event should be processed
                        shouldProcess = true;
                        processedEventIndex = j;
                    }
                }
                if (!shouldProcess) continue; // if we don't have to process current event move to next

                auto sampleIndex = static_cast<drwav_uint64>(eventTimes[i] * sampleRate * channelCount);
                // validate whether we can construct the interval for the sample with specified sampleIndex
                bool isValid = validateIntervalBounds(start, sampleIndex);
                if (!isValid) continue;

                bool added = addInterval(wavPtr, sums[processedEventIndex], start, bufferSize, removeNoiseIntervals,
                                         std, mean);

                if (added) eventCounts[processedEventIndex]++;
            }

            // 3. AVERAGE THE SUMS

            // calculate means for every processed event
            for (int i = 0; i < processedEventCount; i++) {
                averageInterval(averages[i], sums[i], eventCounts[i], min, max);
            }

            // 4. COMPUTE CONFIDENCE INTERVALS IF NECESSARY

            // holds the sums of samples by processed events and channels from all intervals by their respectful index
            auto ***confidenceIntervalsAverages = new float **[CONFIDENCE_INTERVALS_COMPUTATION_REPETITION];
            for (int i = 0; i < CONFIDENCE_INTERVALS_COMPUTATION_REPETITION; i++) {
                confidenceIntervalsAverages[i] = new float *[channelCount];
                for (int j = 0; j < channelCount; j++) {
                    confidenceIntervalsAverages[i][j] = new float[frameCount]{0};
                }
            }
            if (confidenceIntervalsEvent != nullptr) {
                processedEventIndex = -1;
                for (int i = 0; i < processedEventCount; i++) {
                    // find the index of the event we need to compute the confidence intervals for
                    if (processedEvents[i] == confidenceIntervalsEvent) {
                        processedEventIndex = i;
                        break;
                    }
                }
                if (processedEventIndex > 0 && eventCounts[processedEventIndex] > 0) {
                    // fields responsible for selecting a random sample
                    std::random_device rd;
                    std::mt19937 mt(rd());
                    std::uniform_int_distribution<drwav_uint64> dist(0, totalSamples);
                    // counts intervals of averages
                    int counter;
                    // total number of needed average intervals per computation repetition
                    int total = eventCounts[processedEventIndex];
                    // counts repetitions
                    int avgRepetitionCounter = 0;
                    float *mcMin = new float[2];
                    float *mcMax = new float[2];
                    while (avgRepetitionCounter < CONFIDENCE_INTERVALS_COMPUTATION_REPETITION) {
                        counter = 0;
                        for (int i = 0; i < channelCount; i++) {
                            std::fill(sums[0][i], sums[0][i] + frameCount, 0);
                        }
                        while (counter < total) {
                            auto sampleIndex = static_cast<drwav_uint64>(dist(mt));

                            // validate whether we can construct the interval for the sample with specified sampleIndex
                            bool isValid = validateIntervalBounds(start, sampleIndex);
                            if (!isValid) continue;

                            bool added = addInterval(wavPtr, sums[0], start, bufferSize, removeNoiseIntervals, std,
                                                     mean);
                            if (added) counter++;
                        }

                        averageInterval(confidenceIntervalsAverages[avgRepetitionCounter], sums[0], total, mcMin,
                                        mcMax);

                        avgRepetitionCounter++;
                    }

                    // 5. COMPUTE STANDARD DEVIATIONS FOR ALL COLLECTED MONTE CARLO INTERVALS

                    auto *tmp = new float[CONFIDENCE_INTERVALS_COMPUTATION_REPETITION];
                    float tmpSum;
                    float tmpStd;
                    float tmpMean;
                    float tmpTop;
                    float tmpBottom;
                    for (int j = 0; j < channelCount; j++) {
                        for (int k = 0; k < frameCount; k++) {
                            tmpSum = 0;
                            for (int i = 0; i < CONFIDENCE_INTERVALS_COMPUTATION_REPETITION; i++) {
                                tmp[i] = confidenceIntervalsAverages[i][j][k];
                                tmpSum += tmp[i];
                            }

                            tmpMean = tmpSum / CONFIDENCE_INTERVALS_COMPUTATION_REPETITION;
                            tmpStd = backyardbrains::utils::AnalysisUtils::SD(tmp,
                                                                              CONFIDENCE_INTERVALS_COMPUTATION_REPETITION);
                            tmpTop = tmpMean + tmpStd * SIGNAL_STD_MULTIPLYER;
                            tmpBottom = tmpMean - tmpStd * SIGNAL_STD_MULTIPLYER;

                            normMcAverages[j][k] = tmpMean;
                            normMcTop[j][k] = tmpTop;
                            normMcBottom[j][k] = tmpBottom;

                            if (tmpBottom < min[j]) min[j] = tmpBottom;
                            if (tmpTop > max[j]) max[j] = tmpTop;
                        }
                        delete[] tmp;
                    }

                    // map all averages, top and bottom MC intervals between -1 and 1
                    for (int i = 0; i < channelCount; i++) {
                        backyardbrains::utils::AnalysisUtils::map(normMcAverages[i], normMcAverages[i],
                                                                  frameCount, min[i], max[i], -1.0F, 1.0F);
                        backyardbrains::utils::AnalysisUtils::map(normMcTop[i], normMcTop[i],
                                                                  frameCount, min[i], max[i], -1.0F, 1.0F);
                        backyardbrains::utils::AnalysisUtils::map(normMcBottom[i], normMcBottom[i],
                                                                  frameCount, min[i], max[i], -1.0F, 1.0F);
                    }
                }
            }

            // map all averages between -1 and 1
            for (int i = 0; i < processedEventCount; i++) {
                for (int j = 0; j < channelCount; j++) {
                    backyardbrains::utils::AnalysisUtils::map(averages[i][j], normAverages[i][j], frameCount, min[j],
                                                              max[j], -1.0F, 1.0F);
                }
            }

            delete[] eventTimes;
            delete[] eventNames;
            for (int i = 0; i < processedEventCount; i++) {
                for (int j = 0; j < channelCount; j++) {
                    delete[] sums[i][j];
                }
                delete[] sums[i];
            }
            delete[] sums;
            delete[] eventCounts;
            for (int i = 0; i < CONFIDENCE_INTERVALS_COMPUTATION_REPETITION; i++) {
                for (int j = 0; j < channelCount; j++) {
                    delete[] confidenceIntervalsAverages[i][j];
                }
                delete[] confidenceIntervalsAverages[i];
            }
            delete[] confidenceIntervalsAverages;
        }

        bool EventTriggeredAverageAnalysis::validateIntervalBounds(drwav_uint64 &start, drwav_uint64 sampleIndex) {
            start = sampleIndex - leftOffsetSampleCount;
            drwav_uint64 end = sampleIndex + rightOffsetSampleCount;

            // validation
            if (sampleIndex < leftOffsetSampleCount)
                return false; // discard event if we can't fill batch on the left
            return end <= totalSamples; // discard event if we can't fill batch on the right
        }

        bool EventTriggeredAverageAnalysis::addInterval(drwav *wavPtr, float **sums, drwav_uint64 start,
                                                        drwav_uint64 size, bool checkNoise, double std,
                                                        double mean) {
            const drwav_uint16 channelCount = wavPtr->channels;
            int sampleCount = static_cast<int>(size);
            int frameCount = sampleCount / channelCount;

            // holds batch of currently processed samples
            auto *samples = new drwav_int16[size];
            // holds deinterleaved samples of the currently processed batch
            auto **deinterleavedSamples = new drwav_int16 *[channelCount];
            for (int i = 0; i < channelCount; i++) {
                deinterleavedSamples[i] = new drwav_int16[frameCount]{0};
            }

            bool added = true;

            // number of read samples in single read loop
            drwav_uint64 read;
            int i;
            int j;
            if (drwav_seek_to_sample(wavPtr, start)) { // seek to the batch start
                read = drwav_read_s16(wavPtr, size, samples); // read the whole batch
                if (read == size) {
                    // check for noise and skip this batch if necessary
                    if (checkNoise) {
                        auto *normalized = new float[sampleCount]{0};
                        backyardbrains::utils::SignalUtils::normalizeSignalToFloat(normalized, samples,
                                                                                   sampleCount);
                        for (i = 0; i < sampleCount; i++) {
                            if (normalized[i] < mean - std * NOISE_DETECTION_STD_THRESHOLD ||
                                normalized[i] > mean + std * NOISE_DETECTION_STD_THRESHOLD) {
                                added = false;
                            }
                        }
                        delete[] normalized;
                    }

                    if (added) {
                        // we didn't skip this batch add it to the sums
                        backyardbrains::utils::SignalUtils::deinterleaveSignal(deinterleavedSamples, samples,
                                                                               sampleCount,
                                                                               channelCount);
                        for (i = 0; i < channelCount; i++) {
                            auto *normalized = new float[frameCount]{0};
                            backyardbrains::utils::SignalUtils::normalizeSignalToFloat(normalized,
                                                                                       deinterleavedSamples[i],
                                                                                       frameCount);
                            for (j = 0; j < frameCount; j++) {
                                sums[i][j] += normalized[j];
                            }
                            delete[] normalized;
                        }
                    }
                } else added = false;
            } else added = false;

            delete[] samples;
            for (i = 0; i < channelCount; i++) {
                delete[] deinterleavedSamples[i];
            }
            delete[] deinterleavedSamples;

            return added;
        }

        void EventTriggeredAverageAnalysis::averageInterval(float **averages, float **sums, int divider, float *min,
                                                            float *max) {
            for (int i = 0; i < channelCount; i++) {
                if (divider > 0) {
                    for (int j = 0; j < frameCount; j++) {
                        averages[i][j] = sums[i][j] / divider;
                        if (averages[i][j] > max[i]) max[i] = averages[i][j];
                        if (averages[i][j] < min[i]) min[i] = averages[i][j];
                    }
                }
            }
        }

        void
        EventTriggeredAverageAnalysis::findStdAndMean(drwav *wavPtr, double &std, double &mean) {
            // determine sample batch size
            auto batchSize = static_cast<drwav_uint64>(ceil(
                    totalSamples / backyardbrains::utils::AnalysisUtils::BIN_COUNT));
            // max size of the currently processed sample batch
            auto maxBatchSize = static_cast<drwav_uint64>(ceil(
                    (sampleRate * backyardbrains::utils::AnalysisUtils::BUFFER_SIZE_IN_SECS) / channelCount));
            if (batchSize > maxBatchSize) batchSize = maxBatchSize;
            // holds batch of currently processed samples
            auto *samples = new drwav_int16[batchSize];
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

            // release resources
            delete[] samples;
        }
    }
}