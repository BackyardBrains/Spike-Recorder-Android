package com.backyardbrains.drawing;

import android.support.annotation.NonNull;
import android.view.MotionEvent;
import com.backyardbrains.BaseFragment;
import com.backyardbrains.drawing.GlGraphThumbTouchHelper.Rect;
import com.backyardbrains.events.RedrawAudioAnalysisEvent;
import com.backyardbrains.utils.AnalysisUtils;
import com.backyardbrains.utils.BYBGlUtils;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import org.greenrobot.eventbus.EventBus;

import static com.backyardbrains.utils.LogUtils.makeLogTag;

public abstract class BYBAnalysisBaseRenderer extends BaseRenderer implements TouchEnabledRenderer {

    private static final String TAG = makeLogTag(BYBAnalysisBaseRenderer.class);

    private static final float DEFAULT_MAX_GRAPH_THUMB_SIZE = 80f;
    static final float MARGIN = 20f;

    protected int surfaceWidth;
    protected int surfaceHeight;
    GlGraphThumbTouchHelper thumbTouchHelper = new GlGraphThumbTouchHelper();

    private float maxGraphThumbSize;

    //==============================================
    //  CONSTRUCTOR & SETUP
    //==============================================

    BYBAnalysisBaseRenderer(@NonNull BaseFragment fragment) {
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
        gl.glEnable(GL10.GL_BLEND); // Enable Alpha Blend
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA); // Set Alpha Blend Function
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

        thumbTouchHelper.setSurfaceHeight(surfaceHeight);

        gl.glViewport(0, 0, width, height);

        BYBGlUtils.glClear(gl);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrthof(0f, surfaceWidth, 0f, surfaceHeight, -1f, 1f);
        gl.glRotatef(0f, 0f, 0f, 0f);
    }

    /**
     * {@inheritDoc}
     */
    @Override public void onDrawFrame(GL10 gl) {
        //long start = System.currentTimeMillis();

        final int surfaceWidth = this.surfaceWidth;
        final int surfaceHeight = this.surfaceHeight;

        BYBGlUtils.glClear(gl);
        draw(gl, surfaceWidth, surfaceHeight);

        //LOGD(TAG, "" + (System.currentTimeMillis() - start));
        //LOGD(TAG, "================================================");
    }

    abstract protected void draw(GL10 gl, int surfaceWidth, int surfaceHeight);

    //=================================================
    //  TouchEnabledRenderer INTERFACE IMPLEMENTATIONS
    //=================================================

    @Override public void onTouchEvent(MotionEvent event) {
        boolean graphThumbTouched = thumbTouchHelper.onTouch(event);
        if (graphThumbTouched) EventBus.getDefault().post(new RedrawAudioAnalysisEvent());
    }

    /**
     * Creates rectangles for graph thumbs and main graph that will be used as configs for drawing
     */
    //void makeThumbRectangles(int surfaceWidth, int surfaceHeight) {
    //    int maxSpikeTrains = AnalysisUtils.MAX_SPIKE_TRAIN_COUNT;
    //    float thumbSize = (Math.min(surfaceWidth, surfaceHeight) - MARGIN * (maxSpikeTrains + 1)) / maxSpikeTrains;
    //
    //    // create rectangles for thumbs
    //    for (int i = 0; i < maxSpikeTrains; i++) {
    //        graphThumbs.add(new Rect(MARGIN, MARGIN + (thumbSize + MARGIN) * i, thumbSize, thumbSize));
    //    }
    //    // create main rectangle
    //    graph =
    //        new Rect(2 * MARGIN + thumbSize, MARGIN, surfaceWidth - 3 * MARGIN - thumbSize, surfaceHeight - 2 * MARGIN);
    //}

    /**
     * Returns default size for the graph thumb.
     */
    float getDefaultGraphThumbSize(int surfaceWidth, int surfaceHeight) {
        float result = (Math.min(surfaceWidth, surfaceHeight) - MARGIN * (AnalysisUtils.MAX_SPIKE_TRAIN_COUNT + 1))
            / AnalysisUtils.MAX_SPIKE_TRAIN_COUNT;
        if (result > maxGraphThumbSize) result = maxGraphThumbSize;

        return result;
    }

    // ----------------------------------------------------------------------------------------
    void graphIntegerList(GL10 gl, int[] ac, Rect r, float[] color, boolean bDrawBox) {
        graphIntegerList(gl, ac, r.x, r.y, r.width, r.height, color, bDrawBox);
    }

    // ----------------------------------------------------------------------------------------
    private void graphIntegerList(GL10 gl, int[] ac, float px, float py, float w, float h, float[] color,
        boolean bDrawBox) {
        if (ac != null) {
            if (ac.length > 0) {
                int s = ac.length;
                float[] values = new float[s];
                int mx = Integer.MIN_VALUE;
                for (int i = 0; i < s; i++) {
                    int y = ac[i];
                    if (mx < y) mx = y;
                }
                if (mx == 0) mx = 1;// avoid division by zero
                for (int i = 0; i < s; i++) {
                    values[i] = ((float) ac[i]) / (float) mx;
                }
                BYBBarGraph graph = new BYBBarGraph(values, px, py, w, h, color);
                if (bDrawBox) {
                    graph.makeBox(BYBColors.getColorAsGlById(BYBColors.white));
                }
                graph.setVerticalAxis(0, mx, 5);
                graph.setHorizontalAxis(0, ac.length, 6);
                graph.draw(gl);
            }
        }
    }
}
