package com.backyardbrains.analysis;

import android.support.annotation.NonNull;
import com.backyardbrains.audio.BYBAudioFile;
import com.backyardbrains.data.AverageSpike;
import com.backyardbrains.utils.JniUtils;

import static com.backyardbrains.utils.LogUtils.makeLogTag;

class BYBAverageSpikeAnalysis extends BYBBaseAnalysis<AverageSpike> {

    private static final String TAG = makeLogTag(BYBAverageSpikeAnalysis.class);

    private static final float BATCH_SPIKE_HALF_IN_SECS = 0.002f;

    private final BYBAudioFile audioFile;
    private final int[][] trains;

    BYBAverageSpikeAnalysis(@NonNull BYBAudioFile audioFile, @NonNull int[][] trains,
        @NonNull AnalysisListener<AverageSpike> listener) {
        super(audioFile.getAbsolutePath(), listener);

        this.audioFile = audioFile;
        this.trains = trains;
    }

    @Override AverageSpike[] process() {
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
