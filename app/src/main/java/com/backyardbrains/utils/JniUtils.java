package com.backyardbrains.utils;

import com.backyardbrains.data.processing.SamplesWithEvents;
import com.backyardbrains.usb.AbstractUsbSampleSource;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class JniUtils {

    public static native String helloTest();

    public static native void testPassByRef(short[] test);

    public static native void setSampleRate(int sampleRate);

    public static native void setChannelCount(int channelCount);

    public static native void setFilters(float lowCutOff, float highCutOff);

    public static native void processSampleStream(SamplesWithEvents out, byte[] data, int length,
        AbstractUsbSampleSource sampleSource);

    public static native boolean isAudioStreamAmModulated();

    public static native void processMicrophoneStream(SamplesWithEvents out, byte[] data, int length);

    public static native void processPlaybackStream(SamplesWithEvents samplesWithEvents, byte[] data, int length,
        int[] eventIndices, String[] eventNames, int eventCount, long fromSample, long toSample, int prependSamples);

    public static native int getAveragedSampleCount();

    public static native void setAveragedSampleCount(int averagedSampleCount);

    public static native void setThreshold(int threshold);

    public static native void resetThreshold();

    public static native void pauseThreshold();

    public static native void resumeThreshold();

    public static native void setBpmProcessing(boolean processBpm);

    public static native void processThreshold(SamplesWithEvents samplesWithEvents, short[] samples, int length);

    public static native void prepareForDrawing(SamplesWithEvents out, short[] samples, int[] eventIndices,
        int eventCount, int fromSample, int toSample, int drawSurfaceWidth);

    public static native void prepareForThresholdDrawing(SamplesWithEvents out, short[] samples, int[] eventIndices,
        int eventCount, int fromSample, int toSample, int drawSurfaceWidth);

    public static native int[] findSpikes(String filePath, short[] valuesPos, int[] indicesPos, float[] timesPos,
        short[] valuesNeg, int[] indicesNeg, float[] timesNeg, int maxSpikes);

    public static native void autocorrelationAnalysis(float[][] spikeTrains, int spikeTrainCount, int[] spikeCounts,
        int[][] analysis, int analysisBinCount);

    public static native void isiAnalysis(float[][] spikeTrains, int spikeTrainCount, int[] spikeCounts,
        int[][] analysis, int analysisBinCount);

    public static native void crossCorrelationAnalysis(float[][] spikeTrains, int spikeTrainCount, int[] spikeCounts,
        int[][] analysis, int analysisCount, int binCount);

    public static native void averageSpikeAnalysis(String filePath, int[][] trains, int trainCount, int[] spikeCounts,
        float[][] averageSpike, float[][] normAverageSpike, float[][] normTopStdLine, float[][] normBottomStdLine,
        int batchSpikeCount);

    static {
        System.loadLibrary("byb-lib");
    }
}
