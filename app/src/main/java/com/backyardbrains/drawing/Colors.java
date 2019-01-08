package com.backyardbrains.drawing;

import android.support.annotation.Size;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class Colors {
    public static final @Size(4) float[] BLACK = { 0f, 0f, 0f, 1f };
    public static final @Size(4) float[] WHITE = { 1f, 1f, 1f, 1f };
    public static final @Size(4) float[] RED = { 1f, 0f, 0f, 1f };
    public static final int RED_HEX = 0xffff0000;
    public static final @Size(4) float[] GREEN = { 0f, 1f, 0f, 1f };
    public static final int GREEN_HEX = 0xff00ff00;
    public static final @Size(4) float[] YELLOW = { 1f, 1f, 0f, 1f };
    public static final int YELLOW_HEX = 0xffffff00;
    public static final @Size(4) float[] CYAN = { 0f, 1f, 1f, 1f };
    public static final @Size(4) float[] MAGENTA = { 1f, 0f, 1f, 1f };
    public static final @Size(4) float[] GRAY = new float[] { .4f, .4f, .4f, .0f };
    public static final @Size(4) float[] GRAY_DARK = new float[] { .58824f, .58824f, .58824f, 1f };
    // channel colors
    public static final @Size(4) float[] CHANNEL_1 = GREEN;
    public static final @Size(4) float[] CHANNEL_2 = { 1f, 0.011764705882352941f, 0.011764705882352941f, 1f };
    public static final @Size(4) float[] CHANNEL_3 =
        { 0.9882352941176471f, 0.9372549019607843f, 0.011764705882352941f, 1f };
    public static final @Size(4) float[] CHANNEL_4 =
        { 0.9686274509803922f, 0.4980392156862745f, 0.011764705882352941f, 1f };
    public static final @Size(4) float[] CHANNEL_5 = MAGENTA;
    // marker colors
    private static final @Size(4) float[] MARKER_0 = new float[] { .847f, .706f, .906f, 1f };
    private static final @Size(4) float[] MARKER_1 = new float[] { 1f, .314f, 0f, 1f };
    private static final @Size(4) float[] MARKER_2 = new float[] { 1f, .925f, .58f, 1f };
    private static final @Size(4) float[] MARKER_3 = new float[] { 1f, .682f, .682f, 1f };
    private static final @Size(4) float[] MARKER_4 = new float[] { .69f, .898f, .486f, 1f };
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
