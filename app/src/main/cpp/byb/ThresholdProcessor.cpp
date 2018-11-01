//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include "ThresholdProcessor.h"

const char *ThresholdProcessor::TAG = "ThresholdProcessor";

ThresholdProcessor::ThresholdProcessor(OnHeartbeatListener *listener) {
    heartbeatHelper = new HeartbeatHelper(getSampleRate(), listener);

    // we need to initialize initial trigger values because they depend on channel count
    triggerValue = new int[getChannelCount()];
    for (int i = 0; i < getChannelCount(); i++) {
        triggerValue[i] = INT_MAX;
    }
    lastTriggeredValue = new int[getChannelCount()]{0};

    init();
}

ThresholdProcessor::~ThresholdProcessor() {
}

int ThresholdProcessor::getSampleCount() {
    return sampleCount;
}

int ThresholdProcessor::getAveragedSampleCount() {
    return averagedSampleCount;
}

void ThresholdProcessor::setAveragedSampleCount(int averagedSampleCount) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "setAveragedSampleCount(%d)", averagedSampleCount);

    if (averagedSampleCount <= 0 || ThresholdProcessor::averagedSampleCount == averagedSampleCount) return;

    ThresholdProcessor::averagedSampleCount = averagedSampleCount;
}

void ThresholdProcessor::setSelectedChannel(int selectedChannel) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "setSelectedChannel(%d)", selectedChannel);

    ThresholdProcessor::selectedChannel = selectedChannel;
}

void ThresholdProcessor::setThreshold(int threshold) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "setThreshold(%d)", threshold);

    ThresholdProcessor::triggerValue[selectedChannel] = threshold;
}

void ThresholdProcessor::resetThreshold() {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "resetThreshold()");

    ThresholdProcessor::resetOnNextBatch = true;
}

void ThresholdProcessor::setPaused(bool paused) {
    if (ThresholdProcessor::paused == paused) return;

    ThresholdProcessor::paused = paused;
}

int ThresholdProcessor::getTriggerType() {
    return triggerType;
}

void ThresholdProcessor::setTriggerType(int triggerType) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "setTriggerType(%d)", triggerType);

    if (ThresholdProcessor::triggerType == triggerType) return;

    ThresholdProcessor::triggerType = triggerType;
}

void ThresholdProcessor::setBpmProcessing(bool processBpm) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "setBpmProcessing(%s)", processBpm ? "ON" : "OFF");

    if (ThresholdProcessor::processBpm == processBpm) return;

    // reset BPM if we stopped processing heartbeat
    if (!processBpm) resetBpm();

    ThresholdProcessor::processBpm = processBpm;
}

