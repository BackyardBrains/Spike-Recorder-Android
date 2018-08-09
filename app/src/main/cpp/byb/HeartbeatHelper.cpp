//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#include "HeartbeatHelper.h"

const char *HeartbeatHelper::TAG = "HeartbeatHelper";

HeartbeatHelper::HeartbeatHelper(float sampleRate, OnHeartbeatListener *listener) {
    HeartbeatHelper::listener = listener;

    setSampleRate(sampleRate);
}

HeartbeatHelper::~HeartbeatHelper() {
}


void HeartbeatHelper::beat(int sampleIndex) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "beat()");

    int diffFromPrevBeat = sampleIndex - prevSampleIndex;
    // we shouldn't process beat if dead period didn't pass
    if (diffFromPrevBeat < deadPeriodSampleCount) {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "Difference between two beats is less than 0.2s");
        return;
    }
    // we should reset if time difference between current and last beat is longer then required
    if (diffFromPrevBeat > maxDiffBetweenBeatsSampleCount) {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "Difference between two beats is more than 2.5s");
        // reset all local variables so we can restart the calculation
        reset();
        // save current sample index for next calculation
        prevSampleIndex = sampleIndex;

        return;
    }

    // store difference between last two beats
//    diffs.add(diffFromPrevBeat);
    diffs[diffsCounter++] = diffFromPrevBeat;
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "DIFFS COUNTER: %d", diffsCounter);
    // we need at least 2 beats to start calculation
    if (/*diffs.size()*/diffsCounter < MIN_NUMBER_OF_BEATS) {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "There is less then 2 collected diffs");
        // we can just save difference because prevSampleIndex is 0
        sampleCount = diffFromPrevBeat;
        // save current sample index for next calculation
        prevSampleIndex = sampleIndex;
        // post to UI that we're still at 0 BPM
        listener->onHeartbeat(0);

        return;
    }

    int tmpSampleCount = sampleCount + diffFromPrevBeat;
    if (tmpSampleCount > maxSampleCount) {
        int counter = 0;
        while ((tmpSampleCount = tmpSampleCount - diffs[counter]/*diffs.get(counter)*/) > maxSampleCount) {
            counter++;
        }
//        if (counter < diffs.size()) diffs = new ArrayList<>(diffs.subList(counter + 1, diffs.size()));
        if (counter < diffsCounter) {
            std::move(diffs + counter + 1, diffs + diffsCounter, diffs);
            diffsCounter -= counter + 1;
        }
    }

    sampleCount = tmpSampleCount;
    int bpm = minute / (sampleCount / diffsCounter/*diffs.size()*/);
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "BPM: %d", bpm);
    // tell UI what is the current bpm
    listener->onHeartbeat(bpm);

    prevSampleIndex = sampleIndex;
}

void HeartbeatHelper::setSampleRate(float sampleRate) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "SAMPLE RATE: %1f", sampleRate);

    if (sampleRate <= 0 || HeartbeatHelper::sampleRate == sampleRate) return;

    HeartbeatHelper::sampleRate = sampleRate;

    // sample rate is changed, we should reset
    reset();
}

void HeartbeatHelper::reset() {
    maxSampleCount = (int) (sampleRate * DEFAULT_PROCESSING_SECONDS);
    deadPeriodSampleCount = (int) (sampleRate * DEFAULT_DEAD_PERIOD_SECONDS);
    maxDiffBetweenBeatsSampleCount = (int) (sampleRate * DEFAULT_MAX_DIFF_BETWEEN_BEATS_SECONDS);
    minute = (int) (sampleRate * 60);

    if (diffsCounter > 0) delete[] diffs;
    diffs = new int[DIFFS_COUNT]{0};
    diffsCounter = 0;
    sampleCount = 0;
    prevSampleIndex = 0;

    // we reset everything so tell UI that BPM is 0
    listener->onHeartbeat(0);
}