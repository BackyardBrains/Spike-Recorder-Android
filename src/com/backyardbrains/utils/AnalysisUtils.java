package com.backyardbrains.utils;

public class AnalysisUtils {

    public static float STD(short data[], int startSample, int samplesToProcess) {
        float std;
        float mean = 0; // place holder for mean
        int endSample = startSample + samplesToProcess;
        for (int i = startSample; i < endSample && i < data.length; i++) {
            mean += data[i];
        }
        mean = mean / samplesToProcess;
        mean = -1 * mean; // Invert mean so when we add it is actually subtraction
        float subMeanVec[] = new float[samplesToProcess]; // placeholder vector
        int j = 0;
        for (int i = startSample; i < endSample && i < data.length; i++) {
            subMeanVec[j++] = data[i] + mean;
        }
        float squared[] = new float[samplesToProcess];
        for (int i = 0; i < samplesToProcess; i++) {
            squared[i] = subMeanVec[i] * subMeanVec[i];
        }
        float sum = 0; // place holder for sum
        for (int i = 0; i < samplesToProcess; i++) {
            sum += squared[i];
        }

        std = (float) Math.sqrt(sum / samplesToProcess); // calculated std deviation
        return std;
    }
}
