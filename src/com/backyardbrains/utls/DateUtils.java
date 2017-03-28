package com.backyardbrains.utls;

import android.support.annotation.NonNull;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public final class DateUtils {

    private static SimpleDateFormat formatter = (SimpleDateFormat) SimpleDateFormat.getDateInstance();

    // Pattern for "Mar 17, 2017 12:06 a.m." date format
    private static final String PATTERN_MMM_D_YYYY_HH_MM_A = "MMM d, yyyy HH:mm a";

    /**
     * Returns string representation of the specified {@code date} formatted like following example "Mar 17, 2017 12:06
     * am".
     */
    public static String format_MMM_d_yyyy_HH_mm_a(@NonNull Date date) {
        formatter.applyPattern(PATTERN_MMM_D_YYYY_HH_MM_A);
        return formatter.format(date);
    }

    // Pattern for "17_Mar_2017_12_06_34_a.m." date format
    private static final String PATTERN_D_MMM_YYYY_HH_MM_S_A = "d_MMM_yyyy_HH_mm_s_a";

    /**
     * Returns string representation of the specified {@code date} formatted like following example
     * "17_Mar_2017_12_06_34_a.m.".
     */
    public static String format_d_MMM_yyyy_HH_mm_s_a(@NonNull Date date) {
        formatter.applyPattern(PATTERN_D_MMM_YYYY_HH_MM_S_A);
        return formatter.format(date);
    }
}
