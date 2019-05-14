package com.backyardbrains.utils;

import com.backyardbrains.drawing.EventsDrawData;
import com.backyardbrains.drawing.FftDrawData;
import com.backyardbrains.drawing.SignalDrawData;
import com.backyardbrains.drawing.SpikesDrawData;
import com.backyardbrains.dsp.FftData;
import com.backyardbrains.dsp.SignalData;
import com.backyardbrains.dsp.usb.AbstractUsbSignalSource;
import com.backyardbrains.vo.SpikeIndexValue;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class JniUtils {

    public static native String helloTest();

    public static native void testPassByRef(short[] test);

    public static native int interleaveSignal(short[] out, SignalData in);

    public static native void setSampleRate(int sampleRate);

    public static native void setChannelCount(int channelCount);

    public static native void setBandFilter(float lowCutOffFreq, float highCutOffFreq);

    public static native void setNotchFilter(float centerFreq);

    public static native void processSampleStream(SignalData out, byte[] data, int length,
        AbstractUsbSignalSource sampleSource);

    public static native boolean isAudioStreamAmModulated();

    public static native void processMicrophoneStream(SignalData out, byte[] data, int length);

    public static native void processPlaybackStream(SignalData signalData, byte[] data, int length, int[] eventIndices,
        String[] eventNames, int eventCount, long fromSample, long toSample, int prependSamples);

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

    public static native void processThreshold(SignalData out, SignalData in, boolean averageSamples);

    public static native void resetFft();

    public static native void processFft(FftData out, SignalData in);

    public static native void prepareForSignalDrawing(SignalDrawData outSignal, EventsDrawData outEvents,
        short[][] inSignal, int inFrameCount, int[] inEventIndices, int inEventCount, int drawStartIndex,
        int drawEndIndex, int drawSurfaceWidth);

    public static native void prepareForThresholdDrawing(SignalDrawData outSignal, EventsDrawData outEvents,
        short[][] inSignal, int inFrameCount, int[] inEventIndices, int inEventCount, int drawStartIndex,
        int drawEndIndex, int drawSurfaceWidth);

    public static native void prepareForFftDrawing(FftDrawData out, float[][] in, int drawStartIndex, int drawEndIndex,
        int drawSurfaceWidth, int drawSurfaceHeight, float fftScaleFactor);

    public static native void prepareForSpikesDrawing(SpikesDrawData out, SpikeIndexValue[] in, float[] colorInRange,
        float[] colorOutOfRange, int rangeStart, int rangeEnd, int sampleStartIndex, int sampleEndIndex,
        int drawStartIndex, int drawEndIndex, int samplesToDraw, int drawSurfaceWidth);

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
