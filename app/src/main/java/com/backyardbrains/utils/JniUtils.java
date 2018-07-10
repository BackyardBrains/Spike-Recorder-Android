package com.backyardbrains.utils;

import com.backyardbrains.data.processing.SamplesWithEvents;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class JniUtils {

    public static native String helloTest();

    public static native void testPassByRef(short[] test);

    public static native void setSampleRate(int sampleRate);

    public static native void setFilters(float lowCutOff, float highCutOff);

    public static native void processSampleStream(SamplesWithEvents out, byte[] data, int length);

    public static native void processAudioStream(SamplesWithEvents out, short[] inSamples, int length);

    public static native boolean isAudioStreamAmModulated();

    public static native void prepareForDrawing(SamplesWithEvents out, short[] samples, int[] eventIndices,
        int eventCount, int start, int end, int drawSurfaceWidth);

    public static native void prepareForThresholdDrawing(SamplesWithEvents out, short[] samples, int[] eventIndices,
        int eventCount, int start, int end, int drawSurfaceWidth);

    public static native int[] findSpikes(String filePath, short[] valuesPos, int[] indicesPos, float[] timesPos,
        short[] valuesNeg, int[] indicesNeg, float[] timesNeg, int maxSpikes);

    public static native void autocorrelationAnalysis(float[][] spikeTrains, int spikeTrainCount, int[] spikeCounts,
        int[][] analysis, int analysisBinCount);

    static {
        System.loadLibrary("byb-lib");
    }
}