void
ThresholdProcessor::process(short **outSamples, short **inSamples, const int *inSampleCounts,
                            const int *inEventIndices, const int *inEvents, const int inEventCount) {
//    __android_log_print(ANDROID_LOG_DEBUG, TAG, "THRESHOLD STARTED WITH: %d", inSampleCounts[0]);
    if (paused) return;

    bool shouldReset = false;
    // reset buffers if selected channel has changed
    if (lastSelectedChannel != selectedChannel) {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "Resetting because channel has changed");
        lastSelectedChannel = selectedChannel;
        shouldReset = true;
    }
    // reset buffers if threshold changed
    if (lastTriggeredValue[selectedChannel] != triggerValue[selectedChannel]) {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "Resetting because trigger value has changed");
        lastTriggeredValue[selectedChannel] = triggerValue[selectedChannel];
        shouldReset = true;
    }
    // reset buffers if averages sample count changed
    if (lastAveragedSampleCount != averagedSampleCount) {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "Resetting because last averaged sample count has changed");
        lastAveragedSampleCount = averagedSampleCount;
        shouldReset = true;
    }
    // reset buffers if sample rate changed
    if (lastSampleRate != getSampleRate()) {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "Resetting because sample rate has changed");
        lastSampleRate = getSampleRate();
        shouldReset = true;
    }
    // let's save last channel count so we can use it to delete all the arrays
    int channelCount = getChannelCount();
    int tmpLastChannelCount = lastChannelCount;
    if (lastChannelCount != channelCount) {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "Resetting because channel count has changed");
        lastChannelCount = channelCount;
        shouldReset = true;
    }
    if (shouldReset || resetOnNextBatch) {
        clean(tmpLastChannelCount);
        init();
        resetOnNextBatch = false;
    }

    // append unfinished sample buffers with incoming samples
    int *prevCounts = new int[channelCount]{0};
    int from = INT_MAX, to = INT_MIN;
    int tmpInSampleCount;
    short *tmpInSamples;
    int samplesToCopy;
    short **tmpSamples;
    short **tmpUnfinishedSamples;
    short *tmpUnfinishedSamplesRow;
    int *tmpUnfinishedSamplesCounts;
    int *tmpUnfinishedSamplesAveragedCounts;
    short *tmpSamplesToSubtractFrom;
    int *tmpSummedSampleCounts;
    int *tmpSummedSamples;
    short *tmpAveragedSamples;
    int i, j, k;
    int kStart, kEnd;
    for (i = 0; i < channelCount; i++) {
        tmpInSampleCount = inSampleCounts[i];
        tmpInSamples = inSamples[i];
        tmpSamples = samplesForCalculation[i];
        tmpUnfinishedSamples = unfinishedSamplesForCalculation[i];
        tmpUnfinishedSamplesCounts = unfinishedSamplesForCalculationCounts[i];
        tmpUnfinishedSamplesAveragedCounts = unfinishedSamplesForCalculationAveragedCounts[i];
        tmpSummedSampleCounts = summedSamplesCounts[i];
        tmpSummedSamples = summedSamples[i];

        for (j = 0; j < unfinishedSamplesForCalculationCount[i]; j++) {
            tmpUnfinishedSamplesRow = tmpUnfinishedSamples[j];
            if (averagedSampleCount <= j) { // we look for the oldest one in the unfinished samples
                tmpSamplesToSubtractFrom = tmpUnfinishedSamples[j - averagedSampleCount];
            } else { // we look for the oldest one in the already collected and calculated samples
                tmpSamplesToSubtractFrom = tmpSamples[samplesForCalculationCount[i] - averagedSampleCount + j];
            }
            kStart = tmpUnfinishedSamplesCounts[j];

            // we just need to append enough to fill the unfinished rows till end (sampleCount)
            samplesToCopy = std::min(sampleCount - kStart, tmpInSampleCount);
            std::copy(tmpInSamples, tmpInSamples + samplesToCopy, tmpUnfinishedSamplesRow + kStart);

            kEnd = kStart + samplesToCopy;
            for (k = kStart; k < kEnd; k++) {
                // if we are calculating averagedSampleCount + 1. sample we should subtract the oldest one in the sum
                if (tmpSummedSampleCounts[k] == averagedSampleCount) {
                    // subtract the value and decrease summed samples count for current position
                    tmpSummedSamples[k] -= tmpSamplesToSubtractFrom[k];
                    tmpSummedSampleCounts[k]--;
                }
                // add new value and increase summed samples count for current position
                tmpSummedSamples[k] += tmpUnfinishedSamplesRow[k];
                tmpSummedSampleCounts[k]++;
            }
            tmpUnfinishedSamplesCounts[j] = kEnd;

            if (to < kEnd) to = kEnd;
        }


        // move filled unfinished sample rows to finished samples
        for (j = 0; j < unfinishedSamplesForCalculationCount[i]; j++) {
            tmpUnfinishedSamplesRow = tmpUnfinishedSamples[j];

            // we are only interested in completed unfinished sample rows
            if (tmpUnfinishedSamplesCounts[j] >= sampleCount) {
                // if there are more filled sample rows than averagedSampleCount remove them from calculations
                if (samplesForCalculationCount[i] >= averagedSampleCount) {
                    delete[] tmpSamples[0];
                    // shift rest of the filled sample rows to left
                    if (samplesForCalculationCount[i] > 1) {
                        std::move(tmpSamples + 1, tmpSamples + samplesForCalculationCount[i], tmpSamples);
                    }
                    samplesForCalculationCount[i]--;
                }
                // add filled unfinished sample row to finished samples
                tmpSamples[samplesForCalculationCount[i]++] = tmpUnfinishedSamplesRow;
                // shift rest of the unfinished sample rows and their appropriate counters to left
                if (unfinishedSamplesForCalculationCount[i] > j + 1) {
                    std::move(tmpUnfinishedSamples + j + 1,
                              tmpUnfinishedSamples + unfinishedSamplesForCalculationCount[i],
                              tmpUnfinishedSamples + j);
                    std::move(tmpUnfinishedSamplesCounts + j + 1,
                              tmpUnfinishedSamplesCounts + unfinishedSamplesForCalculationCount[i],
                              tmpUnfinishedSamplesCounts + j);
                    std::move(tmpUnfinishedSamplesAveragedCounts + j + 1,
                              tmpUnfinishedSamplesAveragedCounts + unfinishedSamplesForCalculationCount[i],
                              tmpUnfinishedSamplesAveragedCounts + j);
                }
                unfinishedSamplesForCalculationCount[i]--;
                j--;
            }

            // save current unfinished sample counts so we can average only the new ones
            prevCounts[i] = unfinishedSamplesForCalculationCount[i];
        }
    }

    short currentSample;
    // loop through incoming samples and listen for the threshold hit
    for (i = 0; i < inSampleCounts[selectedChannel]; i++) {
        currentSample = inSamples[selectedChannel][i];

        // heartbeat processing
        if (processBpm && triggerType == TRIGGER_ON_THRESHOLD) {
            sampleCounter++;
            lastTriggerSampleCounter++;

            // check if minimum BPM reset period passed after last threshold hit and reset if necessary
            if (lastTriggerSampleCounter > minBpmResetPeriodCount) resetBpm();
        }
        // end of heartbeat processing

        if (triggerType == TRIGGER_ON_THRESHOLD) { // triggering by a threshold value
            if (!inDeadPeriod) {
                // check if we hit the threshold
                if ((triggerValue[selectedChannel] >= 0 && currentSample > triggerValue[selectedChannel] &&
                     prevSample <= triggerValue[selectedChannel]) || (
                            triggerValue[selectedChannel] < 0 && currentSample < triggerValue[selectedChannel] &&
                            prevSample >= triggerValue[selectedChannel])) {
                    // we hit the threshold, turn on dead period of 5ms
                    inDeadPeriod = true;

                    // create new samples for current threshold
                    for (j = 0; j < channelCount; j++) {
                        prepareNewSamples(inSamples[j], inSampleCounts[j], j, i);
                    }

                    // heartbeat processingA
                    if (processBpm) {
                        // pass data to heartbeat helper
                        heartbeatHelper->beat(sampleCounter);
                        // reset the last triggered sample counter
                        // and start counting for next heartbeat reset period
                        lastTriggerSampleCounter = 0;
                    }
                    // end of heartbeat processing
                }
            } else {
                if (++deadPeriodSampleCounter > deadPeriodCount) {
                    deadPeriodSampleCounter = 0;
                    inDeadPeriod = false;
                }
            }
        } else if (inEventCount > 0) { // triggering on events
            for (j = 0; j < inEventCount; j++) {
                if (triggerType == TRIGGER_ON_EVENTS) {
                    if (i == inEventIndices[j]) {
                        // create new samples for current threshold
                        for (k = 0; k < channelCount; k++) {
                            prepareNewSamples(inSamples[k], inSampleCounts[k], k, i);
                        }
                    }
                } else {
                    if (i == inEventIndices[j] && triggerType == inEvents[j]) {
                        // create new samples for current threshold
                        for (k = 0; k < channelCount; k++) {
                            prepareNewSamples(inSamples[k], inSampleCounts[k], k, i);
                        }
                    }
                }
            }
        }

        prevSample = currentSample;
    }

    // add samples to local buffer
    int copyFromIncoming, copyFromBuffer;
    for (i = 0; i < channelCount; i++) {
        tmpInSampleCount = inSampleCounts[i];
        tmpInSamples = inSamples[i];
        tmpSamples = samplesForCalculation[i];
        tmpUnfinishedSamples = unfinishedSamplesForCalculation[i];
        tmpUnfinishedSamplesCounts = unfinishedSamplesForCalculationCounts[i];
        tmpUnfinishedSamplesAveragedCounts = unfinishedSamplesForCalculationAveragedCounts[i];
        tmpSummedSampleCounts = summedSamplesCounts[i];
        tmpSummedSamples = summedSamples[i];

        copyFromBuffer = std::max(bufferSampleCount - tmpInSampleCount, 0);
        copyFromIncoming = std::min(bufferSampleCount - copyFromBuffer, tmpInSampleCount);
        if (copyFromBuffer > 0)std::copy(buffer[i] + tmpInSampleCount, buffer[i] + bufferSampleCount, buffer[i]);
        std::copy(tmpInSamples, tmpInSamples + copyFromIncoming, buffer[i] + bufferSampleCount - copyFromIncoming);


        // add incoming samples to calculation of averages
        for (j = prevCounts[i]; j < unfinishedSamplesForCalculationCount[i]; j++) {
            tmpUnfinishedSamplesRow = tmpUnfinishedSamples[j];
            if (averagedSampleCount <= j) { // we look for the oldest one in the unfinished samples
                tmpSamplesToSubtractFrom = tmpUnfinishedSamples[j - averagedSampleCount];
            } else { // we look for the oldest one in the already collected and calculated samples
                tmpSamplesToSubtractFrom = tmpSamples[samplesForCalculationCount[i] -
                                                      averagedSampleCount + j];
            }
            kStart = tmpUnfinishedSamplesAveragedCounts[j];
            if (from > kStart) from = kStart;

            kEnd = tmpUnfinishedSamplesCounts[j];
            for (k = kStart; k < kEnd; k++) {
                // if we are calculating averagedSampleCount + 1. sample we should subtract the oldest one in the sum
                if (tmpSummedSampleCounts[k] == averagedSampleCount) {
                    // subtract the value and decrease summed samples count for current position
                    tmpSummedSamples[k] -= tmpSamplesToSubtractFrom[k];
                    tmpSummedSampleCounts[k]--;
                }
                // add new value and increase summed samples count for current position
                tmpSummedSamples[k] += tmpUnfinishedSamplesRow[k];
                tmpSummedSampleCounts[k]++;
            }
            tmpUnfinishedSamplesAveragedCounts[j] = kEnd;
        }
    }

    delete[] prevCounts;

    from = from != INT_MAX ? from : 0;
    to = to != INT_MIN ? to : 0;

    for (i = 0; i < channelCount; i++) {
        tmpSummedSampleCounts = summedSamplesCounts[i];
        tmpSummedSamples = summedSamples[i];
        tmpAveragedSamples = averagedSamples[i];

        // calculate the averages for all channels
        for (j = from; j < to; j++)
            if (tmpSummedSampleCounts[j] != 0)
                tmpAveragedSamples[j] = (short) (tmpSummedSamples[j] / tmpSummedSampleCounts[j]);
        std::copy(tmpAveragedSamples, tmpAveragedSamples + sampleCount, outSamples[i]);
    }
}

