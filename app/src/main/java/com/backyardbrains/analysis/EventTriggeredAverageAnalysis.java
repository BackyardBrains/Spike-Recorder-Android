package com.backyardbrains.analysis;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.backyardbrains.dsp.audio.AudioFile;
import com.backyardbrains.utils.JniUtils;
import com.backyardbrains.utils.RecordingUtils;
import com.backyardbrains.vo.EventTriggeredAverages;
import java.io.File;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class EventTriggeredAverageAnalysis
    extends BaseAnalysis<EventTriggeredAveragesConfig, EventTriggeredAverages[]> {

    private static final float EVENT_LEFT_OFFSET_IN_SECS = .7f;
    private static final float EVENT_RIGHT_OFFSET_IN_SECS = .7f;

    private final AudioFile audioFile;

    EventTriggeredAverageAnalysis(@NonNull AudioFile audioFile,
        @NonNull AnalysisListener<EventTriggeredAverages[]> listener) {
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
            final float[][][] averages = new float[eventCount][][];
            final float[][][] normAverages = new float[eventCount][][];
            for (int i = 0; i < eventCount; i++) {
                averages[i] = new float[channelCount][];
                normAverages[i] = new float[channelCount][];
                for (int j = 0; j < channelCount; j++) {
                    averages[i][j] = new float[frameCount];
                    normAverages[i][j] = new float[frameCount];
                }
            }
            final float[][] normMcAverages = new float[channelCount][];
            final float[][] normMcTop = new float[channelCount][];
            final float[][] normMcBottom = new float[channelCount][];
            final float[][] minMax = new float[channelCount][];
            for (int i = 0; i < channelCount; i++) {
                normMcAverages[i] = new float[frameCount];
                normMcTop[i] = new float[frameCount];
                normMcBottom[i] = new float[frameCount];
                minMax[i] = new float[2];
            }

            JniUtils.eventTriggeredAverageAnalysis(audioFile.getAbsolutePath(), eventsFile.getAbsolutePath(),
                config.getEvents(), eventCount, averages, normAverages, normMcAverages, normMcTop, normMcBottom, minMax,
                channelCount, frameCount, config.isRemoveNoiseIntervals(), config.getConfidenceIntervalsEvent());

            // let's populate avr array
            final EventTriggeredAverages[] eventTriggeredAverages = new EventTriggeredAverages[channelCount];
            for (int i = 0; i < channelCount; i++) {
                float[][] channelAverages = new float[eventCount][];
                float[][] channelNormAverages = new float[eventCount][];
                for (int j = 0; j < eventCount; j++) {
                    channelAverages[j] = averages[j][i];
                    channelNormAverages[j] = normAverages[j][i];
                }
                eventTriggeredAverages[i] =
                    new EventTriggeredAverages(config.getEvents(), channelAverages, channelNormAverages,
                        config.getConfidenceIntervalsEvent() != null, normMcAverages[i], normMcTop[i], normMcBottom[i],
                        minMax[i][0], minMax[i][1]);
            }

            return eventTriggeredAverages;
        }

        return new EventTriggeredAverages[0];
    }
}
