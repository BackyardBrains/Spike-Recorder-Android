package com.backyardbrains.drawing;

import android.support.annotation.NonNull;
import android.view.MotionEvent;
import com.backyardbrains.drawing.gl.GlGraphThumbTouchHelper;
import com.backyardbrains.events.RedrawAnalysisGraphEvent;
import com.backyardbrains.ui.BaseFragment;
import com.backyardbrains.utils.AnalysisUtils;
import com.backyardbrains.utils.GlUtils;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import org.greenrobot.eventbus.EventBus;

import static com.backyardbrains.utils.LogUtils.makeLogTag;

public abstract class BaseAnalysisRenderer extends BaseRenderer implements TouchEnabledRenderer {

    private static final String TAG = makeLogTag(BaseAnalysisRenderer.class);

    private static final float DEFAULT_MAX_GRAPH_THUMB_SIZE = 80f;
    static final float MARGIN = 30f;
    static final String SPIKE_TRAIN_THUMB_GRAPH_NAME_PREFIX = "ST";

    final GlGraphThumbTouchHelper glGraphThumbTouchHelper = new GlGraphThumbTouchHelper();

    private int surfaceWidth;
    private int surfaceHeight;

    private float maxGraphThumbSize;

    //==============================================
    //  CONSTRUCTOR & SETUP
    //==============================================

    BaseAnalysisRenderer(@NonNull BaseFragment fragment) {
        super(fragment);

        maxGraphThumbSize = DEFAULT_MAX_GRAPH_THUMB_SIZE * fragment.getResources().getDisplayMetrics().density;
    }

    /**
     * Cleans any occupied resources.
     */
    public void close() {
    }

    //=================================================
    //  Renderer INTERFACE IMPLEMENTATIONS
    //=================================================

    /**
     * {@inheritDoc}
     */
    @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        gl.glClearColor(0f, 0f, 0f, 1.0f);
        gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        gl.glDisable(GL10.GL_DEPTH_TEST);
        gl.glDisable(GL10.GL_DITHER);
        gl.glHint(GL10.GL_LINE_SMOOTH_HINT, GL10.GL_NICEST);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
    }

    /**
     * {@inheritDoc}
     */
    @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
        this.surfaceWidth = width;
        this.surfaceHeight = height;

        glGraphThumbTouchHelper.resetTouchableAreas();
        glGraphThumbTouchHelper.setSurfaceHeight(surfaceHeight);

        gl.glViewport(0, 0, width, height);

        GlUtils.glClear(gl);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrthof(0f, surfaceWidth, 0f, surfaceHeight, -1f, 1f);
        gl.glRotatef(0f, 0f, 0f, 0f);
    }

    /**
     * {@inheritDoc}
     */
    @Override public void onDrawFrame(GL10 gl) {
        final int surfaceWidth = this.surfaceWidth;
        final int surfaceHeight = this.surfaceHeight;

        GlUtils.glClear(gl);
        draw(gl, surfaceWidth, surfaceHeight);
    }

    abstract protected void draw(GL10 gl, int surfaceWidth, int surfaceHeight);

    //=================================================
    //  TouchEnabledRenderer INTERFACE IMPLEMENTATIONS
    //=================================================

    @Override public boolean onTouchEvent(MotionEvent event) {
        boolean graphThumbTouched = glGraphThumbTouchHelper.onTouch(event);
        if (graphThumbTouched) EventBus.getDefault().post(new RedrawAnalysisGraphEvent());

        return graphThumbTouched;
    }

    /**
     * Returns default size for the graph thumb.
     */
    float getDefaultGraphThumbSize(int surfaceWidth, int surfaceHeight, int thumbCount) {
        if (thumbCount < AnalysisUtils.MAX_SPIKE_TRAIN_COUNT) thumbCount = AnalysisUtils.MAX_SPIKE_TRAIN_COUNT;
        float result = (Math.min(surfaceWidth, surfaceHeight) - MARGIN * (thumbCount + 1)) / thumbCount;
        if (result > maxGraphThumbSize) result = maxGraphThumbSize;

        return result;
    }
}
