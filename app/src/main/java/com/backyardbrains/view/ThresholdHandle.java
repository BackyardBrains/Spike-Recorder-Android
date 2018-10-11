package com.backyardbrains.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.backyardbrains.R;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class ThresholdHandle extends ConstraintLayout {

    private static final String TAG = makeLogTag(ThresholdHandle.class);

    private static final int HANDLE_PADDING = 5;

    @Retention(RetentionPolicy.SOURCE) @IntDef({
        Orientation.LEFT, Orientation.RIGHT
    }) public @interface Orientation {
        /**
         * Handle is aligned left.
         */
        int LEFT = 0;
        /**
         * Handle is aligned right.
         */
        int RIGHT = 1;
    }

    @BindView(R.id.v_threshold_drag_surface) View vDragSurface;
    @BindView(R.id.iv_threshold_handle) ImageView ivHandle;
    @BindView(R.id.v_threshold_ruler) View vRuler;

    private @ColorInt int handleColor;
    private @Orientation int handleOrientation;
    private float handlePosition;
    private float topOffset;

    private int handlePadding;

    /**
     * Interface definition for a callback to be invoked when threshold handle changes position.
     */
    public interface OnThresholdChangeListener {
        /**
         * Listener that is invoked while handle is being dragged.
         */
        void onChange(@NonNull View view, float y);
    }

    private OnThresholdChangeListener listener;

    public ThresholdHandle(@NonNull Context context) {
        this(context, null);
    }

    public ThresholdHandle(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ThresholdHandle(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(attrs);
    }

    /**
     * Register a callback to be invoked when threshold handle position is changed.
     */
    public void setOnHandlePositionChangeListener(@Nullable OnThresholdChangeListener listener) {
        this.listener = listener;
    }

    /**
     * Sets color of the threshold handle.
     */
    public void setColor(@ColorInt int color) {
        if (handleColor == color) return;

        LOGD(TAG, "Handle color set: " + color);

        handleColor = color;
        ivHandle.setColorFilter(color);
        vRuler.setBackgroundColor(color);
    }

    /**
     * Set orientation of the threshold handle.
     */
    public void setOrientation(@Orientation int orientation) {
        if (handleOrientation == orientation) return;

        LOGD(TAG, "Handle orientation set: " + (orientation == Orientation.LEFT ? "left" : "right"));

        handleOrientation = orientation;

        updateUI();
    }

    /**
     * Set top offset of the threshold handle.
     */
    public void setTopOffset(float topOffset) {
        if (this.topOffset == topOffset) return;

        LOGD(TAG, "Top offset set: " + topOffset);

        this.topOffset = topOffset;

        updateUI();
    }

    /**
     * Sets Y position of the handle.
     */
    public float setPosition(float position) {
        handlePosition = position;

        updateUI();

        return handlePosition;
    }

    /**
     * Returns Y position of the handle.
     */
    public float getPosition() {
        return handlePosition;
    }

    // Initializes the view
    private void init(@Nullable AttributeSet attrs) {
        LayoutInflater.from(getContext()).inflate(R.layout.view_threshold_handle, this);
        ButterKnife.bind(this);

        // calculate handle padding
        handlePadding = (int) (HANDLE_PADDING * getResources().getDisplayMetrics().density);

        if (attrs != null) {
            final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ThresholdHandle);
            try {
                final @ColorRes int colorResId = a.getResourceId(R.styleable.ThresholdHandle_byb_color, R.color.white);
                setColor(ContextCompat.getColor(getContext(), colorResId));
                final int orientation = a.getInt(R.styleable.ThresholdHandle_byb_orientation, Orientation.LEFT);
                setOrientation(orientation == Orientation.RIGHT ? Orientation.RIGHT : Orientation.LEFT);
                final float topOffset = a.getFloat(R.styleable.ThresholdHandle_byb_topOffset, 0f);
                setTopOffset(topOffset);
            } finally {
                a.recycle();
            }
        }

        setupUI();
    }

    // Initializes view's children
    @SuppressLint("ClickableViewAccessibility") private void setupUI() {
        ivHandle.setOnTouchListener(
            new OnDragTouchListener(ivHandle, vDragSurface, new OnDragTouchListener.OnDragActionListener() {
                private float pos = 0f;

                @Override public void onDragStart(View view) {
                }

                @Override public void onDrag(View view, float x, float y) {
                    pos = setPosition(y + view.getHeight() / 2);
                }

                @Override public void onDragEnd(View view) {
                    invokeCallback(pos);
                }
            }));
    }

    // Updates view's children
    void updateUI() {
        LayoutParams lp = (LayoutParams) vDragSurface.getLayoutParams();
        lp.leftToLeft = handleOrientation == Orientation.LEFT ? LayoutParams.PARENT_ID : ivHandle.getId();
        lp.rightToRight = handleOrientation == Orientation.LEFT ? ivHandle.getId() : LayoutParams.PARENT_ID;
        if (topOffset > 0) {
            lp.topToTop = LayoutParams.UNSET;
            lp.height = (int) (getHeight() - topOffset);
        } else {
            lp.topToTop = LayoutParams.PARENT_ID;
            lp.height = LayoutParams.MATCH_CONSTRAINT;
        }
        vDragSurface.setLayoutParams(lp);

        lp = (LayoutParams) ivHandle.getLayoutParams();
        lp.leftToLeft = handleOrientation == Orientation.LEFT ? LayoutParams.PARENT_ID : LayoutParams.UNSET;
        lp.rightToRight = handleOrientation == Orientation.LEFT ? LayoutParams.UNSET : LayoutParams.PARENT_ID;
        ivHandle.setImageResource(
            handleOrientation == Orientation.LEFT ? R.drawable.handle_white_left : R.drawable.handle_white_right);
        ivHandle.setLayoutParams(lp);

        if (handlePosition < topOffset) {
            ivHandle.setRotation(handleOrientation == Orientation.LEFT ? -90 : 90);
            ivHandle.setX(handleOrientation == Orientation.LEFT ? vDragSurface.getWidth() - ivHandle.getWidth() / 2
                : vDragSurface.getX() - ivHandle.getWidth() / 2);
            ivHandle.setY(handlePadding + topOffset);
        } else if (handlePosition > vDragSurface.getBottom()) {
            ivHandle.setRotation(handleOrientation == Orientation.LEFT ? 90 : -90);
            ivHandle.setX(handleOrientation == Orientation.LEFT ? vDragSurface.getWidth() - ivHandle.getWidth() / 2
                : vDragSurface.getX() - ivHandle.getWidth() / 2);
            ivHandle.setY(vDragSurface.getBottom() - ivHandle.getHeight() - handlePadding);
        } else {
            ivHandle.setRotation(0);
            ivHandle.setX(handleOrientation == Orientation.LEFT ? 0 : vDragSurface.getX());
            ivHandle.setY(handlePosition - ivHandle.getHeight() / 2);
        }
        vRuler.setY(handlePosition - vRuler.getHeight() / 2);
    }

    // Invokes OnThresholdChangeListener.onChange() callback is set.
    void invokeCallback(float pos) {
        if (listener != null) listener.onChange(this, pos);
    }
}
