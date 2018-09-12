package com.backyardbrains.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

/**
 * Created by roy on 25-10-16.
 */
public class SlidingView {

    @SuppressWarnings("WeakerAccess") boolean bAnimating;
    @SuppressWarnings("WeakerAccess") ObjectAnimator animShow1;
    @SuppressWarnings("WeakerAccess") ObjectAnimator animHide1;

    View view;
    @SuppressWarnings("WeakerAccess") AnimationEndListener listener;

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

    public boolean show(boolean bShow) {
        if (view == null) return false;
        if (animShow1 == null || animHide1 == null) setup();
        if ((isShowing() != bShow) && !bAnimating) {
            if (bShow) {
                animShow1.start();
            } else {
                animHide1.start();
            }
            return true;
        }
        return false;
    }

    private boolean isShowing() {
        return view != null && view.getVisibility() == View.VISIBLE;
    }

    private void setup() {
        animShow1 = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, -174, 0);
        animShow1.setDuration(500);
        animShow1.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationStart(Animator animation) {
                if (SlidingView.this.view == null) return;
                bAnimating = true;
                SlidingView.this.view.setVisibility(View.VISIBLE);
            }

            @Override public void onAnimationEnd(Animator animation) {
                if (SlidingView.this.view == null) return;
                bAnimating = false;
                if (listener != null) listener.onShowAnimationEnd();
            }
        });
        animHide1 = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 0, -174);
        animHide1.setDuration(500);
        animHide1.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationStart(Animator animation) {
                if (SlidingView.this.view == null) return;
                bAnimating = true;
            }

            @Override public void onAnimationEnd(Animator animation) {
                if (SlidingView.this.view == null) return;
                bAnimating = false;
                SlidingView.this.view.setVisibility(View.GONE);
                if (listener != null) listener.onHideAnimationEnd();
            }
        });
    }
}
