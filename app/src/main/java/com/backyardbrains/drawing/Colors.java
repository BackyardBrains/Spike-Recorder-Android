package com.backyardbrains.drawing;

import android.support.annotation.Size;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class Colors {
    public static final @Size(4) float[] BLACK = { 0f, 0f, 0f, 1f };
    public static final @Size(4) float[] WHITE = { 1f, 1f, 1f, 1f };
    public static final @Size(4) float[] RED = { 1f, 0f, 0f, 1f };
    public static final @Size(4) float[] GREEN = { 0f, 1f, 0f, 1f };
    public static final @Size(4) float[] CYAN = { 0f, 1f, 1f, 1f };
    public static final @Size(4) float[] MAGENTA = { 1f, 0f, 1f, 1f };
    public static final @Size(4) float[] GRAY = new float[] { .58824f, .58824f, .58824f, 1f };
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
        Colors.MARKER_0, Colors.MARKER_1, Colors.MARKER_2, Colors.MARKER_3, Colors.MARKER_4, Colors.MARKER_5,
        Colors.MARKER_6, Colors.MARKER_7, Colors.MARKER_8, Colors.MARKER_9
    };
}
