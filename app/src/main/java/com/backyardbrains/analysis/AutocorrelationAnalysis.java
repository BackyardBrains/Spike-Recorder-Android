package com.backyardbrains.analysis;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.utils.JniUtils;

import static com.backyardbrains.utils.LogUtils.makeLogTag;

class AutocorrelationAnalysis extends BaseAnalysis<int[]> {

    private static final String TAG = makeLogTag(AutocorrelationAnalysis.class);

    private static final float MAX_TIME = 0.1f; // 100ms
    private static final float BIN_SIZE = 0.001f; // 1ms

    private final float[][] trains;

    AutocorrelationAnalysis(@NonNull String filePath, @NonNull float[][] trains,
        @NonNull AnalysisListener<int[]> listener) {
        super(filePath, listener);

        this.trains = trains;
    }

    @Nullable @Override int[][] process() throws Exception {
        int binCount = (int) Math.ceil((MAX_TIME + BIN_SIZE) / BIN_SIZE) - 1;
        final int[][] autoCorrelation = new int[trains.length][binCount];
        final int[] spikeCounts = new int[trains.length];
        for (int i = 0; i < trains.length; i++) spikeCounts[i] = trains[i].length;

        JniUtils.autocorrelationAnalysis(trains, trains.length, spikeCounts, autoCorrelation, binCount);

        return autoCorrelation;
    }
}
