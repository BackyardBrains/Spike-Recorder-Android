package com.backyardbrains.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by roy on 25-10-16.
 */
public class SlidingView {

    @SuppressWarnings("WeakerAccess") ObjectAnimator animShow;
    @SuppressWarnings("WeakerAccess") ObjectAnimator animHide;

    @SuppressWarnings("WeakerAccess") final View view;
    @SuppressWarnings("WeakerAccess") AnimationEndListener listener;

    @SuppressWarnings("WeakerAccess") int viewHeight;

    /**
     * Callback used to indicate when the show/hide animation is finished.
     */
    public interface AnimationEndListener {
        /**
         * Called when show animation of the view is finished.
         */
        void onShowAnimationEnd();

        /**
         * Called when hide animation of the view is finished.
         */
        void onHideAnimationEnd();
    }

    public SlidingView(@NonNull View view, @Nullable AnimationEndListener listener) {
        this.view = view;
        this.listener = listener;
    }

    public void setAnimationEndListener(@Nullable AnimationEndListener listener) {
        this.listener = listener;
    }

    public boolean show(boolean show) {
        if (animShow == null || animHide == null) setup();
        if (isShowing() != show) {
            if (show) {
                animShow.start();
            } else {
                animHide.start();
            }
            return true;
        }
        return false;
    }

    private boolean isShowing() {
        return view.getVisibility() == View.VISIBLE;
    }

    private void setup() {
        if (viewHeight == 0) determineViewHeight();
        animShow = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, -viewHeight, 0);
        animShow.setDuration(500);
        animShow.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationStart(Animator animation) {
                view.setVisibility(View.VISIBLE);
            }

            @Override public void onAnimationEnd(Animator animation) {
                if (listener != null) listener.onShowAnimationEnd();
            }
        });
        animHide = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 0, -viewHeight);
        animHide.setDuration(500);
        animHide.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                view.setVisibility(View.GONE);
                if (listener != null) listener.onHideAnimationEnd();
            }
        });
    }

    private void determineViewHeight() {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        viewHeight = params.height;
    }
}