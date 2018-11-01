package com.backyardbrains.analysis;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.dsp.audio.AudioFile;
import com.backyardbrains.db.entity.Spike;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.JniUtils;

import static com.backyardbrains.utils.LogUtils.makeLogTag;

class FindSpikesAnalysis extends BaseAnalysis<Spike> {

    private static final String TAG = makeLogTag(FindSpikesAnalysis.class);

    private final AudioFile audioFile;

    FindSpikesAnalysis(@NonNull AudioFile audioFile, @NonNull AnalysisListener<Spike> listener) {
        super(audioFile.getAbsolutePath(), listener);

        this.audioFile = audioFile;
    }

    @Nullable @Override public Spike[] process() throws Exception {
        final long totalSamples = AudioUtils.getSampleCount(audioFile.length());
        int maxSpikes = (int) (totalSamples / 10);
        short[] valuesPos = new short[maxSpikes];
        int[] indicesPos = new int[maxSpikes];
        float[] timesPos = new float[maxSpikes];
        short[] valuesNeg = new short[maxSpikes];
        int[] indicesNeg = new int[maxSpikes];
        float[] timesNeg = new float[maxSpikes];

        int[] counts =
            JniUtils.findSpikes(audioFile.getAbsolutePath(), valuesPos, indicesPos, timesPos, valuesNeg, indicesNeg,
                timesNeg, maxSpikes);
        int posSize = counts[0];
        int negSize = counts[1];

        final Spike[] allSpikes = new Spike[posSize + negSize];
        int counter = 0;
        for (int i = 0; i < posSize; i++) {
            allSpikes[counter++] = new Spike(valuesPos[i], indicesPos[i], timesPos[i]);
        }
        for (int i = 0; i < negSize; i++) {
            allSpikes[counter++] = new Spike(valuesNeg[i], indicesNeg[i], timesNeg[i]);
        }

        return allSpikes;
    }
}