void ThresholdProcessor::prepareNewSamples(const short *inSamples, int length, int channelIndex, int sampleIndex) {
    short **tmpSamples = samplesForCalculation[channelIndex];
    short **tmpUnfinishedSamples = unfinishedSamplesForCalculation[channelIndex];
    int *tmpUnfinishedSamplesCounts = unfinishedSamplesForCalculationCounts[channelIndex];
    int *tmpUnfinishedSamplesAveragedCounts = unfinishedSamplesForCalculationAveragedCounts[channelIndex];
    int copyFromIncoming, copyFromBuffer;
    // we filled unfinished sample rows, let's start deleting old ones
    if (unfinishedSamplesForCalculationCount[channelIndex] >= UNFINISHED_SAMPLES_COUNT) {
        // first delete oldest finished sample row if it exist
        if (samplesForCalculationCount[channelIndex] > 0) {
            delete[] tmpSamples[0];
            // shift rest of the filled sample rows to left
            if (samplesForCalculationCount[channelIndex] > 1) {
                std::move(tmpSamples + 1,
                          tmpSamples + samplesForCalculationCount[channelIndex],
                          tmpSamples);
            }
            samplesForCalculationCount[channelIndex]--;
        }
        // then if oldest unfinished sample row has been filled move it to finished
        if (samplesForCalculationCount[channelIndex] < averagedSampleCount &&
            tmpUnfinishedSamplesCounts[0] >= sampleCount) {
            // add filled unfinished sample row to finished samples
            tmpSamples[samplesForCalculationCount[channelIndex]++] =
                    tmpUnfinishedSamples[0];
        }

        // shift rest of the unfinished sample rows and their appropriate counters to left
        std::move(tmpUnfinishedSamples + 1, tmpUnfinishedSamples + unfinishedSamplesForCalculationCount[channelIndex],
                  tmpUnfinishedSamples);
        std::move(tmpUnfinishedSamplesCounts + 1,
                  tmpUnfinishedSamplesCounts + unfinishedSamplesForCalculationCount[channelIndex],
                  tmpUnfinishedSamplesCounts);
        std::move(tmpUnfinishedSamplesAveragedCounts + 1,
                  tmpUnfinishedSamplesAveragedCounts + unfinishedSamplesForCalculationCount[channelIndex],
                  tmpUnfinishedSamplesAveragedCounts);
        unfinishedSamplesForCalculationCount[channelIndex]--;

    }


    tmpUnfinishedSamples[unfinishedSamplesForCalculationCount[channelIndex]] = new short[sampleCount]{
            0};
    copyFromBuffer = std::max(bufferSampleCount - sampleIndex, 0);
    copyFromIncoming = std::min(sampleCount - copyFromBuffer, length);
    if (copyFromBuffer > 0) {
        std::copy(buffer[channelIndex] + sampleIndex, buffer[channelIndex] + bufferSampleCount,
                  tmpUnfinishedSamples[unfinishedSamplesForCalculationCount[channelIndex]]);
    }
    std::copy(inSamples, inSamples + copyFromIncoming,
              tmpUnfinishedSamples[unfinishedSamplesForCalculationCount[channelIndex]] +
              copyFromBuffer);

    tmpUnfinishedSamplesCounts[unfinishedSamplesForCalculationCount[channelIndex]] =
            copyFromBuffer + copyFromIncoming;
    tmpUnfinishedSamplesAveragedCounts[unfinishedSamplesForCalculationCount[channelIndex]++] = 0;
}

