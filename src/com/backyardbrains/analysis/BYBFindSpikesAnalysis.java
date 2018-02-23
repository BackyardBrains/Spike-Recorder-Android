package com.backyardbrains.analysis;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.audio.BYBAudioFile;
import com.backyardbrains.data.persistance.entity.Spike;
import com.backyardbrains.utils.AnalysisUtils;
import com.backyardbrains.utils.AudioUtils;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.Comparator;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

class BYBFindSpikesAnalysis extends BYBBaseAnalysis<Spike> {

    private static final String TAG = makeLogTag(BYBFindSpikesAnalysis.class);

    private static final float BUFFER_SIZE_IN_SECS = 12f;
    private static final float MIN_VALID_FILE_LENGTH_IN_SECS = .2f;
    private static final float BIN_COUNT = 200f;
    private static final int SCHMITT_ON = 1;
    private static final int SCHMITT_OFF = 2;

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

        // 1. FIRST LET'S FIND STANDARD DEVIATIONS FOR EVERY CHUNK
        final byte[] buffer = new byte[bufferSize * 2];
        final float[] standardDeviationsArr = new float[(int) Math.ceil((float) totalSamples / bufferSize)];
        //final ArrayList<Float> standardDeviations = new ArrayList<>();
        short[] samples;
        ShortBuffer sb;
        int deviationCounter = 0;
        while (audioFile.read(buffer) > 0) {
            sb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
            samples = new short[sb.capacity()];
            sb.get(samples);
            standardDeviationsArr[deviationCounter] = AnalysisUtils.STD(samples);
            deviationCounter++;
            //standardDeviations.add(AnalysisUtils.STD(shortBuffer));
        }
        LOGD(TAG, (System.currentTimeMillis() - start) + " - AFTER FINDING DEVIATIONS");

        // 2. SORT DEVIATIONS ASCENDING Arrays.sort(standardDeviationsArr, Collections.<Float>reverseOrder());
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
        //Collections.sort(standardDeviations);
        //Collections.reverse(standardDeviations);
        LOGD(TAG, (System.currentTimeMillis() - start) + " - AFTER SORTING AND REVERSING DEVIATIONS");

        // 3. DETERMINE ACCEPTABLE SPIKE VALUES WHICH ARE VALUES GRATER THEN 40% OF SDTs MULTIPLIED BY 2
        float tmpSig = 2 * standardDeviationsArr[(int) Math.ceil(((float) standardDeviationsArr.length) * 0.4f)];
        //float sig = 2 * standardDeviations.get((int) Math.ceil(((float) standardDeviations.size()) * 0.4f));
        short sig = tmpSig > Short.MAX_VALUE ? Short.MAX_VALUE : (short) tmpSig;
        float tmpNegSig = -1 * sig; // we need it for negative values as well
        short negSig = tmpNegSig < Short.MIN_VALUE ? Short.MIN_VALUE : (short) tmpNegSig;
        LOGD(TAG, "SIG: " + sig + ", NEG_SIG: " + negSig);

        bufferSize = maxBufferSize;

        int schmittPosState = SCHMITT_OFF;
        int schmittNegState = SCHMITT_OFF;
        short maxPeakValue = Short.MIN_VALUE;
        int maxPeakIndex = 0;
        short minPeakValue = Short.MAX_VALUE;
        int minPeakIndex = 0;
        float maxPeakTime = 0f;
        float minPeakTime = 0f;

        int peaksCounter = 0, peaksNegCounter = 0;
        int size = (int) (totalSamples / 10);
        short[] valuesPos = new short[size];
        int[] indicesPos = new int[size];
        float[] timesPos = new float[size];
        short[] valuesNeg = new short[size];
        int[] indicesNeg = new int[size];
        float[] timesNeg = new float[size];

        // go to beginning of the file cause we need to run through it again to find spikes
        audioFile.seek(0);

