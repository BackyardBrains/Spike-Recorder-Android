package com.backyardbrains.analysis;

import android.support.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;

class BYBAutocorrelationAnalysis extends BYBBaseAnalysis {

    private final List<List<BYBSpike>> trains;
    private List<List<Integer>> autoCorrelation;

    BYBAutocorrelationAnalysis(@NonNull List<List<BYBSpike>> trains, @NonNull AnalysisListener listener) {
        super(listener);

        this.trains = trains;

        execute();
    }

    List<List<Integer>> getAutoCorrelation() {
        return autoCorrelation;
    }

    @Override void process() {
        float maxTime = 0.1f;
        float binSize = 0.001f;

        clearAutoCorrelation();
        autoCorrelation = new ArrayList<>();

        for (int i = 0; i < trains.size(); i++) {
            BYBSpike firstSpike;
            BYBSpike secondSpike;
            int n = (int) Math.ceil((maxTime + binSize) / binSize);

            int[] histogram = new int[n];
            for (int x = 0; x < n; x++) {
                histogram[x] = 0;
            }

            float minEdge = -binSize * 0.5f;
            float maxEdge = maxTime + binSize * 0.5f;
            float diff;
            int index;
            int mainIndex;
            int secIndex;

            for (mainIndex = 0; mainIndex < trains.get(i).size(); mainIndex++) {
                firstSpike = trains.get(i).get(mainIndex);
                // Check on left of spike
                for (secIndex = mainIndex; secIndex >= 0; secIndex--) {
                    secondSpike = trains.get(i).get(secIndex);
                    diff = firstSpike.time - secondSpike.time;
                    if (diff > minEdge && diff < maxEdge) {
                        index = (int) (((diff - minEdge) / binSize));
                        histogram[index]++;
                    } else {
                        break;
                    }
                }
                // check on right of spike
                for (secIndex = mainIndex + 1; secIndex < trains.get(i).size(); secIndex++) {
                    secondSpike = trains.get(i).get(secIndex);
                    diff = firstSpike.time - secondSpike.time;
                    if (diff > minEdge && diff < maxEdge) {
                        index = (int) (((diff - minEdge) / binSize));
                        histogram[index]++;
                    } else {
                        break;
                    }
                }
            }
            ArrayList<Integer> temp = new ArrayList<>();

            for (int j = 0; j < n; j++) {
                temp.add(histogram[j]);
            }
            autoCorrelation.add(temp);
        }
    }

    private void clearAutoCorrelation() {
        if (autoCorrelation != null) {
            for (int i = 0; i < autoCorrelation.size(); i++) {
                if (autoCorrelation.get(i) != null) {
                    autoCorrelation.get(i).clear();
                }
            }
            autoCorrelation.clear();
            autoCorrelation = null;
        }
    }
}
