package com.backyardbrains.utils;

import java.text.DecimalFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class Formats {

    private static final String UNIT_MILLIS = "msec";
    private static final String UNIT_SECS = "s";
    private static final String UNIT_MILLIVOLTS = "mV";
    private static final String FORMAT_MM_SS = "%02d:%02d";

    private static final DecimalFormat timeFormat = new DecimalFormat("#.0");
    private static final DecimalFormat signalFormat = new DecimalFormat("#.##");

    /**
     * Formats specified millisecond. If {@code ms} is < 1000 time is formatted as millis, else as seconds.
     */
    public static String formatTime_s_msec(float ms) {
        final float time = ms < 1000 ? ms : ms / 1000;
        final String unit = ms < 1000 ? UNIT_MILLIS : UNIT_SECS;
        return timeFormat.format(time) + " " + unit;
    }

    /**
     * Formats specified milliseconds as "mm:ss".
     */
    public static String formatTime_mm_ss(long ms) {
        return String.format(Locale.US, FORMAT_MM_SS, TimeUnit.MILLISECONDS.toMinutes(ms),
            TimeUnit.MILLISECONDS.toSeconds(ms) % TimeUnit.MINUTES.toSeconds(1));
    }

    /**
     * Formats specified seconds as "XX s" if {@code s} is less then 60 and "XXm XXs" if it's more.
     *
     * @param s seconds that need to be formatted.
     */
    public static String formatTime_m_s(long s) {
        final long minute = TimeUnit.MINUTES.toSeconds(1);
        if (s >= minute) {
            long minutes = s / minute;
            s -= minutes * minute;
            return minutes + "m " + s + "s";
        } else {
            return s + "s";
        }
    }

    /**
     * Formats specified millivolts.
     */
    public static String formatSignal(float mV) {
        return signalFormat.format(mV) + " " + UNIT_MILLIVOLTS;
    }
}
