package com.backyardbrains.analysis;

import android.support.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

class BYBCrossCorrelationAnalysis extends BYBBaseAnalysis {

    private static final String TAG = makeLogTag(BYBCrossCorrelationAnalysis.class);

    private final List<List<BYBSpike>> trains;
    private List<List<Integer>> crossCorrelation = new ArrayList<>();

    BYBCrossCorrelationAnalysis(@NonNull List<List<BYBSpike>> trains, @NonNull AnalysisListener listener) {
        super(listener);

        this.trains = trains;

        execute();
    }

    @NonNull List<List<Integer>> getCrossCorrelation() {
        return crossCorrelation;
    }

    @Override void process() {
        float maxTime = 0.1f;
        float binSize = 0.001f;

        clearCrossCorrelation();
        crossCorrelation = new ArrayList<>();

        for (int fSpikeTrainIndex = 0; fSpikeTrainIndex < trains.size(); fSpikeTrainIndex++) {
            for (int sSpikeTrainIndex = 0; sSpikeTrainIndex < trains.size(); sSpikeTrainIndex++) {
                List<BYBSpike> fspikeTrain = trains.get(fSpikeTrainIndex);
                List<BYBSpike> sspikeTrain = trains.get(sSpikeTrainIndex);
                ArrayList<Integer> temp = new ArrayList<>();
                if (fspikeTrain.size() > 1 && sspikeTrain.size() > 1) {
                    BYBSpike firstSpike;
                    BYBSpike secondSpike;
                    int n = (int) Math.ceil((2 * maxTime + binSize) / binSize);

                    int[] histogram = new int[n];
                    for (int x = 0; x < n; ++x) {
                        histogram[x] = 0;
                    }

                    float minEdge = -maxTime - binSize * 0.5f;
                    float maxEdge = maxTime + binSize * 0.5f;
                    float diff;
                    int index;
                    int mainIndex;
                    int secIndex;
                    boolean insideInterval;
                    // go through first spike train
                    for (mainIndex = 0; mainIndex < fspikeTrain.size(); mainIndex++) {
                        firstSpike = fspikeTrain.get(mainIndex);
                        // Check on left of spike
                        insideInterval = false;
                        // go through second spike train
                        for (secIndex = 0; secIndex < sspikeTrain.size(); secIndex++) {
                            secondSpike = sspikeTrain.get(secIndex);
                            diff = firstSpike.time - secondSpike.time;
                            if (diff > minEdge && diff < maxEdge) {
                                insideInterval = false;
                                index = (int) (((diff - minEdge) / binSize));
                                histogram[index]++;
                            } else if (insideInterval) {
                                break;
                            }
                        }
                    }

                    for (int j = 0; j < n; j++) {
                        temp.add(histogram[j]);
                    }
                }
                crossCorrelation.add(temp);
            }
        }
    }

    private void clearCrossCorrelation() {
        LOGD(TAG, "clearCrossCorrelation()");

        if (crossCorrelation != null) {
            for (int i = 0; i < crossCorrelation.size(); i++) {
                if (crossCorrelation.get(i) != null) {
                    crossCorrelation.get(i).clear();
                }
            }
            crossCorrelation.clear();
            crossCorrelation = null;
        }
    }
}
