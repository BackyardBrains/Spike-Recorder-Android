package com.backyardbrains.analysis;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.util.Arrays;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

class BYBAutocorrelationAnalysis extends BYBBaseAnalysis<int[]> {

    private static final String TAG = makeLogTag(BYBAutocorrelationAnalysis.class);

    private static final float MAX_TIME = 0.1f; // 100ms
    private static final float BIN_SIZE = 0.001f; // 1ms
    private static final float MIN_EDGE = -BIN_SIZE * 0.5f; // -.5ms
    private static final float MAX_EDGE = MAX_TIME + BIN_SIZE * 0.5f; // 100.5ms

    private final float[][] trains;

    BYBAutocorrelationAnalysis(@NonNull String filePath, @NonNull float[][] trains,
        @NonNull AnalysisListener<int[]> listener) {
        super(filePath, listener);

        this.trains = trains;
    }

    @Nullable @Override int[][] process() throws Exception {
        long start = System.currentTimeMillis();
        int loopCounter = 0;

        float diff;
        int mainIndex, secIndex;
        int spikeCount;
        float[] train;

        int[] histogram = new int[(int) Math.ceil((MAX_TIME + BIN_SIZE) / BIN_SIZE)];
        final int[][] autoCorrelation = new int[trains.length][];
        for (int i = 0; i < trains.length; i++) {
            train = trains[i];
            spikeCount = train.length;

            Arrays.fill(histogram, 0);

            for (mainIndex = 0; mainIndex < spikeCount; mainIndex++) {
                // check on left of spike
                for (secIndex = mainIndex; secIndex >= 0; secIndex--) {
                    diff = train[mainIndex] - train[secIndex];
                    if (diff > MIN_EDGE && diff < MAX_EDGE) {
                        histogram[(int) (((diff - MIN_EDGE) / BIN_SIZE))]++;
                    } else {
                        break;
                    }
                }
                // check on right of spike
                for (secIndex = mainIndex + 1; secIndex < spikeCount; secIndex++) {
                    diff = train[mainIndex] - train[secIndex];
                    if (diff > MIN_EDGE && diff < MAX_EDGE) {
                        histogram[(int) (((diff - MIN_EDGE) / BIN_SIZE))]++;
                    } else {
                        break;
                    }
                }
            }

            int[] tmp = new int[histogram.length - 1]; // we are excluding 1st value
            System.arraycopy(histogram, 1, tmp, 0, histogram.length - 1);

            autoCorrelation[i] = tmp;

            LOGD(TAG, "LOOP END (" + ++loopCounter + "): " + (System.currentTimeMillis() - start));
        }

        return autoCorrelation;
    }
}
