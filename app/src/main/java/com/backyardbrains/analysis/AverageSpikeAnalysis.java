package com.backyardbrains.analysis;

import android.support.annotation.NonNull;
import com.backyardbrains.dsp.audio.AudioFile;
import com.backyardbrains.utils.JniUtils;
import com.backyardbrains.vo.AverageSpike;

import static com.backyardbrains.utils.LogUtils.makeLogTag;

class AverageSpikeAnalysis extends BaseAnalysis<int[][], AverageSpike> {

    private static final String TAG = makeLogTag(AverageSpikeAnalysis.class);

    private static final float BATCH_SPIKE_HALF_IN_SECS = 0.002f;

    private final AudioFile audioFile;

    AverageSpikeAnalysis(@NonNull AudioFile audioFile, @NonNull AnalysisListener<AverageSpike> listener) {
        super(audioFile.getAbsolutePath(), listener);

        this.audioFile = audioFile;
    }

    @Override protected AverageSpike[] process(int[][]... params) {
        if (params.length <= 0) return new AverageSpike[0];

        final int[][] trains = params[0];
        final int trainCount = trains.length;
        final int sampleRate = audioFile.sampleRate();
        final int batchSpikeHalfCount = (int) (sampleRate * BATCH_SPIKE_HALF_IN_SECS);
        final int batchSpikeCount = 2 * batchSpikeHalfCount + 1;
        final float[][] averageSpike = new float[trainCount][batchSpikeCount];
        final float[][] normAverageSpike = new float[trainCount][batchSpikeCount];
        final float[][] normTopStdLine = new float[trainCount][batchSpikeCount];
        final float[][] normBottomStdLine = new float[trainCount][batchSpikeCount];
        final int[] spikeCounts = new int[trainCount];
        for (int i = 0; i < trainCount; i++) spikeCounts[i] = trains[i].length;

        JniUtils.averageSpikeAnalysis(audioFile.getAbsolutePath(), trains, trainCount, spikeCounts, averageSpike,
            normAverageSpike, normTopStdLine, normBottomStdLine, batchSpikeCount);

        // let's populate avr array
        final AverageSpike[] averageSpikes = new AverageSpike[trainCount];
        for (int i = 0; i < trainCount; i++) {
            averageSpikes[i] =
                new AverageSpike(averageSpike[i], normAverageSpike[i], normTopStdLine[i], normBottomStdLine[i]);
        }

        return averageSpikes;
    }
}
