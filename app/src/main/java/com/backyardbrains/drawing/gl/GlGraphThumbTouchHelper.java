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

    private final List<Rect> touchableAreas = new ArrayList<>();

    private int height;
    private int selectedGraphThumb;
    private int tmpSelectedTouchableArea = NONE;

    /**
     * Sets the height of the drawable surface which is necessary for helper to calculate position correctly.
     */
    public void setSurfaceHeight(int height) {
        this.height = height;
    }

    /**
     * Registers a single touchable area.
     */
    public void registerTouchableArea(@NonNull Rect rect) {
        touchableAreas.add(rect);
    }

    /**
     * Returns index of the currently selected graph thumb.
     */
    public int getSelectedTouchableArea() {
        return selectedGraphThumb;
    }

    /**
     * Resets registered touchable areas.
     */
    public void resetTouchableAreas() {
        touchableAreas.clear();
    }

    /**
     * Handles specified {@code event} by checking whether any of the registered touchable areas has been clicked and
     * returns {@code true} if it was. It returns {@code false} otherwise.
     */
    public boolean onTouch(MotionEvent event) {
        if (event.getActionIndex() == 0) {
            int selected = getSelectedTouchableArea(event.getX(), event.getY());
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    tmpSelectedTouchableArea = selected;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (selected == NONE || selected != tmpSelectedTouchableArea) tmpSelectedTouchableArea = NONE;
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_OUTSIDE:
                    tmpSelectedTouchableArea = NONE;
                    break;
                case MotionEvent.ACTION_UP:
                    // valid click!!!
                    if (tmpSelectedTouchableArea != NONE && selected == tmpSelectedTouchableArea) {
                        selectedGraphThumb = selected;
                        return true;
                    }
                    break;
            }
        }

        return false;
    }

    // If one of the touchable areas is touched, area index is returned, -1 is returned otherwise.
    private int getSelectedTouchableArea(float x, float y) {
        for (int i = 0; i < touchableAreas.size(); i++) {
            if (touchableAreas.get(i) != null && touchableAreas.get(i).inside(x, height - y)) return i;
        }

        return NONE;
    }

    /**
     * Represents position and size of a touchable area within the surface view drawable. Renderers should register all
     * touchable areas through {@link #registerTouchableArea(Rect)} passing instance of this class as area
     * representation.
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
