package com.backyardbrains.utils;

import android.support.annotation.NonNull;

import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class UsbUtils {

    private static final String TAG = makeLogTag(UsbUtils.class);

    // Board type SpikerShield reply message prefix.
    private static final String BOARD_TYPE_PREFIX = "HWT:";
    // Plant SpikerShield reply message for board type inquiry.
    private static final String BOARD_TYPE_PLANT = BOARD_TYPE_PREFIX + "PLANTSS;";
    // Muscle SpikerShield reply message for board type inquiry.
    private static final String BOARD_TYPE_MUSCLE = BOARD_TYPE_PREFIX + "MUSCLESS;";
    // Brain & Heart SpikerShield reply message for board type inquiry.
    private static final String BOARD_TYPE_HEART = BOARD_TYPE_PREFIX + "HEARTSS;";

    /**
     * Sample rate used throughout the app.
     */
    public static final int SAMPLE_RATE = 10000;

    /**
     * Whether specified {@code msg} sent from SpikerShield is a board type message.
     */
    public static boolean isBoardTypeMsg(@NonNull String msg) {
        return msg.startsWith(BOARD_TYPE_PREFIX);
    }

    /**
     * Returns SpikerShield board type depending on the specified {@code message}.
     */
    public static @SpikerBoxBoardType int getBoardType(@NonNull String message) {
        if (ObjectUtils.equals(BOARD_TYPE_PLANT, message)) return SpikerBoxBoardType.PLANT;
        if (ObjectUtils.equals(BOARD_TYPE_MUSCLE, message)) return SpikerBoxBoardType.MUSCLE;
        if (ObjectUtils.equals(BOARD_TYPE_HEART, message)) return SpikerBoxBoardType.HEART;
        return SpikerBoxBoardType.UNKNOWN;
    }
}