        // 4. NOW THAT WE HAVE BORDER VALUES LET'S FIND THE SPIKES IMPLEMENTING SCHMITT TRIGGER
        final float sampleRateDivider = (float) 1 / audioFile.sampleRate();
        float currentTime = 0f;
        int index = 0;
        short sample;
        int sampleCount;
        //int counter = 0;
        //long stopwatch;
        final byte[] buffer1 = new byte[bufferSize * 2];
        while (audioFile.read(buffer1) > 0) {
            sb = ByteBuffer.wrap(buffer1).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
            samples = new short[sb.capacity()];
            sb.get(samples);

            //LOGD(TAG, "SAMPLE COUNT: " + samples.length);
            //stopwatch = System.currentTimeMillis();

            // find peaks
            sampleCount = samples.length;
            //noinspection ForLoopReplaceableByForEach
            for (int k = 0; k < sampleCount; k++) {
                sample = samples[k];
                // determine state of positive schmitt trigger
                if (schmittPosState == SCHMITT_OFF) {
                    if (sample > sig) {
                        schmittPosState = SCHMITT_ON;
                        maxPeakValue = Short.MIN_VALUE;
                    }
                } else {
                    if (sample < 0) {
                        schmittPosState = SCHMITT_OFF;
                        valuesPos[peaksCounter] = maxPeakValue;
                        indicesPos[peaksCounter] = maxPeakIndex;
                        timesPos[peaksCounter] = maxPeakTime;
                        //spike = new Spike(maxPeakValue, maxPeakIndex, maxPeakTime);
                        //peaksIndexes.add(spike);
                        //peaksIndexesArr[peaksCounter] = spike;
                        peaksCounter++;
                    } else if (sample > maxPeakValue) {
                        maxPeakValue = sample;
                        maxPeakIndex = index;
                        maxPeakTime = currentTime;
                    }
                }

                // determine state of negative schmitt trigger
                if (schmittNegState == SCHMITT_OFF) {
                    if (sample < negSig) {
                        schmittNegState = SCHMITT_ON;
                        minPeakValue = Short.MAX_VALUE;
                    }
                } else {
                    if (sample > 0) {
                        schmittNegState = SCHMITT_OFF;
                        valuesNeg[peaksNegCounter] = minPeakValue;
                        indicesNeg[peaksNegCounter] = minPeakIndex;
                        timesNeg[peaksNegCounter] = minPeakTime;
                        //spike = new Spike(minPeakValue, minPeakIndex, minPeakTime);
                        //peaksIndexesNeg.add(spike);
                        //peaksIndexesNegArr[peaksNegCounter] = spike;
                        peaksNegCounter++;
                    } else if (sample < minPeakValue) {
                        minPeakValue = sample;
                        minPeakIndex = index;
                        minPeakTime = currentTime;
                    }
                }

                index++;
                currentTime += sampleRateDivider;
            }
            //LOGD(TAG, (System.currentTimeMillis() - stopwatch) + " - FINDING SPIKES LOOP #" + (++counter) + " ENDED");
            //LOGD(TAG, "==============================================");
        }
        LOGD(TAG, (System.currentTimeMillis() - start) + " - AFTER FINDING SPIKES");
        LOGD(TAG, "FOUND POSITIVE: " + peaksCounter);
        LOGD(TAG, "FOUND NEGATIVE: " + peaksNegCounter);

        final short[] tmpValuesPos = new short[peaksCounter];
        System.arraycopy(valuesPos, 0, tmpValuesPos, 0, tmpValuesPos.length);
        final int[] tmpIndicesPos = new int[peaksCounter];
        System.arraycopy(indicesPos, 0, tmpIndicesPos, 0, tmpIndicesPos.length);
        final float[] tmpTimesPos = new float[peaksCounter];
        System.arraycopy(timesPos, 0, tmpTimesPos, 0, tmpTimesPos.length);
        final short[] tmpValuesNeg = new short[peaksNegCounter];
        System.arraycopy(valuesNeg, 0, tmpValuesNeg, 0, tmpValuesNeg.length);
        final int[] tmpIndicesNeg = new int[peaksNegCounter];
        System.arraycopy(indicesNeg, 0, tmpIndicesNeg, 0, tmpIndicesNeg.length);
        final float[] tmpTimesNeg = new float[peaksNegCounter];
        System.arraycopy(timesNeg, 0, tmpTimesNeg, 0, tmpTimesNeg.length);

