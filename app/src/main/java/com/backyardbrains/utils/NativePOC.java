package com.backyardbrains.utils;

import com.backyardbrains.usb.SamplesWithMarkers;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class NativePOC {

    public static native String helloTest();

    public static native void testPassByRef(short[] test);

    public static native SamplesWithMarkers processSampleStream(byte[] data);

    public static native short[] prepareForWaveformDrawing(short[] samples, int start, int end, int drawSurfaceWidth);

    public static native int[] prepareForMarkerDrawing(int[] eventIndices, int fromSample, int toSample,
        int drawSurfaceWidth);

    public static native short[] prepareForThresholdDrawing(short[] samples, int start, int end, int drawSurfaceWidth);

    static {
        System.loadLibrary("byb-lib");
    }
}
