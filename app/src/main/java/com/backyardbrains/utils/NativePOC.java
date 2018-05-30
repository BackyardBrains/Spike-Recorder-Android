package com.backyardbrains.utils;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class NativePOC {

    public static native String helloTest();

    public static native void testPassByRef(short[] test);

    public static native short[] prepareForWaveformDrawing(short[] samples, int start, int end, int returnCount);

    public static native int[] prepareForMarkerDrawing(int[] eventIndices, int fromSample, int toSample,
        int returnCount);

    public static native short[] prepareForThresholdDrawing(short[] samples, int start, int end, int returnCount);

    static {
        System.loadLibrary("poc-lib");
    }
}