        // 5. FINALLY WE SHOULD FILTER FOUND SPIKES BY APPLYING KILL INTERVAL OF 5ms
        final float killInterval = 0.005f;// 5ms
        //if (peaksIndexes.size() > 0) { // Filter positive spikes using kill interval
        //    for (i = 0; i < peaksIndexes.size() - 1; i++) { // look on the right
        //        if (peaksIndexes.get(i).getValue() < peaksIndexes.get(i + 1).getValue()) {
        //            if ((peaksIndexes.get(i + 1).getTime() - peaksIndexes.get(i).getTime()) < killInterval) {
        //                peaksIndexes.remove(i);
        //                i--;
        //            }
        //        }
        //    }
        //    for (i = 1; i < peaksIndexes.size(); i++) { // look on the left neighbor
        //        if (peaksIndexes.get(i).getValue() < peaksIndexes.get(i - 1).getValue()) {
        //            if ((peaksIndexes.get(i).getTime() - peaksIndexes.get(i - 1).getTime()) < killInterval) {
        //                peaksIndexes.remove(i);
        //                i--;
        //            }
        //        }
        //    }
        //}
        //if (peaksIndexesNeg.size() > 0) { // Filter negative spikes using kill interval
        //    for (i = 0; i < peaksIndexesNeg.size() - 1; i++) { // look on the right
        //        if (peaksIndexesNeg.get(i).getValue() > peaksIndexesNeg.get(i + 1).getValue()) {
        //            if ((peaksIndexesNeg.get(i + 1).getTime() - peaksIndexesNeg.get(i).getTime()) < killInterval) {
        //                peaksIndexesNeg.remove(i);
        //                i--;
        //            }
        //        }
        //    }
        //    for (i = 1; i < peaksIndexesNeg.size(); i++) { // look on the left neighbor
        //        if (peaksIndexesNeg.get(i).getValue() > peaksIndexesNeg.get(i - 1).getValue()) {
        //            if ((peaksIndexesNeg.get(i).getTime() - peaksIndexesNeg.get(i - 1).getTime()) < killInterval) {
        //                peaksIndexesNeg.remove(i);
        //                i--;
        //            }
        //        }
        //    }
        //}
        int len = tmpValuesPos.length;
        int removedCounter = 0;
        if (len > 0) { // Filter positive spikes using kill interval
            for (i = 0; i < len - 1; i++) { // look on the right
                if (tmpValuesPos[i] < tmpValuesPos[i + 1]) {
                    if ((tmpTimesPos[i + 1] - tmpTimesPos[i]) < killInterval) {
                        int numMoved = len - i - 1;
                        if (numMoved > 0) {
                            System.arraycopy(tmpValuesPos, i + 1, tmpValuesPos, i, numMoved);
                            System.arraycopy(tmpIndicesPos, i + 1, tmpIndicesPos, i, numMoved);
                            System.arraycopy(tmpTimesPos, i + 1, tmpTimesPos, i, numMoved);
                        }
                        //peaksIndexesUnfilteredArr[--len] = null; // clear to let GC do its work
                        len--;
                        removedCounter++;
                        i--;
                    }
                }
            }
            len = i;
            for (i = 1; i < len; i++) { // look on the left neighbor
                if (tmpValuesPos[i] < tmpValuesPos[i - 1]) {
                    if ((tmpTimesPos[i] - tmpTimesPos[i - 1]) < killInterval) {
                        int numMoved = len - i - 1;
                        if (numMoved > 0) {
                            System.arraycopy(tmpValuesPos, i + 1, tmpValuesPos, i, numMoved);
                            System.arraycopy(tmpIndicesPos, i + 1, tmpIndicesPos, i, numMoved);
                            System.arraycopy(tmpTimesPos, i + 1, tmpTimesPos, i, numMoved);
                        }
                        //peaksIndexesUnfilteredArr[--len] = null; // clear to let GC do its work
                        len--;
                        removedCounter++;
                        i--;
                    }
                }
            }
        }
        len = tmpValuesNeg.length;
        int removedNegCounter = 0;
        if (len > 0) { // Filter negative spikes using kill interval
            for (i = 0; i < len - 1; i++) { // look on the right
                if (tmpValuesNeg[i] > tmpValuesNeg[i + 1]) {
                    if ((tmpTimesNeg[i + 1] - tmpTimesNeg[i]) < killInterval) {
                        int numMoved = len - i - 1;
                        if (numMoved > 0) {
                            System.arraycopy(tmpValuesNeg, i + 1, tmpValuesNeg, i, numMoved);
                            System.arraycopy(tmpIndicesNeg, i + 1, tmpIndicesNeg, i, numMoved);
                            System.arraycopy(tmpTimesNeg, i + 1, tmpTimesNeg, i, numMoved);
                        }
                        //peaksIndexesNegUnfilteredArr[--len] = null; // clear to let GC do its work
                        len--;
                        removedNegCounter++;
                        i--;
                    }
                }
            }
            len = i;
            for (i = 1; i < len; i++) { // look on the left neighbor
                if (tmpValuesNeg[i] > tmpValuesNeg[i - 1]) {
                    if ((tmpTimesNeg[i] - tmpTimesNeg[i - 1]) < killInterval) {
                        int numMoved = len - i - 1;
                        if (numMoved > 0) {
                            System.arraycopy(tmpValuesNeg, i + 1, tmpValuesNeg, i, numMoved);
                            System.arraycopy(tmpIndicesNeg, i + 1, tmpIndicesNeg, i, numMoved);
                            System.arraycopy(tmpTimesNeg, i + 1, tmpTimesNeg, i, numMoved);
                        }
                        //peaksIndexesNegUnfilteredArr[--len] = null; // clear to let GC do its work
                        len--;
                        removedNegCounter++;
                        i--;
                    }
                }
            }
        }
        LOGD(TAG, (System.currentTimeMillis() - start) + " - AFTER FILTERING SPIKES");
        LOGD(TAG, "FOUND POSITIVE: " + (peaksCounter - removedCounter));
        LOGD(TAG, "FOUND NEGATIVE: " + (peaksNegCounter - removedNegCounter));

