package com.backyardbrains.utils;

import com.backyardbrains.usb.SamplesWithMarkers;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class NativePOC {

    public static native String helloTest();

    public static native void testPassByRef(short[] test);

    public static native SamplesWithMarkers processSampleStream(byte[] data);

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
