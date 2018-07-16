package com.backyardbrains.view;

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
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.backyardbrains.R;
import com.backyardbrains.utils.Func;
import com.backyardbrains.utils.ViewUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class BYBThresholdHandle extends ConstraintLayout {

    private static final String TAG = makeLogTag(BYBThresholdHandle.class);

    private static final int HANDLE_PADDING = 5;

    @Retention(RetentionPolicy.SOURCE) @IntDef({
        Orientation.LEFT, Orientation.RIGHT
    }) @interface Orientation {
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

    public BYBThresholdHandle(@NonNull Context context) {
        this(context, null);
    }

    public BYBThresholdHandle(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BYBThresholdHandle(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
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

        LayoutParams lp = (LayoutParams) vDragSurface.getLayoutParams();
        lp.leftToLeft = orientation == Orientation.LEFT ? LayoutParams.PARENT_ID : ivHandle.getId();
        lp.rightToRight = orientation == Orientation.LEFT ? ivHandle.getId() : LayoutParams.PARENT_ID;
        vDragSurface.setLayoutParams(lp);

        lp = (LayoutParams) ivHandle.getLayoutParams();
        lp.leftToLeft = orientation == Orientation.LEFT ? LayoutParams.PARENT_ID : LayoutParams.UNSET;
        lp.rightToRight = orientation == Orientation.LEFT ? LayoutParams.UNSET : LayoutParams.PARENT_ID;
        ivHandle.setImageResource(
            orientation == Orientation.LEFT ? R.drawable.handle_white_left : R.drawable.handle_white_right);
        ivHandle.setLayoutParams(lp);
    }

    /**
     * Sets Y position of the handle.
     */
    public float setPosition(float position) {
        handlePosition = position;
        updateUI();

        return handlePosition;
    }

    // Initializes the view
    private void init(@Nullable AttributeSet attrs) {
        LayoutInflater.from(getContext()).inflate(R.layout.view_threshold_handle, this);
        ButterKnife.bind(this);

        setVisibility(INVISIBLE);

        // calculate handle padding
        handlePadding = (int) (HANDLE_PADDING * getResources().getDisplayMetrics().density);

        if (attrs != null) {
            final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.BYBThresholdHandle);
            try {
                final @ColorRes int colorResId =
                    a.getResourceId(R.styleable.BYBThresholdHandle_byb_color, R.color.white);
                setColor(ContextCompat.getColor(getContext(), colorResId));
                final int orientationInt = a.getInt(R.styleable.BYBThresholdHandle_byb_orientation, Orientation.LEFT);
                setOrientation(orientationInt == Orientation.RIGHT ? Orientation.RIGHT : Orientation.LEFT);
            } finally {
                a.recycle();
            }
        }

        setupUI();
    }

    // Initializes view's children
    private void setupUI() {
        vDragSurface.setOnTouchListener(new OnTouchListener() {

            @Override public boolean onTouch(View v, MotionEvent event) {
                if (v.getVisibility() == View.VISIBLE) {
                    // always track only the first pointer
                    if (event.getActionIndex() == 0) {
                        final float pos = setPosition(event.getY());
                        if (event.getActionMasked() == MotionEvent.ACTION_UP) invokeCallback(pos);
                    }

                    return true;
                }
                return false;
            }
        });

        ViewUtils.playAfterNextLayout(this, new Func<View, Void>() {
            @Nullable @Override public Void apply(@Nullable View source) {
                updateUI();
                post(new Runnable() {
                    @Override public void run() {
                        setVisibility(VISIBLE);
                    }
                });
                return null;
            }
        });
    }

    // Updates view's children
    private void updateUI() {
        if (handlePosition < 0) {
            ivHandle.setRotation(handleOrientation == Orientation.LEFT ? -90 : 90);
            ivHandle.setX(handleOrientation == Orientation.LEFT ? vDragSurface.getWidth() - ivHandle.getWidth() / 2
                : vDragSurface.getX() - ivHandle.getWidth() / 2);
            ivHandle.setY(handlePadding);
        } else if (handlePosition > vDragSurface.getHeight()) {
            ivHandle.setRotation(handleOrientation == Orientation.LEFT ? 90 : -90);
            ivHandle.setX(handleOrientation == Orientation.LEFT ? vDragSurface.getWidth() - ivHandle.getWidth() / 2
                : vDragSurface.getX() - ivHandle.getWidth() / 2);
            ivHandle.setY(vDragSurface.getHeight() - ivHandle.getHeight() - handlePadding);
        } else {
            ivHandle.setRotation(0);
            ivHandle.setX(handleOrientation == Orientation.LEFT ? 0 : vDragSurface.getX());
            ivHandle.setY(handlePosition - ivHandle.getHeight() / 2);
        }
        vRuler.setY(handlePosition - vRuler.getHeight());
    }

    // Invokes OnThresholdChangeListener.onChange() callback is set.
    private void invokeCallback(float pos) {
        if (listener != null) listener.onChange(this, pos);
    }
}