void ThresholdProcessor::init() {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "init()");

    sampleCount = static_cast<int>(getSampleRate() * MAX_PROCESSED_SECONDS);
    bufferSampleCount = sampleCount / 2;

    buffer = new short *[getChannelCount()];
    unfinishedSamplesForCalculationCount = new int[getChannelCount()]{0};
    unfinishedSamplesForCalculationCounts = new int *[getChannelCount()];
    unfinishedSamplesForCalculationAveragedCounts = new int *[getChannelCount()];
    unfinishedSamplesForCalculation = new short **[getChannelCount()];
    samplesForCalculationCount = new int[getChannelCount()]{0};
    samplesForCalculation = new short **[getChannelCount()];
    summedSamplesCounts = new int *[getChannelCount()];
    summedSamples = new int *[getChannelCount()];
    averagedSamples = new short *[getChannelCount()];
    for (int i = 0; i < getChannelCount(); i++) {
        buffer[i] = new short[bufferSampleCount];
        unfinishedSamplesForCalculationCounts[i] = new int[UNFINISHED_SAMPLES_COUNT + averagedSampleCount]{0};
        unfinishedSamplesForCalculationAveragedCounts[i] = new int[UNFINISHED_SAMPLES_COUNT + averagedSampleCount]{
                0};
        unfinishedSamplesForCalculation[i] = new short *[UNFINISHED_SAMPLES_COUNT + averagedSampleCount];
        samplesForCalculation[i] = new short *[averagedSampleCount];
        summedSamplesCounts[i] = new int[sampleCount]{0};
        summedSamples[i] = new int[sampleCount]{0};
        averagedSamples[i] = new short[sampleCount]{0};
    }

    deadPeriodCount = static_cast<int>(getSampleRate() * DEAD_PERIOD_SECONDS);
    deadPeriodSampleCounter = 0;
    inDeadPeriod = false;

    prevSample = 0;

    heartbeatHelper->reset();
    heartbeatHelper->setSampleRate(getSampleRate());
    minBpmResetPeriodCount = (int) (getSampleRate() * DEFAULT_MIN_BPM_RESET_PERIOD_SECONDS);
    lastTriggerSampleCounter = 0;
    sampleCounter = 0;
}

