package com.backyardbrains.utls;

import java.text.DecimalFormat;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class Formats {

    private static final String UNIT_MILLIS = "msec";
    private static final String UNIT_SECS = "s";

    private static final DecimalFormat timeFormat = new DecimalFormat("#.0");

    /**
     * Formats specified millisecond. If {@code ms} is < 1000 time is formatted as millis, else as seconds.
     */
    public static String formatTime(float ms) {
        final float time = ms < 1000 ? ms : ms / 1000;
        final String unit = ms < 1000 ? UNIT_MILLIS : UNIT_SECS;
        return timeFormat.format(time) + " " + unit;
    }
}
