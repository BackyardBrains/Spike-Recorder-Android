package com.backyardbrains.drawing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLSurfaceView;
import androidx.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

public class TouchGlSurfaceView extends GLSurfaceView {

    private TouchEnabledRenderer renderer;

    public TouchGlSurfaceView(Context context) {
        this(context, null);
    }

    public TouchGlSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Sets the renderer associated with this view. Also starts the thread that will call the renderer, which in turn
     * causes the rendering to start.
     * <p>This method should be called once and only once in the life-cycle of a GLSurfaceView.
     * <p>The following GLSurfaceView methods can only be called <em>before</em> setRenderer is called:
     * <ul>
     *
     * <li>{@link #setEGLConfigChooser(boolean)}
     * <li>{@link #setEGLConfigChooser(EGLConfigChooser)}
     * <li>{@link #setEGLConfigChooser(int, int, int, int, int, int)}
     * </ul>
     * <p>
     * The following GLSurfaceView methods can only be called <em>after</em> setRenderer is called:
     * <ul>
     * <li>{@link #getRenderMode()}
     * <li>{@link #onPause()}
     * <li>{@link #onResume()}
     * <li>{@link #queueEvent(Runnable)}
     * <li>{@link #requestRender()}
     * <li>{@link #setRenderMode(int)}
     * </ul>
     *
     * @param renderer the renderer to use to perform OpenGL drawing.
     */
    public void setRenderer(@NonNull TouchEnabledRenderer renderer) {
        this.renderer = renderer;

        super.setRenderer(renderer);
    }

    @Override public final void setRenderer(Renderer renderer) {
        if (renderer instanceof TouchEnabledRenderer) {
            setRenderer(renderer);
            return;
        }

        throw new IllegalArgumentException("Renderer needs to be instance of BaseAnalysisRenderer class");
    }

    @Override public void surfaceCreated(SurfaceHolder holder) {
        super.surfaceCreated(holder);
        setKeepScreenOn(true);
    }

    @Override public void surfaceDestroyed(SurfaceHolder holder) {
        setKeepScreenOn(false);
        super.surfaceDestroyed(holder);
    }

    @SuppressLint("ClickableViewAccessibility") @Override public boolean onTouchEvent(MotionEvent event) {
        if (renderer != null) renderer.onTouchEvent(event);

        return true;
    }
}
