package com.backyardbrains.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.SoundPool;
import android.support.annotation.ColorRes;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatDrawableManager;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.View;
import com.backyardbrains.R;
import com.backyardbrains.utils.AudioUtils;

/**
 * Pretty simple heartbeat pulsating view based on the small GitHub library (https://github.com/scottyab/HeartBeatView)
 *
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class HeartbeatView extends AppCompatImageView {

    private static final String TAG = "HeartBeatView";

    private static final float SCALE_FACTOR_NORMAL = 1f;
    private static final float SCALE_FACTOR_PUMPED = 1.2f;
    private static final int HEARTBEAT_ANIMATION_DURATION = 50;

    private Drawable heartDrawable;

    private SoundPool soundPool;
    private int soundId;
    private boolean isOn;
    private boolean isMuted;

    private final ValueAnimator.AnimatorUpdateListener animatorUpdateListener =
        new ValueAnimator.AnimatorUpdateListener() {
            @SuppressLint("RestrictedApi") @Override public void onAnimationUpdate(ValueAnimator animator) {
                heartDrawable.setColorFilter(
                    AppCompatDrawableManager.getPorterDuffColorFilter((int) animator.getAnimatedValue(),
                        PorterDuff.Mode.SRC_IN));
            }
        };

    public HeartbeatView(Context context) {
        this(context, null);
    }

    public HeartbeatView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HeartbeatView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        // create sound pool for heartbeat beep sound
        if (!isInEditMode()) {
            soundPool = AudioUtils.createSoundPool();
            soundId = soundPool.load(getContext(), R.raw.beep, 1);
        }
    }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        // release sound pool
        soundPool.release();
        soundPool = null;
    }

    /**
     * Animate heartbeat.
     */
    public void beep() {
        if (!isOn) {
            setImageDrawable(heartDrawable);
            isOn = true;
        }

        animate(SCALE_FACTOR_PUMPED, R.color.red, R.color.red_orange_75, HEARTBEAT_ANIMATION_DURATION,
            new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator animation) {
                    animate(SCALE_FACTOR_NORMAL, R.color.red_orange_75, R.color.red, HEARTBEAT_ANIMATION_DURATION * 2,
                        null);
                }
            });
        if (!isMuted) soundPool.play(soundId, 0.5f, 0.5f, 0, 0, 1);
    }

    /**
     * Sets heartbeat drawable in off state.
     */
    public void off() {
        isOn = false;
        setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_heartbeat_off));
    }

    /**
     * If {@code mute} is true sound is un-muted, else sound is muted.
     */
    public void setMuteSound(boolean mute) {
        isMuted = mute;
    }

    // Initializes UI
    private void init() {
        heartDrawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_favorite_red_orange_24dp);
        setImageDrawable(heartDrawable);
    }

    // Set's up heartbeat animation and triggers it.
    private void animate(float scale, @ColorRes int fromColor, @ColorRes int toColor, int duration,
        @Nullable Animator.AnimatorListener listener) {
        final ValueAnimator colorAnimation =
            ValueAnimator.ofObject(new ArgbEvaluator(), ContextCompat.getColor(getContext(), fromColor),
                ContextCompat.getColor(getContext(), toColor));
        colorAnimation.addUpdateListener(animatorUpdateListener);

        final AnimatorSet as = new AnimatorSet();
        as.setDuration(duration);
        as.playTogether(ObjectAnimator.ofFloat(HeartbeatView.this, View.SCALE_X, scale),
            ObjectAnimator.ofFloat(HeartbeatView.this, View.SCALE_Y, scale), colorAnimation);
        if (listener != null) as.addListener(listener);
        as.start();
    }
}