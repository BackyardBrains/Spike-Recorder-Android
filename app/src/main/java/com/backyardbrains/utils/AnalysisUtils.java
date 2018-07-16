package com.backyardbrains.utils;

public class AnalysisUtils {

    public static final int MAX_SPIKE_TRAIN_COUNT = 3;

    /**
     * Calculates and returns root mean square for the provided (@code data}.
     */
    public static float RMS(short[] data, int length) {
        float squares = 0f;
        float number;
        for (int i = 0; i < length; i++) {
            number = data[i];
            squares += number * number;
        }
        return (float) Math.sqrt(squares / length);
    }
}
