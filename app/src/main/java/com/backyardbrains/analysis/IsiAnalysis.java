package com.backyardbrains.analysis;

import android.support.annotation.NonNull;
import com.backyardbrains.utils.JniUtils;

import static com.backyardbrains.utils.LogUtils.makeLogTag;

class IsiAnalysis extends BaseAnalysis<int[]> {

    private static final String TAG = makeLogTag(IsiAnalysis.class);

    private static final int BIN_COUNT = 100;

    private final float[][] trains;

    IsiAnalysis(@NonNull String filePath, @NonNull float[][] trains, @NonNull AnalysisListener<int[]> listener) {
        super(filePath, listener);

        this.trains = trains;
    }

    @Override int[][] process() {
        final int[][] isi = new int[trains.length][BIN_COUNT];
        final int[] spikeCounts = new int[trains.length];
        for (int i = 0; i < trains.length; i++) spikeCounts[i] = trains[i].length;

        JniUtils.isiAnalysis(trains, trains.length, spikeCounts, isi, BIN_COUNT);

        return isi;
    }
}