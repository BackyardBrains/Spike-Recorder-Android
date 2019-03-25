package com.backyardbrains.utils;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class ArrayUtils {

    /**
     * Creates and returns deep copy of the specified {@code src} 2d array.
     */
    public static float[][] copy(float[][] src) {
        final float[][] result = new float[src.length][];
        for (int i = 0; i < result.length; i++) {
            result[i] = new float[src[i].length];
            System.arraycopy(src[i], 0, result[i], 0, result[i].length);
        }

        return result;
    }
}
