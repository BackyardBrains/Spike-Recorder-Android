//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include "ThresholdProcessor.h"

const char *ThresholdProcessor::TAG = "ThresholdProcessor";

ThresholdProcessor::ThresholdProcessor(OnHeartbeatListener *listener) {
    heartbeatHelper = new HeartbeatHelper(getSampleRate(), listener);

    // we need to initialize initial trigger values and local buffer because they depend on channel count
    triggerValue = new float[getChannelCount()];
    for (int i = 0; i < getChannelCount(); i++) {
        triggerValue[i] = INT_MAX;
    }
    lastTriggeredValue = new float[getChannelCount()]{0};

    init(true);
}

ThresholdProcessor::~ThresholdProcessor() {
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

void ThresholdProcessor::setThreshold(float threshold) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "setThreshold(%f)", threshold);

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

void ThresholdProcessor::appendIncomingSamples(short **inSamples, int *inSampleCounts) {
    bool shouldReset = false;
    bool shouldResetLocalBuffer = false;
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
        shouldResetLocalBuffer = true;
    }
    // let's save last channel count so we can use it to delete all the arrays
    int channelCount = getChannelCount();
    int tmpLastChannelCount = lastChannelCount;
    if (lastChannelCount != channelCount) {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "Resetting because channel count has changed");
        lastChannelCount = channelCount;
        shouldReset = true;
        shouldResetLocalBuffer = true;
    }
    if (shouldReset || resetOnNextBatch) {
        // reset rest of the data
        clean(tmpLastChannelCount, shouldResetLocalBuffer);
        init(shouldResetLocalBuffer);
        resetOnNextBatch = false;
    }

    int tmpInSampleCount;
    short *tmpInSamples;
    int copyFromIncoming, copyFromBuffer;
    int i;

    // in case we don't need to average let's just add incoming samples to local buffer
    for (i = 0; i < channelCount; i++) {
        tmpInSampleCount = inSampleCounts[i];
        tmpInSamples = inSamples[i];

        // add samples to local buffer
        copyFromBuffer = std::max(bufferSampleCount - tmpInSampleCount, 0);
        copyFromIncoming = std::min(bufferSampleCount - copyFromBuffer, tmpInSampleCount);
        if (copyFromBuffer > 0) std::copy(buffer[i] + tmpInSampleCount, buffer[i] + bufferSampleCount, buffer[i]);
        std::copy(tmpInSamples, tmpInSamples + copyFromIncoming, buffer[i] + bufferSampleCount - copyFromIncoming);
    }
    return;
}

