package com.backyardbrains.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import com.backyardbrains.BackyardBrainsMain;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class PrefUtils {

    /**
     * Boolean indicating whether scaling instructions should be shown or not.
     */
    private static final String PREF_BOOL_SHOW_SCALING_INSTRUCTIONS = "_ShowScalingInstructions";

    public static boolean isShowScalingInstructions(@NonNull BackyardBrainsMain context, @NonNull String tag) {
        return context.getPreferences(Context.MODE_PRIVATE).getBoolean(tag + PREF_BOOL_SHOW_SCALING_INSTRUCTIONS, true);
    }

    public static void setShowScalingInstructions(@NonNull BackyardBrainsMain context, @NonNull String tag,
        boolean showScalingInstructions) {
        context.getPreferences(Context.MODE_PRIVATE)
            .edit()
            .putBoolean(tag + PREF_BOOL_SHOW_SCALING_INSTRUCTIONS, showScalingInstructions)
            .apply();
    }
}
