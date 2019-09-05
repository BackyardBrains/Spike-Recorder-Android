package com.backyardbrains.analysis;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.backyardbrains.db.entity.Spike;
import com.backyardbrains.dsp.audio.AudioFile;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.JniUtils;
import java.util.ArrayList;
import java.util.List;

import static com.backyardbrains.utils.LogUtils.makeLogTag;

class FindSpikesAnalysis extends BaseAnalysis<Void, Spike[]> {

    private static final String TAG = makeLogTag(FindSpikesAnalysis.class);

    private final AudioFile audioFile;

    FindSpikesAnalysis(@NonNull AudioFile audioFile, @NonNull AnalysisListener<Spike[]> listener) {
        super(audioFile.getAbsolutePath(), listener);

        this.audioFile = audioFile;
    }

    @Nullable @Override public Spike[] process(Void... params) throws Exception {
        final long totalSamples = AudioUtils.getSampleCount(audioFile.length(), audioFile.bitsPerSample());
        final int channelCount = audioFile.channelCount();
        int maxSpikes = (int) (totalSamples / 10);

        short[][] valuesPos = new short[channelCount][maxSpikes];
        int[][] indicesPos = new int[channelCount][maxSpikes];
        float[][] timesPos = new float[channelCount][maxSpikes];
        short[][] valuesNeg = new short[channelCount][maxSpikes];
        int[][] indicesNeg = new int[channelCount][maxSpikes];
        float[][] timesNeg = new float[channelCount][maxSpikes];

        int[][] counts =
            JniUtils.findSpikes(audioFile.getAbsolutePath(), valuesPos, indicesPos, timesPos, valuesNeg, indicesNeg,
                timesNeg, channelCount, maxSpikes);

        List<Spike> spikeList = new ArrayList<>();
        for (int channel = 0; channel < channelCount; channel++) {
            for (int i = 0; i < counts[channel][0]; i++) {
                spikeList.add(new Spike(channel, valuesPos[channel][i], indicesPos[channel][i], timesPos[channel][i]));
            }
            for (int i = 0; i < counts[channel][1]; i++) {
                spikeList.add(new Spike(channel, valuesNeg[channel][i], indicesNeg[channel][i], timesNeg[channel][i]));
            }
        }

        Spike[] allSpikes = new Spike[spikeList.size()];
        return spikeList.toArray(allSpikes);
    }
}