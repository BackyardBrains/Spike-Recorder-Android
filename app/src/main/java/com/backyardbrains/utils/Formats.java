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

    private static final DecimalFormat TIME_FORMAT = new DecimalFormat("#.0");
    private static final DecimalFormat SIGNAL_FORMAT = new DecimalFormat("#.##");

    static {
        TIME_FORMAT.setMaximumFractionDigits(1);
    }

    private static final StringBuffer mMsecStringBuilder = new StringBuffer(10);
    private static final StringBuffer mmSsStringBuilder = new StringBuffer(10);
    private static final StringBuffer msStringBuilder = new StringBuffer(10);
    private static final StringBuffer signalStringBuilder = new StringBuffer(10);
    private static final FieldPosition fieldPosition = new FieldPosition(NumberFormat.FRACTION_FIELD);

    /**
     * Formats specified millisecond. If {@code ms} is < 1000 time is formatted as millis, else as seconds.
     */
    public static synchronized String formatTime_s_msec(float ms) {
        mMsecStringBuilder.setLength(0);

        final double time = ms < 1000 ? ms : ms / 1000;
        TIME_FORMAT.format(time, mMsecStringBuilder, fieldPosition);
        return mMsecStringBuilder.append(" ").append(ms < 1000 ? UNIT_MILLIS : UNIT_SECS).toString();
    }

    /**
     * Formats specified milliseconds as "mm:ss".
     */
    static synchronized String formatTime_mm_ss(long ms) {
        mmSsStringBuilder.setLength(0);

        long minutes = TimeUnit.MILLISECONDS.toMinutes(ms);
        if (minutes < 10) mmSsStringBuilder.append(0);
        mmSsStringBuilder.append(minutes).append(":");
        long seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % TimeUnit.MINUTES.toSeconds(1);
        if (seconds < 10) mmSsStringBuilder.append(0);
        mmSsStringBuilder.append(seconds);
        return mmSsStringBuilder.toString();
    }

    /**
     * Formats specified seconds as "XX s" if {@code s} is less then 60 and "XXm XXs" if it's more.
     *
     * @param s seconds that need to be formatted.
     */
    static synchronized String formatTime_m_s(long s) {
        msStringBuilder.delete(0, msStringBuilder.length());

        final long minute = TimeUnit.MINUTES.toSeconds(1);
        if (s >= minute) {
            long minutes = s / minute;
            s -= minutes * minute;
            msStringBuilder.append(minutes).append("m ").append(s).append("s");
        } else {
            msStringBuilder.append(s).append("s");
        }

        return msStringBuilder.toString();
    }

    /**
     * Formats specified millivolts.
     */
    public static synchronized String formatSignal(float mV) {
        signalStringBuilder.delete(0, signalStringBuilder.length());

        SIGNAL_FORMAT.format(mV, signalStringBuilder, fieldPosition);
        return signalStringBuilder.append(" ").append(UNIT_MILLIVOLTS).toString();
    }
}
