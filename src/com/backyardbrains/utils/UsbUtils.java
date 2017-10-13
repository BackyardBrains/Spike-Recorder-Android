package com.backyardbrains.utils;

import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class UsbUtils {

    private static final String TAG = makeLogTag(UsbUtils.class);

    /**
     * Sample rate used throughout the app.
     */
    public static final int SAMPLE_RATE = 10000;

    /**
     * Plant SpikerShield reply message for board type inquiry.
     */
    public static final String BOARD_TYPE_PLANT = "HWT:PLANTSS;";

    /**
     * Muscle SpikerShield reply message for board type inquiry.
     */
    public static final String BOARD_TYPE_MUSCLE = "HWT:MUSCLESS;";

    /**
     * Brain & Heart SpikerShield reply message for board type inquiry.
     */
    public static final String BOARD_TYPE_HEART = "HWT:HEARTSS;";
}
