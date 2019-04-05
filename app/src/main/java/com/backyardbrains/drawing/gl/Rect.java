package com.backyardbrains.drawing.gl;

/**
 * Represents position and size of an area within the gl surface view.
 *
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class Rect {
    public float x;
    public float y;
    public float width;
    public float height;

    public Rect() {
        this.x = 0f;
        this.y = 0;
        this.width = 0f;
        this.height = 0f;
    }

    public Rect(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void set(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void scale(float scaleX, float scaleY) {
        this.x *= scaleX;
        this.y *= scaleY;
        this.width *= scaleX;
        this.height *= scaleY;
    }

    boolean inside(float px, float py) {
        return px > getMinX() && py > getMinY() && px < getMaxX() && py < getMaxY();
    }

    private float getMinX() {
        return Math.min(x, x + width);
    }

    private float getMaxX() {
        return Math.max(x, x + width);
    }

    private float getMinY() {
        return Math.min(y, y + height);
    }

    private float getMaxY() {
        return Math.max(y, y + height);
    }
}