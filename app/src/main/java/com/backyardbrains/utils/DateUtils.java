package com.backyardbrains.utils;

import android.support.annotation.NonNull;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public final class DateUtils {

    private static SimpleDateFormat formatter = (SimpleDateFormat) SimpleDateFormat.getDateInstance();

    // Pattern for "Mar 17, 2017 12:06 a.m." date format
    private static final String PATTERN_MMM_D_YYYY_HH_MM_A = "MMM d, yyyy hh:mm a";

    /**
     * Returns string representation of the specified {@code date} formatted like following example "Mar 17, 2017 12:06
     * am".
     */
    public static String format_MMM_d_yyyy_HH_mm_a(@NonNull Date date) {
        formatter.applyPattern(PATTERN_MMM_D_YYYY_HH_MM_A);
        return formatter.format(date);
    }

    // Pattern for "2018-03-02_13.06.05" date format
    private static final String PATTERN_YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd_HH:mm:ss";

    /**
     * Returns string representation of the specified {@code date} formatted like following example
     * "2018-03-02_13.06.05".
     */
    static String format_yyyy_MM_dd_HH_mm_ss(@NonNull Date date) {
        formatter.applyPattern(PATTERN_YYYY_MM_DD_HH_MM_SS);
        return formatter.format(date);
    }

    // Pattern for "1/16/2019, 1:09 PM" date format
    private static final String PATTERN_M_D_YYYY_H_MM_A = "M/d/yyyy, H:mm a";

    /**
     * Returns string representation of the specified {@code date} formatted like following example
     * "1/16/2019, 1:09 PM".
     */
    public static String format_M_d_yyyy_H_mm_a(@NonNull Date date) {
        formatter.applyPattern(PATTERN_M_D_YYYY_H_MM_A);
        return formatter.format(date);
    }
}
