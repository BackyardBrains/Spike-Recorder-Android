package com.backyardbrains.analysis;

import android.support.annotation.NonNull;
import com.backyardbrains.audio.BYBAudioFile;
import com.backyardbrains.utils.BYBUtils;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.List;

import static com.backyardbrains.utils.LogUtils.LOGE;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

class BYBAverageSpikeAnalysis extends BYBBaseAnalysis {

    private static final String TAG = makeLogTag(BYBAverageSpikeAnalysis.class);

    private static final float AVERAGE_SPIKE_HALF_LENGTH_SECONDS = 0.002f;

    private final BYBAudioFile audioFile;
    private final List<List<BYBSpike>> trains;

    private BYBAverageSpike[] avr;

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

    BYBAverageSpikeAnalysis(@NonNull BYBAudioFile audioFile, @NonNull List<List<BYBSpike>> trains,
        @NonNull AnalysisListener listener) {
        super(listener);

        this.audioFile = audioFile;
        this.trains = trains;

        execute();
    }

    BYBAverageSpike[] getAverageSpikes() {
        return avr;
    }

    @Override void process() {
        try {
            long numberOfSamples = audioFile.length();
            int sampleRate = audioFile.sampleRate();
            int halfSpikeLength = (int) (sampleRate * AVERAGE_SPIKE_HALF_LENGTH_SECONDS);
            int spikeLength = 2 * halfSpikeLength + 1;

            final AverageSpikeData[] tmpAvr = new AverageSpikeData[trains.size()];
            for (int i = 0; i < trains.size(); i++) {
                tmpAvr[i] = new AverageSpikeData();

                tmpAvr[i].averageSpike = new float[spikeLength];
                tmpAvr[i].topSTDLine = new float[spikeLength];
                tmpAvr[i].bottomSTDLine = new float[spikeLength];

                tmpAvr[i].normAverageSpike = new float[spikeLength];
                tmpAvr[i].normTopSTDLine = new float[spikeLength];
                tmpAvr[i].normBottomSTDLine = new float[spikeLength];

                tmpAvr[i].numberOfSamplesInData = spikeLength;
                tmpAvr[i].samplingRate = sampleRate;
                tmpAvr[i].countOfSpikes = 0;
            }

            // let's rewind the audio file so we can read from it
            audioFile.seek(0);

            List<BYBSpike> tempSpikeTrain;
            for (int spikeTrainIndex = 0; spikeTrainIndex < trains.size(); spikeTrainIndex++) {
                tempSpikeTrain = trains.get(spikeTrainIndex);
                for (int spikeIndex = 0; spikeIndex < tempSpikeTrain.size(); spikeIndex++) {
                    int spikeSampleIndex = tempSpikeTrain.get(spikeIndex).index;
                    // if our spike is outside current batch of samples
                    // go to next channel
                    if ((spikeSampleIndex + halfSpikeLength) >= numberOfSamples
                        || spikeSampleIndex - halfSpikeLength < 0) {
                        break;
                    }
                    // add spike to average buffer
                    final int averageSpikeIndexBase = spikeSampleIndex - halfSpikeLength;
                    for (int i = 0; i < spikeLength; i++) {
                        audioFile.seek((averageSpikeIndexBase + i) * 2);
                        final byte[] buffer = new byte[2];
                        short[] shortBuffer;
                        ShortBuffer sb;
                        short value = 0;
                        if (audioFile.read(buffer) > 0) {
                            sb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
                            shortBuffer = new short[sb.capacity()];
                            sb.get(shortBuffer);
                            if (shortBuffer.length > 0) value = shortBuffer[0];
                        }
                        tmpAvr[spikeTrainIndex].averageSpike[i] += value;
                        tmpAvr[spikeTrainIndex].topSTDLine[i] += Math.pow(value, 2);
                    }
                    tmpAvr[spikeTrainIndex].countOfSpikes++;
                }
            }

            // divide sum of spikes with number of spikes
            // and find max and min
            for (int i = 0; i < trains.size(); i++) {
                if (tmpAvr[i].countOfSpikes > 1) {
                    float divider = (float) tmpAvr[i].countOfSpikes;
                    float mn = Float.MAX_VALUE;
                    float mx = Float.MIN_VALUE;
                    for (int j = 0; j < spikeLength; j++) {
                        tmpAvr[i].averageSpike[j] /= divider;
                        if (tmpAvr[i].averageSpike[j] > mx) mx = tmpAvr[i].averageSpike[j];
                        if (tmpAvr[i].averageSpike[j] < mn) mn = tmpAvr[i].averageSpike[j];
                    }
                    tmpAvr[i].maxAverageSpike = mx;
                    tmpAvr[i].minAverageSpike = mn;

                    float[] temp = new float[spikeLength];
                    for (int j = 0; j < spikeLength; j++) {
                        tmpAvr[i].topSTDLine[j] /= divider;
                        temp[j] = tmpAvr[i].averageSpike[j] * tmpAvr[i].averageSpike[j];
                    }
                    for (int j = 0; j < spikeLength; j++) {
                        temp[j] = tmpAvr[i].topSTDLine[j] - temp[j];
                    }

                    // calculate STD from variance
                    for (int k = 0; k < spikeLength; k++) {
                        temp[k] = (float) Math.sqrt(temp[k]);
                    }

                    // Make top line and bottom line around mean that
                    // represent one STD deviation from mean
                    for (int j = 0; j < spikeLength; j++) {
                        tmpAvr[i].bottomSTDLine[j] = tmpAvr[i].averageSpike[j] - temp[j];
                        tmpAvr[i].topSTDLine[j] = tmpAvr[i].averageSpike[j] + temp[j];
                    }

                    // Find max and min of top and bottom std line respectively
                    tmpAvr[i].minStd = Float.MAX_VALUE;
                    tmpAvr[i].maxStd = Float.MIN_VALUE;
                    for (int j = 0; j < spikeLength; j++) {
                        if (tmpAvr[i].maxStd < tmpAvr[i].topSTDLine[j]) tmpAvr[i].maxStd = tmpAvr[i].topSTDLine[j];
                        if (tmpAvr[i].minStd > tmpAvr[i].bottomSTDLine[j]) {
                            tmpAvr[i].minStd = tmpAvr[i].bottomSTDLine[j];
                        }
                    }
                }
                float mn = Math.min(tmpAvr[i].minStd, tmpAvr[i].minAverageSpike);
                float mx = Math.max(tmpAvr[i].maxStd, tmpAvr[i].maxAverageSpike);
                for (int j = 0; j < spikeLength; j++) {
                    tmpAvr[i].normAverageSpike[j] = BYBUtils.map(tmpAvr[i].averageSpike[j], mn, mx, 0.0f, 1.0f);
                    tmpAvr[i].normTopSTDLine[j] = BYBUtils.map(tmpAvr[i].topSTDLine[j], mn, mx, 0.0f, 1.0f);
                    tmpAvr[i].normBottomSTDLine[j] = BYBUtils.map(tmpAvr[i].bottomSTDLine[j], mn, mx, 0.0f, 1.0f);
                }
            }

            // let's populate avr array
            clearAverageSpike();
            avr = new BYBAverageSpike[tmpAvr.length];

            int count = 0;
            for (AverageSpikeData asd : tmpAvr) {
                avr[count++] =
                    new BYBAverageSpike(asd.averageSpike, asd.normAverageSpike, asd.normTopSTDLine, asd.normBottomSTDLine);
            }
        } catch (IOException e) {
            LOGE(TAG,
                e instanceof FileNotFoundException ? "Error loading file" : "Error reading random access file stream",
                e);
        }
    }

    private void clearAverageSpike() {
        if (avr != null) {
            for (int i = 0; i < avr.length; i++) {
                avr[i] = null;
            }
            avr = null;
        }
    }
}
