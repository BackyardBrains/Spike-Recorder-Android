package com.backyardbrains.drawing;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MotionEvent;
import com.backyardbrains.BaseFragment;
import com.backyardbrains.events.RedrawAudioAnalysisEvent;
import com.backyardbrains.utils.AnalysisUtils;
import com.backyardbrains.utils.BYBGlUtils;
import java.util.ArrayList;
import java.util.List;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import org.greenrobot.eventbus.EventBus;

import static com.backyardbrains.utils.LogUtils.makeLogTag;

public abstract class BYBAnalysisBaseRenderer extends BaseRenderer {

    private static final String TAG = makeLogTag(BYBAnalysisBaseRenderer.class);

    private static final float DEFAULT_MAX_GRAPH_THUMB_SIZE = 80f;
    static final float MARGIN = 20f;

    protected int surfaceWidth;
    protected int surfaceHeight;

    int selected = 0;

    Rect graph;
    List<Rect> graphThumbs = new ArrayList<>();
    private float maxGraphThumbSize;
    private int touchDownRect = -1;

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

    // -----------------------------------------------------------------------------------------------------------------------------
    // ----------------------------------------- TOUCH
    // -----------------------------------------------------------------------------------------------------------------------------
    private int checkInsideAllThumbRects(float x, float y) {
        if (graphThumbs != null) {
            for (int i = 0; i < graphThumbs.size(); i++) {
                if (graphThumbs.get(i) != null) {
                    if (graphThumbs.get(i).inside(x, surfaceHeight - y)) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    // ----------------------------------------------------------------------------------------
    boolean onTouchEvent(MotionEvent event) {
        if (event.getActionIndex() == 0) {
            int insideRect = checkInsideAllThumbRects(event.getX(), event.getY());
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    touchDownRect = insideRect;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (insideRect == -1 || insideRect != touchDownRect) touchDownRect = -1;
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_OUTSIDE:
                    touchDownRect = -1;
                    break;
                case MotionEvent.ACTION_UP:
                    //valid click!!!
                    if (insideRect == touchDownRect) thumbRectClicked(insideRect);
                    break;
            }
        }
        return false;
    }

    // ----------------------------------------------------------------------------------------
    protected void thumbRectClicked(int i) {
        setSelected(i);
    }

    //==============================================
    //  Renderer INTERFACE IMPLEMENTATIONS
    //==============================================

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

    /**
     * Creates rectangles for graph thumbs and main graph that will be used as configs for drawing
     */
    void makeThumbRectangles(int surfaceWidth, int surfaceHeight) {
        int maxSpikeTrains = AnalysisUtils.MAX_SPIKE_TRAIN_COUNT;
        float thumbSize = (Math.min(surfaceWidth, surfaceHeight) - MARGIN * (maxSpikeTrains + 1)) / maxSpikeTrains;

        // create rectangles for thumbs
        for (int i = 0; i < maxSpikeTrains; i++) {
            graphThumbs.add(new Rect(MARGIN, MARGIN + (thumbSize + MARGIN) * i, thumbSize, thumbSize));
        }
        // create main rectangle
        graph =
            new Rect(2 * MARGIN + thumbSize, MARGIN, surfaceWidth - 3 * MARGIN - thumbSize, surfaceHeight - 2 * MARGIN);
    }

    void registerGraph(@NonNull Rect rect) {
        graph = rect;
    }

    void registerThumb(@NonNull Rect rect) {
        graphThumbs.add(rect);
    }

    @Nullable Rect getThumb(int index) {
        return graphThumbs != null && graphThumbs.size() > index ? graphThumbs.get(index) : null;
    }

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

    int getSelectedGraph() {
        return selected;
    }

    private void setSelected(int s) {
        selected = s;

        EventBus.getDefault().post(new RedrawAudioAnalysisEvent());
    }

    /**
     * Represents
     */
    protected static class Rect {
        public float x;
        public float y;
        public float width;
        public float height;

        Rect(float x, float y, float w, float height) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = height;
        }

        boolean inside(float px, float py) {
            return px > getMinX() && py > getMinY() && px < getMaxX() && py < getMaxY();
        }

        private float getMinX() {
            return Math.min(x, x + width);
        }

        private float getMaxX() {
            return Math.max(x, x + width);
        }

        private float getMinY() {
            return Math.min(y, y + height);
        }

        private float getMaxY() {
            return Math.max(y, y + height);
        }
    }
}