void ThresholdProcessor::clean(int channelCount) {
    for (int i = 0; i < channelCount; i++) {
        delete[] buffer[i];
        delete[] unfinishedSamplesForCalculationCounts[i];
        delete[] unfinishedSamplesForCalculationAveragedCounts[i];
        if (unfinishedSamplesForCalculationCount[i] > 0) {
            for (int j = 0; j < unfinishedSamplesForCalculationCount[i]; j++) {
                delete[] unfinishedSamplesForCalculation[i][j];
            }
        }
        delete[] unfinishedSamplesForCalculation[i];
        if (samplesForCalculationCount[i] > 0) {
            for (int j = 0; j < samplesForCalculationCount[i]; j++) {
                delete[] samplesForCalculation[i][j];
            }
        }
        delete[] samplesForCalculation[i];
        delete[] summedSamplesCounts[i];
        delete[] summedSamples[i];
        delete[] averagedSamples[i];
    }
    delete[] buffer;
    delete[] unfinishedSamplesForCalculationCount;
    delete[] unfinishedSamplesForCalculationCounts;
    delete[] unfinishedSamplesForCalculationAveragedCounts;
    delete[] unfinishedSamplesForCalculation;
    delete[] samplesForCalculationCount;
    delete[] samplesForCalculation;
    delete[] summedSamplesCounts;
    delete[] averagedSamples;
    delete[] summedSamples;
}

void ThresholdProcessor::resetBpm() {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "resetBpm()");

    heartbeatHelper->reset();
    sampleCounter = 0;
    lastTriggerSampleCounter = 0;
}
