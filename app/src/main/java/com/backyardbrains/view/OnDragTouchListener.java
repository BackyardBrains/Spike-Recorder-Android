package com.backyardbrains.view;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class OnDragTouchListener implements View.OnTouchListener {

    /**
     * Callback used to indicate when the drag is finished
     */
    public interface OnDragActionListener {
        /**
         * Called when drag event is started
         *
         * @param view The view dragged
         */
        void onDragStart(View view);

        /**
         * Called during drag event
         *
         * @param view The view dragged
         * @param x x coordinate of the dragged view
         * @param y y coordinate of the dragged view
         */
        void onDrag(View view, float x, float y);

        /**
         * Called when drag event is completed
         *
         * @param view The view dragged
         */
        void onDragEnd(View view);
    }

    private View mView;
    private View mParent;
    private boolean isDragging;
    private boolean isInitialized = false;

    private int width;
    private float maxLeft;
    private float maxRight;
    private float dX;

    private int height;
    private float maxTop;
    private float maxBottom;
    private float dY;

    private OnDragActionListener mOnDragActionListener;

    @SuppressWarnings("unused") public OnDragTouchListener(View view) {
        this(view, (View) view.getParent(), null);
    }

    @SuppressWarnings("unused") public OnDragTouchListener(View view, View parent) {
        this(view, parent, null);
    }

    @SuppressWarnings("unused") public OnDragTouchListener(View view, OnDragActionListener onDragActionListener) {
        this(view, (View) view.getParent(), onDragActionListener);
    }

    OnDragTouchListener(View view, View parent, OnDragActionListener onDragActionListener) {
        initListener(view, parent);
        setOnDragActionListener(onDragActionListener);
    }

    private void setOnDragActionListener(OnDragActionListener onDragActionListener) {
        mOnDragActionListener = onDragActionListener;
    }

    private void initListener(View view, View parent) {
        mView = view;
        mParent = parent;
        isDragging = false;
        isInitialized = false;
    }

    private void updateBounds() {
        updateViewBounds();
        updateParentBounds();
        isInitialized = true;
    }

    private void updateViewBounds() {
        width = mView.getWidth();
        dX = 0;

        height = mView.getHeight();
        dY = 0;
    }

    private void updateParentBounds() {
        maxLeft = mParent.getX();
        maxRight = maxLeft + mParent.getWidth();

        maxTop = mParent.getY()/* - mView.getHeight() / 2*/;
        maxBottom = maxTop + mParent.getHeight()/* + mView.getHeight()*/;
    }

    @SuppressLint("ClickableViewAccessibility") @Override public boolean onTouch(View v, MotionEvent event) {
        if (isDragging) {
            float[] bounds = new float[4];
            // LEFT
            bounds[0] = event.getRawX() + dX;
            if (bounds[0] < maxLeft) bounds[0] = maxLeft;
            // RIGHT
            bounds[2] = bounds[0] + width;
            if (bounds[2] > maxRight) {
                bounds[2] = maxRight;
                bounds[0] = bounds[2] - width;
            }
            // TOP
            bounds[1] = event.getRawY() + dY;
            if (bounds[1] < maxTop) bounds[1] = maxTop;
            // BOTTOM
            bounds[3] = bounds[1] + height;
            if (bounds[3] > maxBottom) {
                bounds[3] = maxBottom;
                bounds[1] = bounds[3] - height;
            }

            switch (event.getAction()) {
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    onDragFinish();
                    break;
                case MotionEvent.ACTION_MOVE:
                    mView.animate().x(bounds[0]).y(bounds[1]).setDuration(0).start();
                    if (mOnDragActionListener != null) mOnDragActionListener.onDrag(mView, bounds[0], bounds[1]);
                    break;
            }
            return true;
        } else {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isDragging = true;
                    if (!isInitialized) updateBounds();

                    dX = v.getX() - event.getRawX();
                    dY = v.getY() - event.getRawY();
                    if (mOnDragActionListener != null) mOnDragActionListener.onDragStart(mView);

                    return true;
            }
        }
        return false;
    }

    private void onDragFinish() {
        if (mOnDragActionListener != null) mOnDragActionListener.onDragEnd(mView);

        dX = 0;
        dY = 0;
        isDragging = false;
    }
}
