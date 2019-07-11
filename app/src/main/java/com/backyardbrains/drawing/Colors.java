package com.backyardbrains.drawing;

import android.support.annotation.ColorInt;
import android.support.annotation.Size;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class Colors {
    public static final @Size(4) float[] BLACK = { 0f, 0f, 0f, 1f };
    public static final @Size(4) float[] WHITE = { 1f, 1f, 1f, 1f };
    public static final @Size(4) float[] RED = { 1f, 0f, 0f, 1f };
    public static final @ColorInt int RED_HEX = 0xffff0000;
    public static final @Size(4) float[] GREEN = { 0f, 1f, 0f, 1f };
    public static final @ColorInt int GREEN_HEX = 0xff00ff00;
    public static final @Size(4) float[] BLUE = new float[] { 0f, 0f, 1f, 1f };
    public static final @Size(4) float[] BLUE_LIGHT = new float[] { 0f, 0.47843f, 1f, 1f };
    public static final @Size(4) float[] YELLOW = { 1f, 1f, 0f, 1f };
    public static final @ColorInt int YELLOW_HEX = 0xffffff00;
    public static final @Size(4) float[] CYAN = { 0f, 1f, 1f, 1f };
    public static final @Size(4) float[] MAGENTA = { 1f, 0f, 1f, 1f };
    public static final @Size(4) float[] GRAY_LIGHT = new float[] { 0.8f, 0.8f, 0.8f, 1f };
    public static final @Size(4) float[] GRAY = new float[] { .4f, .4f, .4f, .0f };
    public static final @Size(4) float[] GRAY_50 = new float[] { 0.4f, 0.4f, 0.4f, 0.5f };
    public static final @Size(4) float[] GRAY_DARK = new float[] { .58824f, .58824f, .58824f, 1f };
    // channel colors
    public static final @Size(4) float[] CHANNEL_0 = { 0f, 1f, 0.109f, 1f };
    public static final @Size(4) float[] CHANNEL_1 = { 1f, 0f, 0.231f, 1f };
    public static final @Size(4) float[] CHANNEL_2 = { 0.882f, 0.988f, 0.352f, 1f };
    public static final @Size(4) float[] CHANNEL_3 = { 1f, 0.541f, 0.356f, 1f };
    public static final @Size(4) float[] CHANNEL_4 = { 0.415f, 0.913f, 0.415f, 1f };
    public static final @Size(4) float[] CHANNEL_5 = { 0f, 0.745f, 0.784f, 1f };
    public static final float[][] CHANNEL_COLORS =
        new float[][] { CHANNEL_0, CHANNEL_1, CHANNEL_2, CHANNEL_3, CHANNEL_4, CHANNEL_5 };
    // marker colors
    private static final @ColorInt int MARKER_0_HEX = 0xffd8b4e7;
    private static final @ColorInt int MARKER_1_HEX = 0xffb0e57c;
    private static final @ColorInt int MARKER_2_HEX = 0xffff5000;
    private static final @ColorInt int MARKER_3_HEX = 0xffffec94;
    private static final @ColorInt int MARKER_4_HEX = 0xffffaeae;
    private static final @ColorInt int MARKER_5_HEX = 0xffb4d8e7;
    private static final @ColorInt int MARKER_6_HEX = 0xffc1dad6;
    private static final @ColorInt int MARKER_7_HEX = 0xffacd1e9;
    private static final @ColorInt int MARKER_8_HEX = 0xffaeffae;
    private static final @ColorInt int MARKER_9_HEX = 0xffffecff;
    public static final @ColorInt int[] MARKER_HEX_COLORS = new int[] {
        MARKER_0_HEX, MARKER_1_HEX, MARKER_2_HEX, MARKER_3_HEX, MARKER_4_HEX, MARKER_5_HEX, MARKER_6_HEX, MARKER_7_HEX,
        MARKER_8_HEX, MARKER_9_HEX
    };
    private static final @Size(4) float[] MARKER_0 = new float[] { .847f, .706f, .906f, 1f };
    private static final @Size(4) float[] MARKER_1 = new float[] { .69f, .898f, .486f, 1f };
    private static final @Size(4) float[] MARKER_2 = new float[] { 1f, .314f, 0f, 1f };
    private static final @Size(4) float[] MARKER_3 = new float[] { 1f, .925f, .58f, 1f };
    private static final @Size(4) float[] MARKER_4 = new float[] { 1f, .682f, .682f, 1f };
    private static final @Size(4) float[] MARKER_5 = new float[] { .706f, .847f, .906f, 1f };
    private static final @Size(4) float[] MARKER_6 = new float[] { .757f, .855f, .839f, 1f };
    private static final @Size(4) float[] MARKER_7 = new float[] { .675f, .82f, .914f, 1f };
    private static final @Size(4) float[] MARKER_8 = new float[] { .682f, 1f, .682f, 1f };
    private static final @Size(4) float[] MARKER_9 = new float[] { 1f, .925f, 1f, 1f };
    public static final float[][] MARKER_COLORS = new float[][] {
        MARKER_0, MARKER_1, MARKER_2, MARKER_3, MARKER_4, MARKER_5, MARKER_6, MARKER_7, MARKER_8, MARKER_9
    };
    // train colors
    private static final @Size(4) float[] SPIKE_TRAIN_1 = RED;
    private static final @Size(4) float[] SPIKE_TRAIN_2 = YELLOW;
    private static final @Size(4) float[] SPIKE_TRAIN_3 = GREEN;
    public static final float[][] SPIKE_TRAIN_COLORS = new float[][] { SPIKE_TRAIN_1, SPIKE_TRAIN_2, SPIKE_TRAIN_3 };
}
