package com.backyardbrains.analysis;

import android.support.annotation.NonNull;
import com.backyardbrains.audio.BYBAudioFile;
import com.backyardbrains.data.AverageSpike;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.BYBUtils;
import com.crashlytics.android.Crashlytics;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.LOGE;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

class BYBAverageSpikeAnalysis extends BYBBaseAnalysis<AverageSpike> {

    private static final String TAG = makeLogTag(BYBAverageSpikeAnalysis.class);

    private static final float BATCH_SPIKE_HALF_IN_SECS = 0.002f;

    private final BYBAudioFile audioFile;
    private final int[][] trains;

    private final class AverageSpikeData {
        float[] averageSpike;
        float[] normAverageSpike;
        float maxAverageSpike;
        float minAverageSpike;

        float[] topSTDLine;
        float[] bottomSTDLine;
        float[] normTopSTDLine;
        float[] normBottomSTDLine;
        float maxStd;
        float minStd;

        int numberOfSamplesInData;
        float samplingRate;
        int countOfSpikes;
    }

    BYBAverageSpikeAnalysis(@NonNull BYBAudioFile audioFile, @NonNull int[][] trains,
        @NonNull AnalysisListener<AverageSpike> listener) {
        super(audioFile.getAbsolutePath(), listener);

        this.audioFile = audioFile;
        this.trains = trains;
    }

