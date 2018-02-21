package com.backyardbrains.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.backyardbrains.BackyardBrainsMain;
import com.backyardbrains.R;
import com.backyardbrains.drawing.BYBBaseRenderer;
import com.backyardbrains.drawing.InteractiveGLSurfaceView;
import com.backyardbrains.utils.Formats;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class WaveformLayout extends ConstraintLayout {

    private static final String TAG = makeLogTag(WaveformLayout.class);

    @BindView(R.id.fl_container) FrameLayout flGL;
    @BindView(R.id.tv_signal) TextView tvSignal;
    @BindView(R.id.tv_time) TextView tvTime;
    @BindView(R.id.v_time_scale) View vTimeScale;
    @BindView(R.id.ibtn_zoom_in_h) ImageButton ibtnZoomInHorizontally;
    @BindView(R.id.ibtn_zoom_out_h) ImageButton ibtnZoomOutHorizontally;
    @BindView(R.id.ibtn_zoom_in_v) ImageButton ibtnZoomInVertically;
    @BindView(R.id.ibtn_zoom_out_v) ImageButton ibtnZoomOutVertically;

    private InteractiveGLSurfaceView glSurface;
    private BYBBaseRenderer renderer;
    protected BYBZoomButton zoomInButtonH, zoomOutButtonH, zoomInButtonV, zoomOutButtonV;

    private float millivolts;
    private float milliseconds;

    public WaveformLayout(Context context) {
        this(context, null);
    }

    public WaveformLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WaveformLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    /**
     * Resumes GL surface view
     */
    public void resumeGL() {
        if (glSurface != null) glSurface.onResume();
    }

    /**
     * Pauses GL surface view
     */
    public void pauseGL() {
        if (glSurface != null) glSurface.onPause();
    }

    /**
     * Initializes the surface view for drawing using specified {@code renderer}.
     */
    public void setRenderer(@NonNull BYBBaseRenderer renderer) {
        LOGD(TAG, "setRenderer()");

        if (flGL != null) {
            flGL.removeAllViews();
            // create new GL surface
            if (glSurface != null) glSurface = null;
            glSurface = new InteractiveGLSurfaceView(getContext());
            glSurface.setRenderer(renderer);
            // and add GL surface to UI
            flGL.addView(glSurface);
        }
    }

    /**
     * Updates time text view
     */
    public void setMilliseconds(final float milliseconds) {
        if (this.milliseconds == milliseconds) return;

        this.milliseconds = milliseconds;
        tvTime.setText(Formats.formatTime_s_msec(milliseconds));
    }

    /**
     * Updates signal text view
     */
    public void setMillivolts(final float millivolts) {
        if (this.millivolts == millivolts) return;

        this.millivolts = millivolts;
        tvSignal.setText(Formats.formatSignal(millivolts));
    }

    // Convenience method that casts context to BaseActivity
    BackyardBrainsMain activity() {
        return (BackyardBrainsMain) getContext();
    }

    // Initializes the view
    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.layout_waveform, this);
        ButterKnife.bind(this);

        setupUI();
    }

    // Initializes view's children
    private void setupUI() {
        setupZoomButtons();
        showZoomUI(!activity().isTouchSupported());
    }

    private void setupZoomButtons() {
        zoomInButtonH = new BYBZoomButton(getContext(), ibtnZoomInHorizontally, R.drawable.plus_button_active,
            R.drawable.plus_button, InteractiveGLSurfaceView.MODE_ZOOM_IN_H);
        zoomOutButtonH = new BYBZoomButton(getContext(), ibtnZoomOutHorizontally, R.drawable.minus_button_active,
            R.drawable.minus_button, InteractiveGLSurfaceView.MODE_ZOOM_OUT_H);
        zoomInButtonV =
            new BYBZoomButton(getContext(), ibtnZoomInVertically, R.drawable.plus_button_active, R.drawable.plus_button,
                InteractiveGLSurfaceView.MODE_ZOOM_IN_V);
        zoomOutButtonV = new BYBZoomButton(getContext(), ibtnZoomOutVertically, R.drawable.minus_button_active,
            R.drawable.minus_button, InteractiveGLSurfaceView.MODE_ZOOM_OUT_V);
    }

    private void showZoomUI(boolean bShow) {
        zoomInButtonV.setVisibility(bShow);
        zoomOutButtonV.setVisibility(bShow);
        zoomInButtonH.setVisibility(bShow);
        zoomOutButtonH.setVisibility(bShow);
    }
}
