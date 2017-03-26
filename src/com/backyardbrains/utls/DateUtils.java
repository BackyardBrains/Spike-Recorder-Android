package com.backyardbrains.utls;

import android.support.annotation.NonNull;
import java.text.DateFormat;
import java.util.Date;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public final class DateUtils {

    private static DateFormat formatter = DateFormat.getDateInstance();

    // Pattern for "Fri 17, 2017 12:06 a.m." date format
    private static final String PATTERN_MMM_D_YYYY_HH_MM_A = "MMM d, yyyy HH:mm a";

    /**
     * Returns string representation of the specified {@code date} formatted like following example "Fri 17, 2017 12:06
     * am".
     */
    public static String format_MMM_d_yyyy_HH_mm_a(@NonNull Date date) {
        return formatter.format(date);
    }
}
