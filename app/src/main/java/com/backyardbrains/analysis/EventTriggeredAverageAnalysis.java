package com.backyardbrains.analysis;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.dsp.audio.AudioFile;
import com.backyardbrains.utils.JniUtils;
import com.backyardbrains.utils.RecordingUtils;
import com.backyardbrains.vo.EventTriggeredAverages;
import java.io.File;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class EventTriggeredAverageAnalysis extends BaseAnalysis<EventTriggeredAveragesConfig, EventTriggeredAverages> {

    private static final float EVENT_LEFT_OFFSET_IN_SECS = .7f;
    private static final float EVENT_RIGHT_OFFSET_IN_SECS = .7f;

    private final AudioFile audioFile;

    EventTriggeredAverageAnalysis(@NonNull AudioFile audioFile,
        @NonNull AnalysisListener<EventTriggeredAverages> listener) {
        super(audioFile.getAbsolutePath(), listener);

        this.audioFile = audioFile;
    }

    @Nullable @Override protected EventTriggeredAverages[] process(EventTriggeredAveragesConfig... params) {
        final EventTriggeredAveragesConfig config = params != null && params.length > 0 ? params[0] : null;
        if (config == null) return new EventTriggeredAverages[0];

        final File eventsFile = RecordingUtils.getEventFile(new File(audioFile.getAbsolutePath()));
        if (eventsFile != null && config.getEvents() != null && config.getEvents().length > 0) {
            final int channelCount = audioFile.channelCount();
            final int sampleRate = audioFile.sampleRate();
            final int eventCount = config.getEvents().length;

            // determine buffer size
            final int leftOffsetSampleCount = (int) (sampleRate * channelCount * EVENT_LEFT_OFFSET_IN_SECS);
            final int rightOffsetSampleCount = (int) (sampleRate * channelCount * EVENT_RIGHT_OFFSET_IN_SECS);
            final int sampleCount = leftOffsetSampleCount + rightOffsetSampleCount;
            final int frameCount = sampleCount / channelCount;
            //
            final float[][][] averages = new float[channelCount][][];
            final float[][][] normAverages = new float[channelCount][][];
            for (int i = 0; i < channelCount; i++) {
                averages[i] = new float[eventCount][];
                normAverages[i] = new float[eventCount][];
                for (int j = 0; j < eventCount; j++) {
                    averages[i][j] = new float[frameCount];
                    normAverages[i][j] = new float[frameCount];
                }
            }

            JniUtils.eventTriggeredAverageAnalysis(audioFile.getAbsolutePath(), eventsFile.getAbsolutePath(),
                config.getEvents(), eventCount, averages, normAverages, channelCount, frameCount,
                config.isRemoveNoiseIntervals());

            // let's populate avr array
            final int len = averages.length;
            final EventTriggeredAverages[] eventTriggeredAverages = new EventTriggeredAverages[len];
            for (int i = 0; i < len; i++) {
                eventTriggeredAverages[i] =
                    new EventTriggeredAverages(config.getEvents(), averages[i], normAverages[i]);
            }

            return eventTriggeredAverages;
        }

        return new EventTriggeredAverages[0];
    }
}
