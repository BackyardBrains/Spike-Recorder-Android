package com.backyardbrains.utils;

import android.support.annotation.NonNull;

public class GlUtils {

    public static final float DEFAULT_GL_WINDOW_HORIZONTAL_SIZE = 4000f;
    public static final float DEFAULT_GL_WINDOW_VERTICAL_SIZE = 10000f;
    public static final float DEFAULT_WAVEFORM_SCALE_FACTOR = 20f;
    public static final float DEFAULT_MIN_DETECTED_PCM_VALUE = -5000000f;

    public static final int MIN_VALUE = 0;
    public static final int MAX_VALUE = 1;

    public static final int V_AXIS_VALUES_COUNT = 0;
    public static final int V_AXIS_VALUES_STEP = 1;

    public static int[] getMinMax(@NonNull int[] values) {
        if (values.length > 0) {
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            for (int value : values) {
                if (min > value) min = value;
                if (max < value) max = value;
            }

            return new int[] { min, max };
        }

        return new int[2];
    }

    public static float[] normalize(@NonNull int[] values) {
        if (values.length > 0) {
            int len = values.length;
            int[] minMax = getMinMax(values);
            int max = minMax[MAX_VALUE];
            if (max == 0) max = 1;// avoid division by zero

            float[] result = new float[len];
            for (int i = 0; i < len; i++) {
                result[i] = ((float) values[i]) / (float) max;
            }

            return result;
        }

        return new float[0];
    }

    public static int[] calculateVAxisCountAndStep(int max, int maxVAxisValues) {
        int counter = 0;
        float divider, vAxisValuesCount;
        // max divider can be 10^10
        do {
            divider = (float) Math.pow(10, counter);
            vAxisValuesCount = max / divider; // divide by 10, 100, 1000 etc
            if (vAxisValuesCount < maxVAxisValues) {
                return new int[] { (int) vAxisValuesCount, (int) (divider) };
            }

            vAxisValuesCount *= .5f; // divide by 20, 200, 200 etc
            if (vAxisValuesCount < maxVAxisValues) {
                return new int[] { (int) vAxisValuesCount, (int) (divider * 2) };
            }

            vAxisValuesCount *= .4f; // divide by 50, 500, 5000 etc
            if (vAxisValuesCount < maxVAxisValues) {
                return new int[] { (int) vAxisValuesCount, (int) (divider * 5) };
            }

            counter++;
        } while (counter <= 10);

        return new int[2];
    }

    public static float[] getMinMax(@NonNull float[] values) {
        if (values.length > 0) {
            float min = Float.MAX_VALUE;
            float max = Float.MIN_VALUE;
            for (float value : values) {
                if (min > value) min = value;
                if (max < value) max = value;
            }

            return new float[] { min, max };
        }

        return new float[2];
    }

    public static float[] normalize(@NonNull float[] values) {
        if (values.length > 0) {
            int len = values.length;
            float[] minMax = getMinMax(values);
            float max = minMax[MAX_VALUE];
            if (max == 0) max = 1;// avoid division by zero

            float[] result = new float[len];
            for (int i = 0; i < len; i++) {
                result[i] = values[i] / max;
            }

            return result;
        }

        return new float[0];
    }

    public static float[] calculateVAxisCountAndStep(float max, int maxVAxisValues) {
        int counter = 0;
        float divider, vAxisValuesCount;
        // max divider can be 10^10
        do {
            divider = (float) Math.pow(10, counter);
            vAxisValuesCount = max / divider; // divide by 10, 100, 1000 etc
            if (vAxisValuesCount < maxVAxisValues) {
                return new float[] { (int) vAxisValuesCount, divider };
            }

            vAxisValuesCount *= .5f; // divide by 20, 200, 200 etc
            if (vAxisValuesCount < maxVAxisValues) {
                return new float[] { (int) vAxisValuesCount, divider * 2f };
            }

            vAxisValuesCount *= .4f; // divide by 50, 500, 5000 etc
            if (vAxisValuesCount < maxVAxisValues) {
                return new float[] { (int) vAxisValuesCount, divider * 5f };
            }

            counter++;
        } while (counter <= 10);

        return new float[2];
    }
}
