package com.backyardbrains.analysis;

import android.support.annotation.NonNull;
import com.backyardbrains.audio.BYBAudioFile;
import com.backyardbrains.utils.AnalysisUtils;
import com.backyardbrains.utils.AudioUtils;
import com.crashlytics.android.Crashlytics;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static android.R.attr.duration;
import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.LOGE;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

class BYBFindSpikesAnalysis extends BYBBaseAnalysis {

    private static final String TAG = makeLogTag(BYBFindSpikesAnalysis.class);

    private static final int SCHMITT_ON = 1;
    private static final int SCHMITT_OFF = 2;
    private static final int MIN_TOTAL_SAMPLES = (int) (AudioUtils.SAMPLE_RATE * 0.2);

    private final ArrayList<BYBSpike> allSpikes = new ArrayList<>();
    private final BYBAudioFile audioFile;
    private final int bufferSize;

    private float highestPeak = 0;
    private float lowestPeak = 0;
    private long totalSamples;

    BYBFindSpikesAnalysis(@NonNull BYBAudioFile audioFile, @NonNull AnalysisListener listener) {
        super(listener);

        this.audioFile = audioFile;
        this.bufferSize = AudioUtils.OUT_BUFFER_SIZE;

        execute();
    }

    float getHighestPeak() {
        return highestPeak;
    }

    float getLowestPeak() {
        return lowestPeak;
    }

    long getTotalSamples() {
        return totalSamples;
    }

    BYBSpike[] getSpikes() {
        return allSpikes.toArray(new BYBSpike[allSpikes.size()]);
    }

