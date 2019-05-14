package com.backyardbrains.drawing;

public class BYBColors {

    private static final String TAG = "BYBColors";

    public static final int red = 0;
    public static final int green = 1;
    public static final int blue = 2;
    public static final int cyan = 3;
    public static final int magenta = 4;
    public static final int yellow = 5;
    public static final int orange = 6;
    public static final int gray = 7;
    public static final int white = 8;
    public static final int black = 9;
    // ----------------------------------------------------------------------------------------
    private static final float[][] colors = {
        { 1.0f, 0.0f, 0.0f, 1.0f },     // red
        { 0.0f, 1.0f, 0.0f, 1.0f },     // green
        { 0.0f, 0.0f, 1.0f, 1.0f },     // blue
        { 0.0f, 1.0f, 1.0f, 1.0f },     // cyan
        { 1.0f, 0.0f, 1.0f, 1.0f },     // magenta
        { 1.0f, 1.0f, 0.0f, 1.0f },     // yellow
        { 1.0f, 0.5f, 0.0f, 1.0f },     // orange?
        { 0.5f, 0.5f, 0.5f, 1.0f },     // gray
        { 1.0f, 1.0f, 1.0f, 1.0f },     // white
        { 0.0f, 0.0f, 0.0f, 1.0f },     // black
    };
}