void
ThresholdProcessor::process(short **outSamples, int *outSamplesCounts, short **inSamples, const int *inSampleCounts,
                            const int *inEventIndices, const int *inEvents, const int inEventCount) {
    if (paused) return;

    bool shouldReset = false;
    bool shouldResetLocalBuffer = false;
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
        shouldResetLocalBuffer = true;
    }
    // let's save last channel count so we can use it to delete all the arrays
    int channelCount = getChannelCount();
    int tmpLastChannelCount = lastChannelCount;
    if (lastChannelCount != channelCount) {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "Resetting because channel count has changed");
        lastChannelCount = channelCount;
        shouldReset = true;
        shouldResetLocalBuffer = true;
    }
    if (shouldReset || resetOnNextBatch) {
        // reset rest of the data
        clean(tmpLastChannelCount, shouldResetLocalBuffer);
        init(shouldResetLocalBuffer);
        resetOnNextBatch = false;
    }

    int tmpInSampleCount;
    short *tmpInSamples;
    int copyFromIncoming, copyFromBuffer;
    int i;
    short **tmpSamples;
    int *tmpSamplesCounts;
    short *tmpSamplesRow;
    int *tmpSummedSampleCounts;
    int *tmpSummedSamples;
    short *tmpAveragedSamples;
    int samplesToCopy;
    int j, k;
    int kStart, kEnd;

    for (i = 0; i < channelCount; i++) {
        tmpInSampleCount = inSampleCounts[i];
        tmpInSamples = inSamples[i];
        tmpSamples = samplesForCalculation[i];
        tmpSamplesCounts = samplesForCalculationCounts[i];
        tmpSummedSampleCounts = summedSamplesCounts[i];
        tmpSummedSamples = summedSamples[i];

        // append unfinished sample buffers with incoming samples
        for (j = 0; j < samplesForCalculationCount[i]; j++) {
            tmpSamplesRow = tmpSamples[j];
            kStart = tmpSamplesCounts[j];

            // we just need to append enough to fill the unfinished rows till end (sampleCount)
            samplesToCopy = std::min(sampleCount - kStart, tmpInSampleCount);
            std::copy(tmpInSamples, tmpInSamples + samplesToCopy, tmpSamplesRow + kStart);

            kEnd = kStart + samplesToCopy;
            for (k = kStart; k < kEnd; k++) {
                // add new value and increase summed samples count for current position
                tmpSummedSamples[k] += tmpSamplesRow[k];
                tmpSummedSampleCounts[k]++;
            }
            tmpSamplesCounts[j] = kEnd;
        }
    }


    short currentSample;
    // loop through incoming samples and listen for the threshold hit
    for (i = 0; i < inSampleCounts[selectedChannel]; i++) {
        currentSample = inSamples[selectedChannel][i];

        // heartbeat processing Can't add incoming to buffer, it's larger then buffer
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
                     prevSample <= triggerValue[selectedChannel]) ||
                    (triggerValue[selectedChannel] < 0 && currentSample < triggerValue[selectedChannel] &&
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

    for (i = 0; i < channelCount; i++) {
        tmpInSampleCount = inSampleCounts[i];
        tmpInSamples = inSamples[i];

        // add samples to local buffer
        copyFromBuffer = std::max(bufferSampleCount - tmpInSampleCount, 0);
        copyFromIncoming = std::min(bufferSampleCount - copyFromBuffer, tmpInSampleCount);
        if (copyFromBuffer > 0) std::copy(buffer[i] + tmpInSampleCount, buffer[i] + bufferSampleCount, buffer[i]);
        std::copy(tmpInSamples, tmpInSamples + copyFromIncoming, buffer[i] + bufferSampleCount - copyFromIncoming);
    }

    int *counts = new int[averagedSampleCount]{0};
    for (i = 0; i < channelCount; i++) {
        tmpSummedSampleCounts = summedSamplesCounts[i];
        tmpSummedSamples = summedSamples[i];
        tmpAveragedSamples = averagedSamples[i];

        // calculate the averages for all channels
        for (j = 0; j < sampleCount; j++)
            if (tmpSummedSampleCounts[j] != 0)
                tmpAveragedSamples[j] = (short) (tmpSummedSamples[j] / tmpSummedSampleCounts[j]);
            else
                tmpAveragedSamples[j] = 0;
        std::copy(tmpAveragedSamples, tmpAveragedSamples + sampleCount, outSamples[i]);
        outSamplesCounts[i] = sampleCount;
    }
    delete[] counts;
}

