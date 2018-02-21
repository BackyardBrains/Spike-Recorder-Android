package com.backyardbrains.analysis;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.util.Arrays;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

class BYBCrossCorrelationAnalysis extends BYBBaseAnalysis<int[]> {

    private static final String TAG = makeLogTag(BYBCrossCorrelationAnalysis.class);

    private static final float MAX_TIME = 0.1f;
    private static final float BIN_SIZE = 0.001f;
    private static final float MIN_EDGE = -MAX_TIME - BIN_SIZE * 0.5f;
    private static final float MAX_EDGE = MAX_TIME + BIN_SIZE * 0.5f;
    private static final float DIVIDER = 1 / BIN_SIZE;

    private final float[][] trains;

    BYBCrossCorrelationAnalysis(@NonNull String filePath, @NonNull float[][] trains,
        @NonNull AnalysisListener<int[]> listener) {
        super(filePath, listener);

        this.trains = trains;
    }

    @Nullable @Override int[][] process() {
        long start = System.currentTimeMillis();
        int loopCounter = 0;

        float diff;
        int firstIndex, secondIndex;
        int firstSpikeCount, secondSpikeCount;
        float[] firstTrain, secondTrain;
        int[] temp;

        int[] histogram = new int[(int) Math.ceil((2 * MAX_TIME + BIN_SIZE) / BIN_SIZE)];
        int trainCount = trains.length;
        int[][] crossCorrelation = new int[trainCount * trainCount][];
        for (int i = 0; i < trainCount; i++) {
            for (int j = 0; j < trainCount; j++) {
                firstTrain = trains[i];
                firstSpikeCount = firstTrain.length;
                secondTrain = trains[j];
                secondSpikeCount = secondTrain.length;

                temp = new int[histogram.length];
                if (firstSpikeCount > 1 && secondSpikeCount > 1) {
                    Arrays.fill(histogram, 0);

                    boolean insideInterval;
                    // go through first spike train
                    for (firstIndex = 0; firstIndex < firstSpikeCount; firstIndex++) {
                        // check on left of spike
                        insideInterval = false;
                        // go through second spike train
                        for (secondIndex = 0; secondIndex < secondSpikeCount; secondIndex++) {
                            diff = firstTrain[firstIndex] - secondTrain[secondIndex];
                            if (diff > MIN_EDGE && diff < MAX_EDGE) {
                                insideInterval = true;
                                histogram[(int) (((diff - MIN_EDGE) * DIVIDER))]++;
                            } else if (insideInterval) { //we pass last spike that is in interval of interest
                                break;
                            }
                        }
                    }

                    System.arraycopy(histogram, 0, temp, 0, histogram.length);
                }

                crossCorrelation[i * trainCount + j] = temp;

                LOGD(TAG, "LOOP END (" + ++loopCounter + "): " + toSecs(start));
            }
        }

        return crossCorrelation;
    }

    private long toSecs(long millis) {
        return System.currentTimeMillis() - millis;
    }
}
