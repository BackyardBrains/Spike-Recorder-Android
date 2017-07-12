package com.backyardbrains.analysis;

import android.support.annotation.NonNull;
import com.backyardbrains.utils.BYBUtils;
import java.util.ArrayList;
import java.util.List;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

class BYBIsiAnalysis extends BYBBaseAnalysis {

    private static final String TAG = makeLogTag(BYBIsiAnalysis.class);

    private List<List<BYBInterSpikeInterval>> isi = new ArrayList<>();
    private final List<List<BYBSpike>> trains;

    BYBIsiAnalysis(@NonNull List<List<BYBSpike>> trains, @NonNull AnalysisListener listener) {
        super(listener);

        this.trains = trains;

        execute();
    }

    @NonNull List<List<BYBInterSpikeInterval>> getIsi() {
        return isi;
    }

    @Override void process() {
        int bins = 100;
        float[] logSpace = BYBUtils.generateLogSpace(-3, 1, bins - 1);

        clearIsi();
        isi = new ArrayList<>();

        for (int k = 0; k < trains.size(); k++) {
            int[] histogram = new int[bins];
            for (int x = 0; x < bins; x++) {
                histogram[x] = 0;
            }
            if (trains.get(k) != null) {
                float interSpikeDistance;
                int spikesCount = trains.get(k).size();
                for (int i = 1; i < spikesCount; i++) {
                    interSpikeDistance = trains.get(k).get(i).time - trains.get(k).get(i - 1).time;
                    for (int j = 1; j < bins; j++) {
                        if (interSpikeDistance >= logSpace[j - 1] && interSpikeDistance < logSpace[j]) {
                            histogram[j - 1]++;
                            break;
                        }
                    }
                }
            }
            ArrayList<BYBInterSpikeInterval> temp = new ArrayList<>();
            for (int i = 0; i < bins; i++) {
                temp.add(new BYBInterSpikeInterval(logSpace[i], histogram[i]));
            }
            isi.add(temp);
        }
    }

    private void clearIsi() {
        LOGD(TAG, "clearIsi()");

        if (isi != null) {
            for (int i = 0; i < isi.size(); i++) {
                if (isi.get(i) != null) isi.get(i).clear();
            }
            isi.clear();
            isi = null;
        }
    }
}