void ThresholdProcessor::prepareNewSamples(const short *inSamples, int length, int channelIndex, int sampleIndex) {
    short **tmpSamples = samplesForCalculation[channelIndex];
    int *tmpSamplesCounts = samplesForCalculationCounts[channelIndex];
    int *tmpSummedSamples = summedSamples[channelIndex];
    int *tmpSummedSamplesCounts = summedSamplesCounts[channelIndex];
    short *tmpSamplesRowZero;

    // create new sample row
    short *newSampleRow = new short[sampleCount]{0};
    int copyFromIncoming, copyFromBuffer;
    copyFromBuffer = std::max(bufferSampleCount - sampleIndex, 0);
    copyFromIncoming = std::min(sampleCount - copyFromBuffer, length);
    if (copyFromBuffer > 0) {
        std::copy(buffer[channelIndex] + sampleIndex, buffer[channelIndex] + bufferSampleCount, newSampleRow);
    }
    std::copy(inSamples, inSamples + copyFromIncoming, newSampleRow + copyFromBuffer);


    tmpSamplesRowZero = tmpSamples[0];
    int copySamples = copyFromBuffer + copyFromIncoming;
    bool shouldDeleteOldestRow = samplesForCalculationCount[channelIndex] >= averagedSampleCount;
    int len = shouldDeleteOldestRow ? tmpSamplesCounts[0] : copySamples;
    int i;
    for (i = 0; i < len; i++) {
        // subtract the value and decrease summed samples count for current position
        if (shouldDeleteOldestRow) {
            tmpSummedSamples[i] -= tmpSamplesRowZero[i];
            tmpSummedSamplesCounts[i]--;
        }
        if (i < copySamples) {
            // add new value and increase summed samples count for current position
            tmpSummedSamples[i] += newSampleRow[i];
            tmpSummedSamplesCounts[i]++;
        }
    }

    // remove oldest sample row if we're full
    if (shouldDeleteOldestRow) {
        // delete the oldest sample row
        delete[] tmpSamples[0];
        // shift rest of the filled sample rows to left
        std::move(tmpSamples + 1, tmpSamples + samplesForCalculationCount[channelIndex], tmpSamples);
        std::move(tmpSamplesCounts + 1, tmpSamplesCounts + samplesForCalculationCount[channelIndex], tmpSamplesCounts);
        samplesForCalculationCount[channelIndex]--;
    }
    // add new sample row
    tmpSamples[samplesForCalculationCount[channelIndex]] = newSampleRow;
    tmpSamplesCounts[samplesForCalculationCount[channelIndex]++] = copySamples;
}

void ThresholdProcessor::init(bool resetLocalBuffer) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "init()");
    float sampleRate = getSampleRate();
    int channelCount = getChannelCount();

    sampleCount = static_cast<int>(sampleRate * MAX_PROCESSED_SECONDS);
    bufferSampleCount = sampleCount / 2;

    if (resetLocalBuffer)buffer = new short *[channelCount];
    samplesForCalculationCount = new int[channelCount]{0};
    samplesForCalculationCounts = new int *[channelCount];
    samplesForCalculation = new short **[channelCount];
    summedSamplesCounts = new int *[channelCount];
    summedSamples = new int *[channelCount];
    averagedSamples = new short *[channelCount];
    for (int i = 0; i < channelCount; i++) {
        if (resetLocalBuffer) buffer[i] = new short[bufferSampleCount];
        samplesForCalculationCounts[i] = new int[averagedSampleCount]{0};
        samplesForCalculation[i] = new short *[averagedSampleCount];
        summedSamplesCounts[i] = new int[sampleCount]{0};
        summedSamples[i] = new int[sampleCount]{0};
        averagedSamples[i] = new short[sampleCount]{0};
    }

    deadPeriodCount = static_cast<int>(sampleRate * DEAD_PERIOD_SECONDS);
    deadPeriodSampleCounter = 0;
    inDeadPeriod = false;

    prevSample = 0;

    heartbeatHelper->reset();
    heartbeatHelper->setSampleRate(sampleRate);
    minBpmResetPeriodCount = (int) (sampleRate * DEFAULT_MIN_BPM_RESET_PERIOD_SECONDS);
    lastTriggerSampleCounter = 0;
    sampleCounter = 0;
}

void ThresholdProcessor::clean(int channelCount, bool resetLocalBuffer) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "clean()");
    for (int i = 0; i < channelCount; i++) {
        if (resetLocalBuffer) delete[] buffer[i];
        delete[] samplesForCalculationCounts[i];
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
    if (resetLocalBuffer) delete[] buffer;
    delete[] samplesForCalculationCount;
    delete[] samplesForCalculationCounts;
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