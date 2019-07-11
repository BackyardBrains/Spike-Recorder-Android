package com.backyardbrains.analysis;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.utils.JniUtils;

import static com.backyardbrains.utils.LogUtils.makeLogTag;

class AutocorrelationAnalysis extends BaseAnalysis<float[][], int[]> {

    private static final String TAG = makeLogTag(AutocorrelationAnalysis.class);

    private static final float MAX_TIME = 0.1f; // 100ms
    private static final float BIN_SIZE = 0.001f; // 1ms

    AutocorrelationAnalysis(@NonNull String filePath, @NonNull AnalysisListener<int[]> listener) {
        super(filePath, listener);
    }

    @Nullable @Override protected int[][] process(float[][]... params) {
        if (params.length <= 0) return new int[0][0];

        final float[][] trains = params[0];
        int binCount = (int) Math.ceil((MAX_TIME + BIN_SIZE) / BIN_SIZE);
        final int[][] autoCorrelation = new int[trains.length][binCount];
        final int[] spikeCounts = new int[trains.length];
        for (int i = 0; i < trains.length; i++) spikeCounts[i] = trains[i].length;

        JniUtils.autocorrelationAnalysis(trains, trains.length, spikeCounts, autoCorrelation, binCount);

        return autoCorrelation;
    }
}
