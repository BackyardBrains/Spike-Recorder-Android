package com.backyardbrains.utils;

public class AnalysisUtils {

    /**
     * Calculates and returns standard deviation for the provided {@code data}.
     */
    public static float STD(short[] data) {
        int n = data.length;
        float mean = 0;
        float squares = 0;
        for (short number : data) {
            mean += number;
            squares += number * number;
        }
        mean /= n;
        squares /= n;
        return (float) Math.sqrt(squares - mean * mean);
    }
}
