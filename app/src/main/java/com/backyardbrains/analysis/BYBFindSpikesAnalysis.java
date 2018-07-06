package com.backyardbrains.analysis;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.audio.BYBAudioFile;
import com.backyardbrains.data.persistance.entity.Spike;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.JniUtils;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

class BYBFindSpikesAnalysis extends BYBBaseAnalysis<Spike> {

    private static final String TAG = makeLogTag(BYBFindSpikesAnalysis.class);

    private static final float BUFFER_SIZE_IN_SECS = 12f;
    private static final float MIN_VALID_FILE_LENGTH_IN_SECS = .2f;
    private static final float BIN_COUNT = 200f;

    private final BYBAudioFile audioFile;

    BYBFindSpikesAnalysis(@NonNull BYBAudioFile audioFile, @NonNull AnalysisListener<Spike> listener) {
        super(audioFile.getAbsolutePath(), listener);

        this.audioFile = audioFile;
    }

    @Nullable @Override public Spike[] process() throws Exception {
        final long totalSamples = AudioUtils.getSampleCount(audioFile.length());
        LOGD(TAG, "Audio file sample count is: " + totalSamples);

        int bufferSize = (int) Math.ceil(totalSamples / BIN_COUNT);
        final int maxBufferSize =
            (int) Math.ceil((audioFile.sampleRate() * BUFFER_SIZE_IN_SECS) / audioFile.numChannels());
        if (bufferSize > maxBufferSize) bufferSize = maxBufferSize;

        if (totalSamples < audioFile.sampleRate() * MIN_VALID_FILE_LENGTH_IN_SECS) {
            LOGD(TAG, "File to short! Don't process!");
            return new Spike[0];
        }

        long start = System.currentTimeMillis(); // for measuring execution time

        byte[] buffer = new byte[bufferSize * 2];
        ByteBuffer bb = ByteBuffer.allocate(buffer.length).order(ByteOrder.LITTLE_ENDIAN);
        short[] samples = new short[bufferSize];
        int readBytes, readSamples;

        // 1. FIRST LET'S FIND STANDARD DEVIATIONS FOR EVERY CHUNK
        final float[] standardDeviationsArr = new float[(int) Math.ceil((float) totalSamples / bufferSize)];
        int deviationCounter = 0;

        while ((readBytes = audioFile.read(buffer)) > 0) {
            readSamples = (int) (readBytes * .5f);
            bb.put(buffer, 0, readBytes);
            bb.clear();
            bb.asShortBuffer().get(samples, 0, readSamples);

            standardDeviationsArr[deviationCounter++] = JniUtils.calculateStandardDeviation(samples, readSamples);
        }
        LOGD(TAG, (System.currentTimeMillis() - start) + " - AFTER FINDING DEVIATIONS");
        LOGD(TAG, "DEVIATIONS COUNT: " + deviationCounter);

        // 2. SORT DEVIATIONS ASCENDING
        Arrays.sort(standardDeviationsArr);
        int i = 0;
        int j = standardDeviationsArr.length - 1;
        float tmp;
        while (j > i) {
            tmp = standardDeviationsArr[j];
            standardDeviationsArr[j] = standardDeviationsArr[i];
            standardDeviationsArr[i] = tmp;
            j--;
            i++;
        }
        LOGD(TAG, (System.currentTimeMillis() - start) + " - AFTER SORTING AND REVERSING DEVIATIONS");

        // 3. DETERMINE ACCEPTABLE SPIKE VALUES WHICH ARE VALUES GRATER THEN 40% OF SDTs MULTIPLIED BY 2
        float tmpSig = 2 * standardDeviationsArr[(int) Math.ceil(((float) standardDeviationsArr.length) * 0.4f)];
        //float sig = 2 * standardDeviations.get((int) Math.ceil(((float) standardDeviations.size()) * 0.4f));
        short sig = tmpSig > Short.MAX_VALUE ? Short.MAX_VALUE : (short) tmpSig;
        float tmpNegSig = -1 * sig; // we need it for negative values as well
        short negSig = tmpNegSig < Short.MIN_VALUE ? Short.MIN_VALUE : (short) tmpNegSig;
        LOGD(TAG, "SIG: " + sig + ", NEG_SIG: " + negSig);

        // go to beginning of the file cause we need to run through it again to find spikes
        audioFile.seek(0);

        // 4. NOW THAT WE HAVE BORDER VALUES LET'S FIND THE SPIKES IMPLEMENTING SCHMITT TRIGGER
        int peaksCounter = 0, peaksNegCounter = 0;
        int maxSpikes = (int) (totalSamples / 10);
        short[] valuesPos = new short[maxSpikes];
        int[] indicesPos = new int[maxSpikes];
        float[] timesPos = new float[maxSpikes];
        short[] valuesNeg = new short[maxSpikes];
        int[] indicesNeg = new int[maxSpikes];
        float[] timesNeg = new float[maxSpikes];

        bufferSize = maxBufferSize; // let's use max buffer size
        buffer = new byte[bufferSize * 2];
        bb = ByteBuffer.allocate(buffer.length).order(ByteOrder.LITTLE_ENDIAN);
        samples = new short[bufferSize];
        while ((readBytes = audioFile.read(buffer)) > 0) {
            readSamples = (int) (readBytes * .5f);
            bb.put(buffer, 0, readBytes);
            bb.clear();
            bb.asShortBuffer().get(samples, 0, readSamples);

            // find peaks
            int[] counts =
                JniUtils.findSpikes(samples, readSamples, audioFile.sampleRate(), valuesPos, indicesPos, timesPos,
                    peaksCounter, sig, valuesNeg, indicesNeg, timesNeg, peaksNegCounter, negSig, maxSpikes);
            peaksCounter += counts[0];
            peaksNegCounter += counts[1];
        }
        LOGD(TAG, (System.currentTimeMillis() - start) + " - AFTER FINDING SPIKES");
        LOGD(TAG, "FOUND POSITIVE: " + peaksCounter);
        LOGD(TAG, "FOUND NEGATIVE: " + peaksNegCounter);

        // 5. FINALLY WE SHOULD FILTER FOUND SPIKES BY APPLYING KILL INTERVAL OF 5ms
        int[] counts =
            JniUtils.filterSpikes(valuesPos, indicesPos, timesPos, peaksCounter, valuesNeg, indicesNeg, timesNeg,
                peaksNegCounter);
        int removedCounter = counts[0];
        int removedNegCounter = counts[1];

        LOGD(TAG, (System.currentTimeMillis() - start) + " - AFTER FILTERING SPIKES");
        LOGD(TAG, "FOUND POSITIVE: " + (peaksCounter - removedCounter));
        LOGD(TAG, "FOUND NEGATIVE: " + (peaksNegCounter - removedNegCounter));

        int posSize = peaksCounter - removedCounter;
        int negSize = peaksNegCounter - removedNegCounter;

        final Spike[] allSpikes = new Spike[posSize + negSize];
        int counter = 0;
        for (i = 0; i < posSize; i++) {
            allSpikes[counter++] = new Spike(valuesPos[i], indicesPos[i], timesPos[i]);
        }
        for (i = 0; i < negSize; i++) {
            allSpikes[counter++] = new Spike(valuesNeg[i], indicesNeg[i], timesNeg[i]);
        }

        LOGD(TAG, (System.currentTimeMillis() - start) + " - AFTER CREATING SPIKE OBJECTS");

        return allSpikes;
    }
}