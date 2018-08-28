package com.backyardbrains.audio;

import com.backyardbrains.events.HeartbeatEvent;
import com.backyardbrains.utils.AudioUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.greenrobot.eventbus.EventBus;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class HeartbeatHelper {

    private static final String TAG = makeLogTag(HeartbeatHelper.class);

    // By default we shouldn't process more than 1 seconds of samples in any given moment
    private static final double DEFAULT_PROCESSING_SECONDS = 6;
    // When one heartbeat ends by default we should have a dead period of 0.2s before checking for next heartbeat
    private static final double DEFAULT_DEAD_PERIOD_SECONDS = 0.2;
    // If current heartbeat happens by default 2.5s after previous one we don't want to take it into account
    private static final double DEFAULT_MAX_DIFF_BETWEEN_BEATS_SECONDS = 2.5;
    // Default sample rate that should be used when processing incoming data is 44100
    private static final int DEFAULT_SAMPLE_RATE = AudioUtils.SAMPLE_RATE;
    // The minimum number of beats we want to have before start processing
    private static final int MIN_NUMBER_OF_BEATS = 2;

    // Number of seconds we are monitoring
    private double processingTime = DEFAULT_PROCESSING_SECONDS;
    private double deadPeriod = DEFAULT_DEAD_PERIOD_SECONDS;
    private double maxDiffBetweenBeats = DEFAULT_MAX_DIFF_BETWEEN_BEATS_SECONDS;
    private int sampleRate = DEFAULT_SAMPLE_RATE;

    private int maxSampleCount = (int) (processingTime * sampleRate);
    private int deadPeriodSampleCount = (int) (deadPeriod * sampleRate);
    private int maxDiffBetweenBeatsSampleCount = (int) (maxDiffBetweenBeats * sampleRate);
    private int minute = (int) (TimeUnit.MINUTES.toSeconds(1) * sampleRate);

    private List<Integer> diffs = new ArrayList<>();
    private int sampleCount;
    private int prevSampleIndex;

    public HeartbeatHelper(int sampleRate) {
        setSampleRate(sampleRate);
    }

    /**
     * Sets sample rate to be used when processing.
     */
    public void setSampleRate(int sampleRate) {
        LOGD(TAG, "setSampleRate(" + sampleRate + ")");

        if (sampleRate > 0) this.sampleRate = sampleRate;

        // sample rate is changed, we should reset
        reset("setSampleRate");
    }

    /**
     * Resets all internal counters so processing can re-start. The caller that's "pushing" the beats should reset it's
     * own sample counter otherwise all future calculations from this moment on will be inaccurate.
     */
    public void reset(String who) {
        LOGD(TAG, "reset(" + who + ")");
        maxSampleCount = (int) (processingTime * sampleRate);
        deadPeriodSampleCount = (int) (deadPeriod * sampleRate);
        maxDiffBetweenBeatsSampleCount = (int) (maxDiffBetweenBeats * sampleRate);
        minute = (int) (TimeUnit.MINUTES.toSeconds(1) * sampleRate);

        diffs = new ArrayList<>();
        sampleCount = 0;
        prevSampleIndex = 0;

        // we reset everything so tell UI that BPM is 0
        EventBus.getDefault().post(new HeartbeatEvent(0));
    }

    /**
     * "Pushes" a new beat that's triggered at {@code sampleIndex} sample.
     */
    public void beat(int sampleIndex) {
        LOGD(TAG, "beat()");

        int diffFromPrevBeat = sampleIndex - prevSampleIndex;
        // we shouldn't process beat if dead period didn't pass
        if (diffFromPrevBeat < deadPeriodSampleCount) {
            LOGD(TAG, "Difference between two beats is less than 0.2s");
            return;
        }
        // we should reset if time difference between current and last beat is longer then required
        if (diffFromPrevBeat > maxDiffBetweenBeatsSampleCount) {
            LOGD(TAG, "Difference between two beats is more than 2.5s");
            // reset all local variables so we can restart the calculation
            reset("beat()");
            // save current sample index for next calculation
            prevSampleIndex = sampleIndex;

            return;
        }

        // store difference between last two beats
        diffs.add(diffFromPrevBeat);
        // we need at least 2 beats to start calculation
        if (diffs.size() < MIN_NUMBER_OF_BEATS) {
            LOGD(TAG, "There is less then 2 collected diffs");
            // we can just save difference because prevSampleIndex is 0
            sampleCount = diffFromPrevBeat;
            // save current sample index for next calculation
            prevSampleIndex = sampleIndex;
            // post to UI that we're still at 0 BPM
            EventBus.getDefault().post(new HeartbeatEvent(0));

            return;
        }

        int tmpSampleCount = sampleCount + diffFromPrevBeat;
        if (tmpSampleCount > maxSampleCount) {
            int counter = 0;
            while ((tmpSampleCount = tmpSampleCount - diffs.get(counter)) > maxSampleCount) {
                counter++;
            }
            if (counter < diffs.size()) diffs = new ArrayList<>(diffs.subList(counter + 1, diffs.size()));
        }

        sampleCount = tmpSampleCount;
        int bpm = minute / (sampleCount / diffs.size());
        LOGD(TAG, "Sample count: " + sampleCount);
        LOGD(TAG, "BPM: " + bpm);
        LOGD(TAG, Arrays.toString(diffs.toArray()));
        // tell UI what is the current bpm
        EventBus.getDefault().post(new HeartbeatEvent(bpm));

        prevSampleIndex = sampleIndex;
    }
}
