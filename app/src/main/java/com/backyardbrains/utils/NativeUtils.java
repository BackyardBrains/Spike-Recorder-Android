package com.backyardbrains.utils;

import com.backyardbrains.usb.SamplesWithEvents;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class NativeUtils {

    public static native String helloTest();

    public static native void testPassByRef(short[] test);

    public static native void setSampleRate(int sampleRate);

    public static native void setFilters(float lowCutOff, float highCutOff);

    public static native SamplesWithEvents processSampleStream(byte[] data, int length);

    public static native int[] prepareForDrawing(short[] envelopedSamples, short[] samples, int[] envelopedEventIndices,
        int[] eventIndices, int eventCount, int start, int end, int drawSurfaceWidth);

    public static native int prepareForMarkerDrawing(int[] envelopedEventIndices, int[] eventIndices, int eventCount,
        int start, int end, int drawSurfaceWidth);

    public static native int prepareForThresholdDrawing(short[] envelopedSamples, short[] samples, int start, int end,
        int drawSurfaceWidth);

    static {
        System.loadLibrary("byb-lib");
    }
}
