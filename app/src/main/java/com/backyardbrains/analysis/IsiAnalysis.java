package com.backyardbrains.analysis;

import androidx.annotation.NonNull;
import com.backyardbrains.utils.JniUtils;

import static com.backyardbrains.utils.LogUtils.makeLogTag;

class IsiAnalysis extends BaseAnalysis<float[][], int[][]> {

    private static final String TAG = makeLogTag(IsiAnalysis.class);

    private static final int BIN_COUNT = 100;

    IsiAnalysis(@NonNull String filePath, @NonNull AnalysisListener<int[][]> listener) {
        super(filePath, listener);
    }

    @Override protected int[][] process(float[][]... params) {
        if (params.length <= 0) return new int[0][0];

        final float[][] trains = params[0];
        final int[][] isi = new int[trains.length][BIN_COUNT];
        final int[] spikeCounts = new int[trains.length];
        for (int i = 0; i < trains.length; i++) spikeCounts[i] = trains[i].length;

        JniUtils.isiAnalysis(trains, trains.length, spikeCounts, isi, BIN_COUNT);

        return isi;
    }
}