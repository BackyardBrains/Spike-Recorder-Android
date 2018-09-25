package com.backyardbrains.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class PrefUtils {

    private static final String PREF_NAME_PREFIX = "bb_pref_";

    // Returns reference to default shared preferences
    private static SharedPreferences getSharedPreferences(@NonNull final Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    private static String constructPrefKey(@NonNull Class clazz, @NonNull String key) {
        return PREF_NAME_PREFIX + clazz.getName() + key;
    }

    /**
     * Boolean indicating whether scaling instructions should be shown or not.
     */
    private static final String PREF_BOOL_SHOW_SCALING_INSTRUCTIONS = "_show_scaling_instructions";

    public static boolean isShowScalingInstructions(@NonNull Context context, @NonNull Class clazz) {
        return getSharedPreferences(context).getBoolean(constructPrefKey(clazz, PREF_BOOL_SHOW_SCALING_INSTRUCTIONS),
            true);
    }

    public static void setShowScalingInstructions(@NonNull Context context, @NonNull Class clazz,
        boolean showScalingInstructions) {
        getSharedPreferences(context).edit()
            .putBoolean(constructPrefKey(clazz, PREF_BOOL_SHOW_SCALING_INSTRUCTIONS), showScalingInstructions)
            .apply();
    }

    /**
     * Integer indicating horizontal size of the GL surface view.
     */
    private static final String PREF_INT_GL_WINDOW_HORIZONTAL_SIZE = "_gl_window_horizontal_size";

    public static int getGlWindowHorizontalSize(@NonNull Context context, @NonNull Class clazz) {
        return getSharedPreferences(context).getInt(constructPrefKey(clazz, PREF_INT_GL_WINDOW_HORIZONTAL_SIZE),
            GlUtils.DEFAULT_GL_WINDOW_HORIZONTAL_SIZE);
    }

    public static void setGlWindowHorizontalSize(@NonNull Context context, @NonNull Class clazz,
        int glWindowHorizontalSize) {
        getSharedPreferences(context).edit()
            .putInt(constructPrefKey(clazz, PREF_INT_GL_WINDOW_HORIZONTAL_SIZE), glWindowHorizontalSize)
            .apply();
    }

    /**
     * Integer indicating vertical size of the GL surface view.
     */
    private static final String PREF_INT_GL_WINDOW_VERTICAL_SIZE = "_gl_window_vertical_size";

    public static int getGlWindowVerticalSize(@NonNull Context context, @NonNull Class clazz) {
        return getSharedPreferences(context).getInt(constructPrefKey(clazz, PREF_INT_GL_WINDOW_VERTICAL_SIZE),
            GlUtils.DEFAULT_GL_WINDOW_VERTICAL_SIZE);
    }

    public static void setGlWindowVerticalSize(@NonNull Context context, @NonNull Class clazz,
        int glWindowVerticalSize) {
        getSharedPreferences(context).edit()
            .putInt(constructPrefKey(clazz, PREF_INT_GL_WINDOW_VERTICAL_SIZE), glWindowVerticalSize)
            .apply();
    }

    /**
     * Integer indicating widht of the GL surface viewport.
     */
    private static final String PREF_INT_VIEWPORT_WIDTH = "_viewport_width";

    public static int getViewportWidth(@NonNull Context context, @NonNull Class clazz) {
        return getSharedPreferences(context).getInt(constructPrefKey(clazz, PREF_INT_VIEWPORT_WIDTH), 0);
    }

    public static void setViewportWidth(@NonNull Context context, @NonNull Class clazz, int viewportWidth) {
        getSharedPreferences(context).edit()
            .putInt(constructPrefKey(clazz, PREF_INT_VIEWPORT_WIDTH), viewportWidth)
            .apply();
    }

    /**
     * Integer indicating height of the GL surface viewport.
     */
    private static final String PREF_INT_VIEWPORT_HEIGHT = "_viewport_height";

    public static int getViewportHeight(@NonNull Context context, @NonNull Class clazz) {
        return getSharedPreferences(context).getInt(constructPrefKey(clazz, PREF_INT_VIEWPORT_HEIGHT), 0);
    }

    public static void setViewportHeight(@NonNull Context context, @NonNull Class clazz, int viewportHeight) {
        getSharedPreferences(context).edit()
            .putInt(constructPrefKey(clazz, PREF_INT_VIEWPORT_HEIGHT), viewportHeight)
            .apply();
    }

    /**
     * Boolean indicating whether auto scale is on or off.
     */
    private static final String PREF_BOOL_AUTO_SCALE = "_auto_scale";

    public static boolean getAutoScale(@NonNull Context context, @NonNull Class clazz) {
        return getSharedPreferences(context).getBoolean(constructPrefKey(clazz, PREF_BOOL_AUTO_SCALE), false);
    }

    public static void setAutoScale(@NonNull Context context, @NonNull Class clazz, boolean autoScale) {
        getSharedPreferences(context).edit()
            .putBoolean(constructPrefKey(clazz, PREF_BOOL_AUTO_SCALE), autoScale)
            .apply();
    }

    /**
     * Float indicating minimum detected PCM value.
     */
    private static final String PREF_FLOAT_MINIMUM_DETECTED_PCM_VALUE = "_minimum_detected_pcm_value";

    public static float getMinimumDetectedPcmValue(@NonNull Context context, @NonNull Class clazz) {
        return getSharedPreferences(context).getFloat(constructPrefKey(clazz, PREF_FLOAT_MINIMUM_DETECTED_PCM_VALUE),
            GlUtils.DEFAULT_MIN_DETECTED_PCM_VALUE);
    }

    public static void setMinimumDetectedPcmValue(@NonNull Context context, @NonNull Class clazz,
        float minimumDetectedPcmValue) {
        getSharedPreferences(context).edit()
            .putFloat(constructPrefKey(clazz, PREF_FLOAT_MINIMUM_DETECTED_PCM_VALUE), minimumDetectedPcmValue)
            .apply();
    }

    /**
     * Float indicating value of the last set threshold.
     */
    private static final String PREF_FLOAT_THRESHOLD = "_threshold";

    public static float getThreshold(@NonNull Context context, @NonNull Class clazz) {
        return getSharedPreferences(context).getFloat(constructPrefKey(clazz, PREF_FLOAT_THRESHOLD), 0);
    }

    public static void setThreshold(@NonNull Context context, @NonNull Class clazz, float threshold) {
        getSharedPreferences(context).edit().putFloat(constructPrefKey(clazz, PREF_FLOAT_THRESHOLD), threshold).apply();
    }

    /**
     * Integer indicating the number of sample sets that should be summed when averaging incoming signal.
     */
    private static final String PREF_INT_AVERAGED_SAMPLE_COUNT = "_averaged_sample_count";

    public static int getAveragedSampleCount(@NonNull Context context, @NonNull Class clazz) {
        return getSharedPreferences(context).getInt(constructPrefKey(clazz, PREF_INT_AVERAGED_SAMPLE_COUNT), -1);
    }

    public static void setAveragedSampleCount(@NonNull Context context, @NonNull Class clazz, int averagedSampleCount) {
        getSharedPreferences(context).edit()
            .putInt(constructPrefKey(clazz, PREF_INT_AVERAGED_SAMPLE_COUNT), averagedSampleCount)
            .apply();
    }

    /**
     * Boolean indicating whether BPM sound is on or off.
     */
    private static final String PREF_BOOL_BPM_SOUND = "bpm_sound";

    public static boolean getBpmSound(@NonNull Context context) {
        return getSharedPreferences(context).getBoolean(PREF_NAME_PREFIX + PREF_BOOL_BPM_SOUND, true);
    }

    public static void setBpmSound(@NonNull Context context, boolean bpmSound) {
        getSharedPreferences(context).edit().putBoolean(PREF_NAME_PREFIX + PREF_BOOL_BPM_SOUND, bpmSound).apply();
    }
}