    @Override AverageSpike[] process() {
        long start = System.currentTimeMillis();
        int loopCounter = 0;

        try {
            final long totalSamples = AudioUtils.getSampleCount(audioFile.length());
            final int sampleRate = audioFile.sampleRate();
            final int batchSpikeHalfCount = (int) (sampleRate * BATCH_SPIKE_HALF_IN_SECS);
            final int batchSpikeCount = 2 * batchSpikeHalfCount + 1;
            final int batchSpikeCountInBytes = batchSpikeCount * 2;

            final int trainCount = trains.length;
            final AverageSpikeData[] tmpAvr = new AverageSpikeData[trainCount];
            for (int i = 0; i < trainCount; i++) {
                tmpAvr[i] = new AverageSpikeData();

                tmpAvr[i].averageSpike = new float[batchSpikeCount];
                tmpAvr[i].topSTDLine = new float[batchSpikeCount];
                tmpAvr[i].bottomSTDLine = new float[batchSpikeCount];

                tmpAvr[i].normAverageSpike = new float[batchSpikeCount];
                tmpAvr[i].normTopSTDLine = new float[batchSpikeCount];
                tmpAvr[i].normBottomSTDLine = new float[batchSpikeCount];

                tmpAvr[i].numberOfSamplesInData = batchSpikeCount;
                tmpAvr[i].samplingRate = sampleRate;
                tmpAvr[i].countOfSpikes = 0;
            }

            LOGD(TAG, (System.currentTimeMillis() - start) + " - AFTER PREPARING 4ms ARRAYS");

            int[] train;
            int spikeCount, sampleIndex, spikeIndexBatchHead;
            short[] spikeIndexBatch;
            byte[] buffer;
            ShortBuffer sb;

            for (int i = 0; i < trainCount; i++) {
                train = trains[i];
                spikeCount = train.length;

                for (int j = 0; j < spikeCount; j++) {
                    sampleIndex = train[j];
                    // if we cannot make a batch of 4ms go to next sample
                    if ((sampleIndex + batchSpikeHalfCount) >= totalSamples || sampleIndex - batchSpikeHalfCount < 0) {
                        continue;
                    }

                    // add spike to average buffer
                    spikeIndexBatchHead = sampleIndex - batchSpikeHalfCount;
                    buffer = new byte[batchSpikeCountInBytes];
                    audioFile.seek(spikeIndexBatchHead * 2);
                    if (audioFile.read(buffer) > 0) {
                        sb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
                        spikeIndexBatch = new short[sb.capacity()];
                        sb.get(spikeIndexBatch);
                        for (int k = 0; k < spikeIndexBatch.length; k++) {
                            tmpAvr[i].averageSpike[k] += spikeIndexBatch[k];
                            tmpAvr[i].topSTDLine[k] += Math.pow(spikeIndexBatch[k], 2);
                        }
                    }
                    tmpAvr[i].countOfSpikes++;
                }
            }

            LOGD(TAG, (System.currentTimeMillis() - start) + " - AFTER FILLING 4ms ARRAYS");

            float min, max, divider;
            float[] tmp;
            // divide sum of spikes with number of spikes and find max and min
            for (int i = 0; i < trainCount; i++) {
                if (tmpAvr[i].countOfSpikes > 1) {
                    divider = (float) tmpAvr[i].countOfSpikes;
                    min = Float.MAX_VALUE;
                    max = Float.MIN_VALUE;
                    for (int j = 0; j < batchSpikeCount; j++) {
                        tmpAvr[i].averageSpike[j] /= divider;
                        if (tmpAvr[i].averageSpike[j] > max) max = tmpAvr[i].averageSpike[j];
                        if (tmpAvr[i].averageSpike[j] < min) min = tmpAvr[i].averageSpike[j];
                    }
                    tmpAvr[i].maxAverageSpike = max;
                    tmpAvr[i].minAverageSpike = min;

                    tmp = new float[batchSpikeCount];
                    for (int j = 0; j < batchSpikeCount; j++) {
                        tmpAvr[i].topSTDLine[j] /= divider;
                        tmp[j] = tmpAvr[i].averageSpike[j] * tmpAvr[i].averageSpike[j];
                    }
                    for (int j = 0; j < batchSpikeCount; j++) {
                        tmp[j] = tmpAvr[i].topSTDLine[j] - tmp[j];
                    }

                    // calculate SD from variance
                    for (int k = 0; k < batchSpikeCount; k++) {
                        tmp[k] = (float) Math.sqrt(tmp[k]);
                    }

                    // Make top line and bottom line around mean that represent one SD deviation from mean
                    for (int j = 0; j < batchSpikeCount; j++) {
                        tmpAvr[i].bottomSTDLine[j] = tmpAvr[i].averageSpike[j] - tmp[j];
                        tmpAvr[i].topSTDLine[j] = tmpAvr[i].averageSpike[j] + tmp[j];
                    }

                    // Find max and min of top and bottom std line respectively
                    tmpAvr[i].minStd = Float.MAX_VALUE;
                    tmpAvr[i].maxStd = Float.MIN_VALUE;
                    for (int j = 0; j < batchSpikeCount; j++) {
                        if (tmpAvr[i].maxStd < tmpAvr[i].topSTDLine[j]) tmpAvr[i].maxStd = tmpAvr[i].topSTDLine[j];
                        if (tmpAvr[i].minStd > tmpAvr[i].bottomSTDLine[j]) {
                            tmpAvr[i].minStd = tmpAvr[i].bottomSTDLine[j];
                        }
                    }
                }
                min = Math.min(tmpAvr[i].minStd, tmpAvr[i].minAverageSpike);
                max = Math.max(tmpAvr[i].maxStd, tmpAvr[i].maxAverageSpike);
                for (int j = 0; j < batchSpikeCount; j++) {
                    tmpAvr[i].normAverageSpike[j] = BYBUtils.map(tmpAvr[i].averageSpike[j], min, max, 0.0f, 1.0f);
                    tmpAvr[i].normTopSTDLine[j] = BYBUtils.map(tmpAvr[i].topSTDLine[j], min, max, 0.0f, 1.0f);
                    tmpAvr[i].normBottomSTDLine[j] = BYBUtils.map(tmpAvr[i].bottomSTDLine[j], min, max, 0.0f, 1.0f);
                }

                LOGD(TAG, "LOOP END (" + ++loopCounter + "): " + (System.currentTimeMillis() - start));
            }

            // let's populate avr array
            final AverageSpike[] averageSpikes = new AverageSpike[tmpAvr.length];

            AverageSpikeData avrSpikeData;
            for (int i = 0; i < trainCount; i++) {
                avrSpikeData = tmpAvr[i];
                averageSpikes[i] = new AverageSpike(avrSpikeData.averageSpike, avrSpikeData.normAverageSpike,
                    avrSpikeData.normTopSTDLine, avrSpikeData.normBottomSTDLine);
            }

            return averageSpikes;
        } catch (IOException e) {
            LOGE(TAG,
                e instanceof FileNotFoundException ? "Error loading file" : "Error reading random access file stream",
                e);
            Crashlytics.logException(e);

            return new AverageSpike[0];
        }
    }
}
