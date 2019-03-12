package com.backyardbrains.utils;

import com.backyardbrains.drawing.FftDrawData;
import com.backyardbrains.dsp.FftData;
import com.backyardbrains.dsp.SamplesWithEvents;
import com.backyardbrains.dsp.usb.AbstractUsbSignalSource;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class JniUtils {

    public static native String helloTest();

    public static native void testPassByRef(short[] test);

    public static native int interleaveSignal(short[] out, SamplesWithEvents in);

    public static native void setSampleRate(int sampleRate);

    public static native void setChannelCount(int channelCount);

    public static native void setBandFilter(float lowCutOffFreq, float highCutOffFreq);

    public static native void setNotchFilter(float centerFreq);

    public static native void processSampleStream(SamplesWithEvents out, byte[] data, int length,
        AbstractUsbSignalSource sampleSource);

    public static native boolean isAudioStreamAmModulated();

    public static native void processMicrophoneStream(SamplesWithEvents out, byte[] data, int length);

    public static native void processPlaybackStream(SamplesWithEvents samplesWithEvents, byte[] data, int length,
        int[] eventIndices, String[] eventNames, int eventCount, long fromSample, long toSample, int prependSamples);

    public static native int getAveragedSampleCount();

    public static native void setAveragedSampleCount(int averagedSampleCount);

    public static native void setSelectedChannel(int selectedChannel);

    public static native void setThreshold(float threshold);

    public static native void resetThreshold();

    public static native void pauseThreshold();

    public static native int getAveragingTriggerType();

    public static native void setAveragingTriggerType(int triggerType);

    public static native void resumeThreshold();

    public static native void setBpmProcessing(boolean processBpm);

    public static native void processThreshold(SamplesWithEvents out, SamplesWithEvents in, boolean averageSamples);

    public static native void processFft(FftData out, SamplesWithEvents in);

    public static native void prepareForSignalDrawing(SamplesWithEvents out, short[][] samples, int frameCount,
        int[] eventIndices, int eventCount, int fromSample, int toSample, int drawSurfaceWidth);

    public static native void prepareForThresholdDrawing(SamplesWithEvents out, short[][] samples, int sampleCount,
        int[] eventIndices, int eventCount, int fromSample, int toSample, int drawSurfaceWidth);

    public static native void prepareForFftDrawing(FftDrawData out, float[][] in, int drawSurfaceWidth,
        int drawSurfaceHeight);

    public static native int[][] findSpikes(String filePath, short[][] valuesPos, int[][] indicesPos,
        float[][] timesPos, short[][] valuesNeg, int[][] indicesNeg, float[][] timesNeg, int channelCount,
        int maxSpikes);

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
