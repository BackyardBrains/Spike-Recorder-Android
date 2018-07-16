package com.backyardbrains.utils;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.util.concurrent.TimeUnit;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class Formats {

    private static final String UNIT_MILLIS = "msec";
    private static final String UNIT_SECS = "s";
    private static final String UNIT_MILLIVOLTS = "mV";

    private static final DecimalFormat timeFormat = new DecimalFormat("#.0");
    private static final DecimalFormat signalFormat = new DecimalFormat("#.##");

    private static final StringBuffer stringBuilder = new StringBuffer(10);
    private static final FieldPosition fieldPosition = new FieldPosition(NumberFormat.FRACTION_FIELD);

    /**
     * Formats specified millisecond. If {@code ms} is < 1000 time is formatted as millis, else as seconds.
     */
    public static String formatTime_s_msec(float ms) {
        stringBuilder.delete(0, stringBuilder.length());

        final double time = ms < 1000 ? ms : ms / 1000;
        timeFormat.format(time, stringBuilder, fieldPosition);
        return stringBuilder.append(" ").append(ms < 1000 ? UNIT_MILLIS : UNIT_SECS).toString();
    }

    /**
     * Formats specified milliseconds as "mm:ss".
     */
    public static String formatTime_mm_ss(long ms) {
        stringBuilder.delete(0, stringBuilder.length());

        long minutes = TimeUnit.MILLISECONDS.toMinutes(ms);
        if (minutes < 10) stringBuilder.append(0);
        stringBuilder.append(minutes).append(":");
        long seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % TimeUnit.MINUTES.toSeconds(1);
        if (seconds < 10) stringBuilder.append(0);
        stringBuilder.append(seconds);
        return stringBuilder.toString();
    }

    /**
     * Formats specified seconds as "XX s" if {@code s} is less then 60 and "XXm XXs" if it's more.
     *
     * @param s seconds that need to be formatted.
     */
    public static String formatTime_m_s(long s) {
        stringBuilder.delete(0, stringBuilder.length());

        final long minute = TimeUnit.MINUTES.toSeconds(1);
        if (s >= minute) {
            long minutes = s / minute;
            s -= minutes * minute;
            stringBuilder.append(minutes).append("m ").append(s).append("s");
        } else {
            stringBuilder.append(s).append("s");
        }

        return stringBuilder.toString();
    }

    /**
     * Formats specified millivolts.
     */
    public static String formatSignal(float mV) {
        stringBuilder.delete(0, stringBuilder.length());

        signalFormat.format(mV, stringBuilder, fieldPosition);
        return stringBuilder.append(" ").append(UNIT_MILLIVOLTS).toString();
    }
}
