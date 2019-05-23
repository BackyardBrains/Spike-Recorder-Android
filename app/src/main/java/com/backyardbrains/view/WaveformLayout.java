package com.backyardbrains.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.backyardbrains.R;
import com.backyardbrains.drawing.BaseWaveformRenderer;
import com.backyardbrains.drawing.InteractiveGLSurfaceView;
import com.backyardbrains.ui.MainActivity;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class WaveformLayout extends ConstraintLayout {

    private static final String TAG = makeLogTag(WaveformLayout.class);

    @BindView(R.id.fl_container) FrameLayout flGL;
    @BindView(R.id.ibtn_zoom_in_h) ImageButton ibtnZoomInHorizontally;
    @BindView(R.id.ibtn_zoom_out_h) ImageButton ibtnZoomOutHorizontally;
    @BindView(R.id.ibtn_zoom_in_v) ImageButton ibtnZoomInVertically;
    @BindView(R.id.ibtn_zoom_out_v) ImageButton ibtnZoomOutVertically;

    private InteractiveGLSurfaceView glSurface;
    protected ZoomButton zoomInButtonH, zoomOutButtonH, zoomInButtonV, zoomOutButtonV;

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
    public void setRenderer(@NonNull BaseWaveformRenderer renderer) {
        LOGD(TAG, "setRenderer(" + renderer.getClass().getSimpleName() + ")");

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

    // Convenience method that casts context to BaseActivity
    MainActivity activity() {
        return (MainActivity) getContext();
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
        zoomInButtonH =
            new ZoomButton(getContext(), ibtnZoomInHorizontally, R.drawable.plus_button_active, R.drawable.plus_button,
                InteractiveGLSurfaceView.MODE_ZOOM_IN_H);
        zoomOutButtonH = new ZoomButton(getContext(), ibtnZoomOutHorizontally, R.drawable.minus_button_active,
            R.drawable.minus_button, InteractiveGLSurfaceView.MODE_ZOOM_OUT_H);
        zoomInButtonV =
            new ZoomButton(getContext(), ibtnZoomInVertically, R.drawable.plus_button_active, R.drawable.plus_button,
                InteractiveGLSurfaceView.MODE_ZOOM_IN_V);
        zoomOutButtonV =
            new ZoomButton(getContext(), ibtnZoomOutVertically, R.drawable.minus_button_active, R.drawable.minus_button,
                InteractiveGLSurfaceView.MODE_ZOOM_OUT_V);
    }

    private void showZoomUI(boolean bShow) {
        zoomInButtonV.setVisibility(bShow);
        zoomOutButtonV.setVisibility(bShow);
        zoomInButtonH.setVisibility(bShow);
        zoomOutButtonH.setVisibility(bShow);
    }
}
