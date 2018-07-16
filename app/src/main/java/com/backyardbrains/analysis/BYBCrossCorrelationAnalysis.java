package com.backyardbrains.analysis;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.utils.JniUtils;

import static com.backyardbrains.utils.LogUtils.makeLogTag;

class BYBCrossCorrelationAnalysis extends BYBBaseAnalysis<int[]> {

    private static final String TAG = makeLogTag(BYBCrossCorrelationAnalysis.class);

    private static final float MAX_TIME = 0.1f;
    private static final float BIN_SIZE = 0.001f;

    private final float[][] trains;

    BYBCrossCorrelationAnalysis(@NonNull String filePath, @NonNull float[][] trains,
        @NonNull AnalysisListener<int[]> listener) {
        super(filePath, listener);

        this.trains = trains;
    }

    @Nullable @Override int[][] process() {
        int binCount = (int) Math.ceil((2 * MAX_TIME + BIN_SIZE) / BIN_SIZE);
        final int[][] crossCorrelation = new int[trains.length * trains.length][binCount];
        final int[] spikeCounts = new int[trains.length];
        for (int i = 0; i < trains.length; i++) spikeCounts[i] = trains[i].length;

        JniUtils.crossCorrelationAnalysis(trains, trains.length, spikeCounts, crossCorrelation, crossCorrelation.length,
            binCount);

        return crossCorrelation;
    }
}
