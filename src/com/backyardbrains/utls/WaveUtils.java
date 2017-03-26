package com.backyardbrains.utls;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class WaveUtils {

    public static CharSequence getWaveLengthString(long length) {
        length -= 44;

        long seconds = length / 88200;
        if (seconds >= 60) {
            long minutes = seconds / 60;
            seconds -= minutes * 60;
            return minutes + "m " + seconds + "s";
        } else {
            return seconds + "s";
        }
    }
}