    @Override public void process() {
        try {
            totalSamples = AudioUtils.getSampleCount(audioFile.length());
            LOGD(TAG, "Audio file byte count is: " + duration);

            if (totalSamples < MIN_TOTAL_SAMPLES) {
                LOGD(TAG, "File to short! Don't process!");
                return;
            }

            long start = System.currentTimeMillis(); // for measuring execution time

            // 1. FIRST LET'S FIND STANDARD DEVIATIONS FOR EVERY CHUNK
            final byte[] buffer = new byte[bufferSize];
            final ArrayList<Float> standardDeviations = new ArrayList<>();
            short[] shortBuffer;
            ShortBuffer sb;
            while (audioFile.read(buffer) > 0) {
                sb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
                shortBuffer = new short[sb.capacity()];
                sb.get(shortBuffer);
                standardDeviations.add(AnalysisUtils.STD(shortBuffer, 0, shortBuffer.length));
            }
            LOGD(TAG, (System.currentTimeMillis() - start) + " - AFTER FINDING DEVIATIONS");

            // 2. SORT DEVIATIONS ASCENDING
            Collections.sort(standardDeviations);
            Collections.reverse(standardDeviations);
            LOGD(TAG, (System.currentTimeMillis() - start) + " - AFTER SORTING AND REVERSING DEVIATIONS");

            // 3. DETERMINE ACCEPTABLE SPIKE VALUES WHICH ARE VALUES GRATER THEN 40% OF SDTs MULTIPLIED BY 2
            float sig = 2 * standardDeviations.get((int) Math.ceil(((float) standardDeviations.size()) * 0.4f));
            float negSig = -1 * sig; // we need it for negative values as well

            int schmittPosState = SCHMITT_OFF;
            int schmittNegState = SCHMITT_OFF;
            float maxPeakValue = Float.MIN_VALUE;
            int maxPeakIndex = 0;
            float minPeakValue = Float.MAX_VALUE;
            int minPeakIndex = 0;

            ArrayList<BYBSpike> peaksIndexes = new ArrayList<>();
            ArrayList<BYBSpike> peaksIndexesNeg = new ArrayList<>();

            // go to beginning of the file cause we need to run through it again to find spikes
            audioFile.seek(0);

            // 4. NOW THAT WE HAVE BORDER VALUES LET'S FIND THE SPIKES IMPLEMENTING SCHMITT TRIGGER
            int index = 0;
            while (audioFile.read(buffer) > 0) {
                sb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
                shortBuffer = new short[sb.capacity()];
                sb.get(shortBuffer);

                // find peaks
                for (short aShortBuffer : shortBuffer) {
                    // determine state of positive schmitt trigger
                    if (schmittPosState == SCHMITT_OFF && aShortBuffer > sig) {
                        schmittPosState = SCHMITT_ON;
                        maxPeakValue = Float.MIN_VALUE;
                    } else if (schmittPosState == SCHMITT_ON && aShortBuffer < 0) {
                        schmittPosState = SCHMITT_OFF;
                        peaksIndexes.add(
                            new BYBSpike(maxPeakValue, maxPeakIndex, ((float) maxPeakIndex) / audioFile.sampleRate()));
                    }

                    // determine state of negative schmitt trigger
                    if (schmittNegState == SCHMITT_OFF && aShortBuffer < negSig) {
                        schmittNegState = SCHMITT_ON;
                        minPeakValue = Float.MAX_VALUE;
                    } else if (schmittNegState == SCHMITT_ON && aShortBuffer > 0) {
                        schmittNegState = SCHMITT_OFF;
                        peaksIndexesNeg.add(
                            new BYBSpike(minPeakValue, minPeakIndex, ((float) minPeakIndex) / audioFile.sampleRate()));
                    }

                    // find max in positive peak
                    if (schmittPosState == SCHMITT_ON && aShortBuffer > maxPeakValue) {
                        maxPeakValue = aShortBuffer;
                        maxPeakIndex = index;
                    }

                    // find min in negative peak
                    else if (schmittNegState == SCHMITT_ON && aShortBuffer < minPeakValue) {
                        minPeakValue = aShortBuffer;
                        minPeakIndex = index;
                    }

                    index++;
                }
            }
            LOGD(TAG, (System.currentTimeMillis() - start) + " - AFTER FINDING SPIKES");

            // 5. FINALLY WE SHOULD FILTER FOUND SPIKES BY APPLYING KILL INTERVAL OF 5ms
            int i;
            final float killInterval = 0.005f;// 5ms
            if (peaksIndexes.size() > 0) { // Filter positive spikes using kill interval
                for (i = 0; i < peaksIndexes.size() - 1; i++) { // look on the right
                    if (peaksIndexes.get(i).value < peaksIndexes.get(i + 1).value) {
                        if ((peaksIndexes.get(i + 1).time - peaksIndexes.get(i).time) < killInterval) {
                            peaksIndexes.remove(i);
                            i--;
                        }
                    }
                }
                for (i = 1; i < peaksIndexes.size(); i++) { // look on the left neighbor
                    if (peaksIndexes.get(i).value < peaksIndexes.get(i - 1).value) {
                        if ((peaksIndexes.get(i).time - peaksIndexes.get(i - 1).time) < killInterval) {
                            peaksIndexes.remove(i);
                            i--;
                        }
                    }
                }
            }
            if (peaksIndexesNeg.size() > 0) { // Filter positive spikes using kill interval
                for (i = 0; i < peaksIndexesNeg.size() - 1; i++) { // look on the right
                    if (peaksIndexesNeg.get(i).value > peaksIndexesNeg.get(i + 1).value) {
                        if ((peaksIndexesNeg.get(i + 1).time - peaksIndexesNeg.get(i).time) < killInterval) {
                            peaksIndexesNeg.remove(i);
                            i--;
                        }
                    }
                }
                for (i = 1; i < peaksIndexesNeg.size(); i++) { // look on the left neighbor
                    if (peaksIndexesNeg.get(i).value > peaksIndexesNeg.get(i - 1).value) {
                        if ((peaksIndexesNeg.get(i).time - peaksIndexesNeg.get(i - 1).time) < killInterval) {
                            peaksIndexesNeg.remove(i);
                            i--;
                        }
                    }
                }
            }
            LOGD(TAG, (System.currentTimeMillis() - start) + " - AFTER FILTERING SPIKES");

            peaksIndexes.addAll(peaksIndexesNeg);
            Collections.sort(peaksIndexes, new Comparator<BYBSpike>() {
                @Override public int compare(BYBSpike o1, BYBSpike o2) {
                    return o1.index - o2.index;
                }
            });

            allSpikes.addAll(peaksIndexes);

            highestPeak = Float.MIN_VALUE;
            lowestPeak = Float.MAX_VALUE;
            for (int k = 0; k < allSpikes.size(); k++) {
                if (allSpikes.get(k).value > highestPeak) highestPeak = allSpikes.get(k).value;
                if (allSpikes.get(k).value < lowestPeak) lowestPeak = allSpikes.get(k).value;
            }
        } catch (IOException e) {
            LOGE(TAG,
                e instanceof FileNotFoundException ? "Error loading file" : "Error reading random access file stream",
                e);
            Crashlytics.logException(e);
        }
    }
}