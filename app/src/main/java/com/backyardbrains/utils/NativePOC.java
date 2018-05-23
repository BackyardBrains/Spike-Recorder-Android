package com.backyardbrains.utils;

/**
 * @author Tihomir Leka <ticapeca at gmail.com.
 */
public class NativePOC {

    public static native String helloTest();

    public static native short[] prepareForWaveformDrawing(short[] samples, int start, int end, int returnCount);

    public static native short[] prepareForThresholdDrawing(short[] samples, int start, int end);

    static {
        System.loadLibrary("poc-lib");
    }
}