        int posSize = peaksCounter - removedCounter;
        int negSize = peaksNegCounter - removedNegCounter;
        valuesPos = new short[posSize];
        System.arraycopy(tmpValuesPos, 0, valuesPos, 0, valuesPos.length);
        indicesPos = new int[posSize];
        System.arraycopy(tmpIndicesPos, 0, indicesPos, 0, indicesPos.length);
        timesPos = new float[posSize];
        System.arraycopy(tmpTimesPos, 0, timesPos, 0, timesPos.length);
        valuesNeg = new short[negSize];
        System.arraycopy(tmpValuesNeg, 0, valuesNeg, 0, valuesNeg.length);
        indicesNeg = new int[negSize];
        System.arraycopy(tmpIndicesNeg, 0, indicesNeg, 0, indicesNeg.length);
        timesNeg = new float[negSize];
        System.arraycopy(tmpTimesNeg, 0, timesNeg, 0, timesNeg.length);

        final Spike[] allSpikes = new Spike[posSize + negSize];
        int counter = 0;
        for (int k = 0; k < posSize; k++) {
            Spike spike = new Spike(valuesPos[k], indicesPos[k], tmpTimesPos[k]);
            allSpikes[counter] = spike;
            counter++;
        }
        for (int k = 0; k < negSize; k++) {
            Spike spike = new Spike(valuesNeg[k], indicesNeg[k], tmpTimesNeg[k]);
            allSpikes[counter] = spike;
            counter++;
        }

        LOGD(TAG, (System.currentTimeMillis() - start) + " - AFTER CREATING SPIKE OBJECTS");

        Arrays.sort(allSpikes, new Comparator<Spike>() {
            @Override public int compare(Spike s1, Spike s2) {
                return s1.getIndex() - s2.getIndex();
            }
        });

        LOGD(TAG, (System.currentTimeMillis() - start) + " - AFTER SORTING SPIKE OBJECTS");

        float highestPeak = Float.MIN_VALUE;
        float lowestPeak = Float.MAX_VALUE;
        for (int k = 0; k < len; k++) {
            if (allSpikes[k].getValue() > highestPeak) highestPeak = allSpikes[k].getValue();
            if (allSpikes[k].getValue() < lowestPeak) lowestPeak = allSpikes[k].getValue();
        }

        return allSpikes;

        //Spike[] allPeaksIndexes;
        //
        //int filteredPeaksIndexesSize = peaksIndexesUnfilteredArr.length - removedCounter;
        //int filteredPeaksIndexesNegSize = peaksIndexesNegUnfilteredArr.length - removedNegCounter;
        //int allPeaksIndexesSize = filteredPeaksIndexesSize + filteredPeaksIndexesNegSize;
        //if (allPeaksIndexesSize > 0) {
        //    allPeaksIndexes = new Spike[allPeaksIndexesSize];
        //    if (peaksCounter > 0) {
        //        System.arraycopy(peaksIndexesUnfilteredArr, 0, allPeaksIndexes, 0, filteredPeaksIndexesSize);
        //    }
        //    if (peaksNegCounter > 0) {
        //        System.arraycopy(peaksIndexesNegUnfilteredArr, 0, allPeaksIndexes, filteredPeaksIndexesSize,
        //            filteredPeaksIndexesNegSize);
        //    }
        //    Arrays.sort(allPeaksIndexes, new Comparator<Spike>() {
        //        @Override public int compare(Spike s1, Spike s2) {
        //            return s1.getIndex() - s2.getIndex();
        //        }
        //    });
        //} else {
        //    allPeaksIndexes = new Spike[0];
        //}
        //
        ////peaksIndexes.addAll(peaksIndexesNeg);
        ////Collections.sort(peaksIndexes, new Comparator<Spike>() {
        ////    @Override public int compare(Spike s1, Spike s2) {
        ////        return s1.getIndex() - s2.getIndex();
        ////    }
        ////});
        //
        ////int len = peaksIndexes.size();
        ////final Spike[] spikes = new Spike[len];
        ////for (int j = 0; j < len; j++)
        ////    spikes[j] = peaksIndexes.get(j);
        //
        //float highestPeak = Float.MIN_VALUE;
        //float lowestPeak = Float.MAX_VALUE;
        //for (int k = 0; k < len; k++) {
        //    if (allPeaksIndexes[k].getValue() > highestPeak) highestPeak = allPeaksIndexes[k].getValue();
        //    if (allPeaksIndexes[k].getValue() < lowestPeak) lowestPeak = allPeaksIndexes[k].getValue();
        //}
        //
        //return allPeaksIndexes;
    }
}