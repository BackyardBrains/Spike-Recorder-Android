package com.backyardbrains.drawing;

import android.support.annotation.NonNull;
import android.view.MotionEvent;
import com.backyardbrains.BaseFragment;
import com.backyardbrains.events.RedrawAudioAnalysisEvent;
import com.backyardbrains.utils.BYBGlUtils;
import com.backyardbrains.view.ofRectangle;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import org.greenrobot.eventbus.EventBus;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public abstract class BYBAnalysisBaseRenderer extends BaseRenderer {

    private static final String TAG = makeLogTag(BYBAnalysisBaseRenderer.class);

    protected int surfaceWidth;
    protected int surfaceHeight;

    ofRectangle mainRect;
    ofRectangle[] thumbRects;
    int selected = 0;

    private int touchDownRect = -1;

    //==============================================
    //  CONSTRUCTOR & SETUP
    //==============================================

    BYBAnalysisBaseRenderer(@NonNull BaseFragment fragment) {
        super(fragment);
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
        if (thumbRects != null) {
            for (int i = 0; i < thumbRects.length; i++) {
                if (thumbRects[i] != null) {
                    if (thumbRects[i].inside(x, y)) {
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
        gl.glOrthof(0f, surfaceWidth, surfaceHeight, 0f, -1f, 1f);
        gl.glRotatef(0f, 0f, 0f, 1f);
    }

    /**
     * {@inheritDoc}
     */
    @Override public void onDrawFrame(GL10 gl) {
        long start = System.currentTimeMillis();

        final int surfaceWidth = this.surfaceWidth;
        final int surfaceHeight = this.surfaceHeight;

        BYBGlUtils.glClear(gl);
        draw(gl, surfaceWidth, surfaceHeight);

        LOGD(TAG, "" + (System.currentTimeMillis() - start));
        LOGD(TAG, "================================================");
    }

    abstract protected void draw(GL10 gl, int surfaceWidth, int surfaceHeight);

    // ----------------------------------------------------------------------------------------
    void initGL(GL10 gl) {
        // set viewport
        gl.glViewport(0, 0, surfaceWidth, surfaceHeight);

        BYBGlUtils.glClear(gl);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrthof(0f, surfaceWidth, surfaceHeight, 0f, -1f, 1f);
        gl.glRotatef(0f, 0f, 0f, 1f);
    }

    /**
     * Creates rectangles for graph thumbs and main graph that will be used as configs for drawing
     */
    void makeThumbAndMainRectangles() {
        int maxSpikeTrains = 3;
        float margin = 20f;
        float thumbSize = (Math.min(surfaceWidth, surfaceHeight) - margin * (maxSpikeTrains + 1)) / maxSpikeTrains;

        // create rectangles for thumbs
        thumbRects = new ofRectangle[maxSpikeTrains];
        for (int i = 0; i < maxSpikeTrains; i++) {
            thumbRects[i] = new ofRectangle(margin, margin + (thumbSize + margin) * i, thumbSize, thumbSize);
        }
        // create main rectangle
        mainRect = new ofRectangle(2 * margin + thumbSize, margin, surfaceWidth - 3 * margin - thumbSize,
            surfaceHeight - 2 * margin);
    }

    // ----------------------------------------------------------------------------------------
    void graphIntegerList(GL10 gl, int[] ac, ofRectangle r, float[] color, boolean bDrawBox) {
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

    private void setSelected(int s) {
        selected = s;

        EventBus.getDefault().post(new RedrawAudioAnalysisEvent());
    }
}
