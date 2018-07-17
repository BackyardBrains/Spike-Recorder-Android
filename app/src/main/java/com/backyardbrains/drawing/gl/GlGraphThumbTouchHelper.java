package com.backyardbrains.drawing.gl;

import android.support.annotation.NonNull;
import android.view.MotionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class GlGraphThumbTouchHelper {

    private static final int NONE = -1;

    private final List<Rect> graphThumbs = new ArrayList<>();

    private int height;
    private int selectedGraphThumb;
    private int tmpSelectedGraphThumb = NONE;

    /**
     * Sets the height of the drawable surface which is necessary for helper to calculate position correctly.
     */
    public void setSurfaceHeight(int height) {
        this.height = height;
    }

    /**
     * Registers a single touchable graph thumb.
     */
    public void registerGraphThumb(@NonNull Rect rect) {
        graphThumbs.add(rect);
    }

    /**
     * Returns index of the currently selected graph thumb.
     */
    public int getSelectedGraphThumb() {
        return selectedGraphThumb;
    }

    /**
     * Resets registered thumbs.
     */
    public void resetGraphThumbs() {
        graphThumbs.clear();
    }

    /**
     * Handles specified {@code event} by checking whether any of the registered graph thumbs has been clicked and
     * returns {@code true} if it was, {@code false} otherwise.
     */
    public boolean onTouch(MotionEvent event) {
        if (event.getActionIndex() == 0) {
            int selected = getSelectedGraphThumb(event.getX(), event.getY());
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    tmpSelectedGraphThumb = selected;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (selected == NONE || selected != tmpSelectedGraphThumb) tmpSelectedGraphThumb = NONE;
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_OUTSIDE:
                    tmpSelectedGraphThumb = NONE;
                    break;
                case MotionEvent.ACTION_UP:
                    // valid click!!!
                    if (tmpSelectedGraphThumb != NONE && selected == tmpSelectedGraphThumb) {
                        selectedGraphThumb = selected;
                        return true;
                    }
                    break;
            }
        }

        return false;
    }

    // If one of the graph thumbs is touched thumb index is returned, -1 is returned otherwise.
    private int getSelectedGraphThumb(float x, float y) {
        for (int i = 0; i < graphThumbs.size(); i++) {
            if (graphThumbs.get(i) != null && graphThumbs.get(i).inside(x, height - y)) return i;
        }

        return NONE;
    }

    /**
     * Represents position and size of a graph thumb within the surface view drawable. Renderers should register all
     * touchable thumbs through {@link #registerGraphThumb(Rect)} passing instance of this class a thumb representation.
     */
    public static class Rect {
        public float x;
        public float y;
        public float width;
        public float height;

        public Rect(float x, float y, float w, float height) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = height;
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
}
