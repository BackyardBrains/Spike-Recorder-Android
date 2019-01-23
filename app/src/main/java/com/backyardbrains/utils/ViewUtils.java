package com.backyardbrains.utils;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.AppCompatDrawableManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;
import com.backyardbrains.R;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class ViewUtils {

    private static final int SOFT_KEYBOARD_DEFAULT_DELAY = 200;

    /**
     * Shows system default toast with the specified {@code message} for a {@link Toast#LENGTH_SHORT} duration.
     */
    public static Toast toast(@NonNull Context context, @NonNull String message) {
        return toast(context, message, Toast.LENGTH_SHORT);
    }

    /**
     * Shows system default toast with the specified {@code message} for a specified {@code duration}. Duration should
     * be either {@link Toast#LENGTH_SHORT} or {@link Toast#LENGTH_LONG}.
     */
    public static Toast toast(@NonNull Context context, @NonNull String message, int duration) {
        final Toast toast = Toast.makeText(context, message, duration);
        toast.show();

        return toast;
    }

    /**
     * Shows toast with the specified {@code message}. If formatted string needs to be shown, caller can pass {@code
     * args} that will be used when formatting the message.
     */
    @SuppressWarnings("UnusedReturnValue") public static Toast customToast(@NonNull FragmentActivity context,
        @NonNull String message) {
        final View layout =
            LayoutInflater.from(context).inflate(R.layout.layout_toast, (context).findViewById(R.id.toast_container));

        // set a message
        final TextView text = layout.findViewById(R.id.text);
        text.setText(message);

        // Toast...
        Toast toast = new Toast(context);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();

        return toast;
    }

    /**
     * Implements the correct behaviour to have a callback scheduled after Android has
     * laid out all components. It is the ideal time to play animations that need to
     * depend on layout being done to know starting positions of elements
     *
     * @param v View to listen for layout over
     * @param cb callback to play
     */
    public static void playBeforeNextDraw(final View v, final Func<View, ?> cb) {
        final ViewTreeObserver vto = v.getViewTreeObserver();
        if (vto == null) throw new RuntimeException("VTO not assigned! called too early :/");
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override public boolean onPreDraw() {
                // new to re-get observer since old one is stle
                v.getViewTreeObserver().removeOnPreDrawListener(this);
                return cb.apply(v) != null;
            }
        });
    }

    /**
     * Implements the correct behaviour to have a callback scheduled after Android has
     * laid out all components. This callback waits for next layout pass.
     *
     * @param v View to listen for layout over
     * @param cb callback to play
     */
    public static void playAfterNextLayout(final View v, final Func<View, Void> cb) {
        final ViewTreeObserver vto = v.getViewTreeObserver();
        if (vto == null) throw new RuntimeException("VTO not assigned! called too early :/");
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @SuppressLint("NewApi") @Override public void onGlobalLayout() {
                // new to re-get observer since old one is stle
                v.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                cb.apply(v);
            }
        });
    }

    /**
     * Hides software keyboard.
     */
    public static void hideSoftKeyboard(View v) {
        if (v != null) {
            InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    /**
     * Hides software keyboard with a specified delay. If delay is <= 0 default delay of 200 millis is applied.
     */
    public static void hideSoftKeyboard(final View v, int delay) {
        if (v != null) {
            if (delay <= 0) delay = SOFT_KEYBOARD_DEFAULT_DELAY;
            v.postDelayed(() -> {
                v.clearFocus();
                ViewUtils.hideSoftKeyboard(v);
            }, delay);
        }
    }

    /**
     * Shows software keyboard.
     */
    public static void openSoftKeyboard(View v) {
        if (v != null) {
            InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    /**
     * Shows software keyboard with a specified delay. If delay is <= 0 default delay of 200 millis is applied.
     */
    public static void openSoftKeyboard(final View v, int delay) {
        if (v != null) {
            if (delay <= 0) delay = SOFT_KEYBOARD_DEFAULT_DELAY;
            v.postDelayed(() -> {
                v.requestFocus();
                ViewUtils.openSoftKeyboard(v);
            }, delay);
        }
    }

    /**
     * Forcefully shows software keyboard.
     */
    public static void forceOpenSoftKeyboard(View v) {
        if (v != null) {
            InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(v, InputMethodManager.SHOW_FORCED);
        }
    }

    /**
     * Forcefully shows software keyboard with a specified delay. If delay is <= 0 default delay of 200 millis is
     * applied.
     */
    public static void forceOpenSoftKeyboard(final View v, int delay) {
        if (v != null) {
            if (delay <= 0) delay = SOFT_KEYBOARD_DEFAULT_DELAY;
            v.postDelayed(() -> {
                v.requestFocus();
                ViewUtils.forceOpenSoftKeyboard(v);
            }, delay);
        }
    }

    /**
     * Converts density-independent pixels to screen pixels.
     */
    public static float dpToPx(Resources res, float dpiValue) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpiValue, res.getDisplayMetrics());
    }

    /**
     * Converts density-independent pixels to screen pixels and returns them as integer.
     */
    public static int dpToPxInt(Resources res, float dpiValue) {
        return (int) dpToPx(res, dpiValue);
    }

    /**
     * Converts scale-independent pixels to screen pixels.
     */
    public static float spToPx(Resources res, float spValue) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spValue, res.getDisplayMetrics());
    }

    /**
     * Applies a {@code color} color filter to a specified {@code drawable}.
     */
    public static void tintDrawable(@NonNull Drawable drawable, @ColorInt int color) {
        drawable.setColorFilter(AppCompatDrawableManager.getPorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
    }

    /**
     * Animates background color transition on the specified {@code view} from {@code colorFrom} to {@code colorTo}.
     */
    public static void animateColorTransition(@NonNull final View view, @ColorInt int colorFrom,
        @ColorInt int colorTo) {
        final ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
        colorAnimation.addUpdateListener(animator -> view.setBackgroundColor((int) animator.getAnimatedValue()));
        colorAnimation.start();
    }
}