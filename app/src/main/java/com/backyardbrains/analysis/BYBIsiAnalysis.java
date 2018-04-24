package com.backyardbrains.analysis;

import android.support.annotation.NonNull;
import com.backyardbrains.data.InterSpikeInterval;
import com.backyardbrains.utils.BYBUtils;
import java.util.Arrays;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

class BYBIsiAnalysis extends BYBBaseAnalysis<InterSpikeInterval[]> {

    private static final String TAG = makeLogTag(BYBIsiAnalysis.class);

    private static final int BIN_COUNT = 100;

    private final float[][] trains;

    BYBIsiAnalysis(@NonNull String filePath, @NonNull float[][] trains,
        @NonNull AnalysisListener<InterSpikeInterval[]> listener) {
        super(filePath, listener);

        this.trains = trains;
    }

    @Override InterSpikeInterval[][] process() {
        long start = System.currentTimeMillis();
        int loopCounter = 0;

        final float[] logSpace = BYBUtils.generateLogSpace(-3, 1, BIN_COUNT - 1);

        float diff;
        float[] train;
        int spikesCount;

        final int[] histogram = new int[BIN_COUNT];
        final InterSpikeInterval[][] isi = new InterSpikeInterval[trains.length][];
        for (int k = 0; k < trains.length; k++) {
            train = trains[k];
            spikesCount = train.length;

            Arrays.fill(histogram, 0);

            for (int i = 1; i < spikesCount; i++) {
                diff = train[i] - train[i - 1];
                for (int j = 1; j < BIN_COUNT; j++) {
                    if (diff >= logSpace[j - 1] && diff < logSpace[j]) {
                        histogram[j - 1]++;
                        break;
                    }
                }
            }

            InterSpikeInterval[] tmp = new InterSpikeInterval[BIN_COUNT];
            for (int i = 0; i < BIN_COUNT; i++) {
                tmp[i] = new InterSpikeInterval(logSpace[i], histogram[i]);
            }

            isi[k] = tmp;

            LOGD(TAG, "LOOP END (" + ++loopCounter + "): " + (System.currentTimeMillis() - start));
        }

        return isi;
    }
}