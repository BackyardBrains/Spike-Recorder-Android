package com.backyardbrains.utils;

public class AnalysisUtils {

    /**
     * Calculates and returns standard deviation for the provided {@code data}.
     */
    public static float SD(short[] data, int length) {
        float mean = 0;
        float squares = 0;
        float number;
        for (int i = 0; i <= length; i++) {
            number = data[i];
            mean += number;
            squares += number * number;
        }
        mean /= length;
        squares /= length;
        return (float) Math.sqrt(squares - mean * mean);
    }

    /**
     * Calculates and returns root mean square for the provided (@code data}.
     */
    public static float RMS(short[] data, int length) {
        float squares = 0f;
        float number;
        for (int i = 0; i <= length; i++) {
            number = data[i];
            squares += number * number;
        }
        return (float) Math.sqrt(squares / length);
    }
}
