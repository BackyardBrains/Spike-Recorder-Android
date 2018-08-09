//
// Created by Tihomir Leka <tihomir at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_HEARTBEATHELPER_H
#define SPIKE_RECORDER_ANDROID_HEARTBEATHELPER_H

#include <android/log.h>
#include <algorithm>

namespace util {
    class OnHeartbeatListener;

    class HeartbeatHelper;
}

class OnHeartbeatListener {
public:
    virtual void onHeartbeat(int bmp) = 0;
};

class HeartbeatHelper {
public:
    HeartbeatHelper(float sampleRate, OnHeartbeatListener *listener);

    ~HeartbeatHelper();

    /**
     * "Pushes" a new beat that's triggered at {@code sampleIndex} sample.
     */
    void beat(int sampleIndex);

    /**
     * Sets the current sample rate.
     */
    void setSampleRate(float sampleRate);

    /**
     * Resets all internal counters so processing can re-start. The caller that's "pushing" the beats should reset it's
     * own sample counter otherwise all future calculations from this moment on will be inaccurate.
     */
    void reset();

private:
    static const char *TAG;

    // By default we shouldn't process more than 1 seconds of samples in any given moment
    static constexpr float DEFAULT_PROCESSING_SECONDS = 6.0f;
    // When one heartbeat ends by default we should have a dead period of 0.2s before checking for next heartbeat
    static constexpr float DEFAULT_DEAD_PERIOD_SECONDS = 0.2f;
    // If current heartbeat happens by default 2.5s after previous one we don't want to take it into account
    static constexpr float DEFAULT_MAX_DIFF_BETWEEN_BEATS_SECONDS = 2.5f;
    // Default sample rate we start with
    static constexpr float DEFAULT_SAMPLE_RATE = 44100.0f;
    // The minimum number of beats we want to have before start processing
    static const int MIN_NUMBER_OF_BEATS = 2;
    // Max number of diffs we save
    static const int DIFFS_COUNT = 100;

    // Listener that's being invoked every time bpm is triggered
    OnHeartbeatListener *listener;

    // Current sample rate
    float sampleRate = DEFAULT_SAMPLE_RATE;
    // Number of samples processed at once
    int maxSampleCount = (int) (sampleRate * DEFAULT_PROCESSING_SECONDS);
    // Number of samples we should skip processing (dead period)
    int deadPeriodSampleCount = (int) (sampleRate * DEFAULT_DEAD_PERIOD_SECONDS);
    // Number of samples between two consecutive beats
    int maxDiffBetweenBeatsSampleCount = (int) (sampleRate * DEFAULT_MAX_DIFF_BETWEEN_BEATS_SECONDS);
    // Number of samples in one minute
    int minute = (int) (sampleRate * 60);

    int *diffs;
    int diffsCounter = 0;
    int sampleCount = 0;
    int prevSampleIndex = 0;
};


#endif //SPIKE_RECORDER_ANDROID_HEARTBEATHELPER_H
