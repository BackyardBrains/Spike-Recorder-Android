package com.backyardbrains.drawing;

import android.content.Context;
import android.support.annotation.NonNull;
import com.backyardbrains.BaseFragment;
import com.backyardbrains.drawing.GlGraphThumbTouchHelper.Rect;
import com.backyardbrains.utils.AnalysisUtils;
import com.backyardbrains.utils.GlUtils;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class AutoCorrelationRenderer extends BYBAnalysisBaseRenderer {

    static final String TAG = makeLogTag(AutoCorrelationRenderer.class);

    private static final float[] H_GRAPH_AXIS_VALUES = new float[6];

    static {
        H_GRAPH_AXIS_VALUES[0] = 0f;
        H_GRAPH_AXIS_VALUES[1] = .02f;
        H_GRAPH_AXIS_VALUES[2] = .04f;
        H_GRAPH_AXIS_VALUES[3] = .06f;
        H_GRAPH_AXIS_VALUES[4] = .08f;
        H_GRAPH_AXIS_VALUES[5] = .1f;
    }

    private static final String[] SPIKE_TRAIN_THUMB_GRAPH_NAMES = new String[AnalysisUtils.MAX_SPIKE_TRAIN_COUNT];

    static {
        SPIKE_TRAIN_THUMB_GRAPH_NAMES[0] = "ST1";
        SPIKE_TRAIN_THUMB_GRAPH_NAMES[1] = "ST2";
        SPIKE_TRAIN_THUMB_GRAPH_NAMES[2] = "ST3";
    }

    private GlBarGraph glBarGraph;
    private GlBarGraphThumb glBarGraphThumb;
    private Context context;

    @SuppressWarnings("WeakerAccess") int[][] autocorrelationAnalysis;

    public AutoCorrelationRenderer(@NonNull BaseFragment fragment) {
        super(fragment);

        context = fragment.getContext();
    }

    /**
     * {@inheritDoc}
     */
    @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        super.onSurfaceCreated(gl, config);

        glBarGraph = new GlBarGraph(context, gl);
        glBarGraphThumb = new GlBarGraphThumb(context, gl);
    }

    @Override protected void draw(GL10 gl, int surfaceWidth, int surfaceHeight) {
        if (getAutocorrelationAnalysis()) {
            int len = autocorrelationAnalysis.length;
            if (len > 0) {
                final float thumbSize = getDefaultGraphThumbSize(surfaceWidth, surfaceHeight);
                boolean portraitOrientation = surfaceWidth < surfaceHeight;
                float x, y, w, h;
                for (int i = 0; i < len; i++) {
                    x = portraitOrientation ? MARGIN * (i + 1) + thumbSize * i
                        : (float) surfaceWidth - (thumbSize + MARGIN);
                    y = portraitOrientation ? MARGIN : (float) surfaceHeight - (MARGIN * (i + 1) + thumbSize * (i + 1));
                    w = h = thumbSize;
                    // pass thumb to parent class so we can detect thumb click
                    thumbTouchHelper.registerGraphThumb(new Rect(x, y, thumbSize, thumbSize));
                    glBarGraphThumb.draw(gl, x, y, w, h, autocorrelationAnalysis[i], GlUtils.SPIKE_TRAIN_COLORS[i],
                        SPIKE_TRAIN_THUMB_GRAPH_NAMES[i]);
                }
                x = MARGIN;
                y = portraitOrientation ? 2 * MARGIN + thumbSize : MARGIN;
                w = portraitOrientation ? surfaceWidth - 2 * MARGIN : surfaceWidth - 3 * MARGIN - thumbSize;
                h = portraitOrientation ? surfaceHeight - 3 * MARGIN - thumbSize : surfaceHeight - 2 * MARGIN;

                int selected = thumbTouchHelper.getSelectedGraphThumb();
                glBarGraph.draw(gl, x, y, w, h, autocorrelationAnalysis[selected], H_GRAPH_AXIS_VALUES,
                    GlUtils.SPIKE_TRAIN_COLORS[selected], SPIKE_TRAIN_THUMB_GRAPH_NAMES[selected]);
            }
        }
    }

    //=================================================
    //  PRIVATE METHODS
    //=================================================

    private boolean getAutocorrelationAnalysis() {
        if (autocorrelationAnalysis != null && autocorrelationAnalysis.length > 0) return true;

        if (getAnalysisManager() != null) {
            autocorrelationAnalysis = getAnalysisManager().getAutocorrelation();
            if (autocorrelationAnalysis != null) {
                LOGD(TAG, "AUTOCORRELATION ANALYSIS RETURNED: " + autocorrelationAnalysis.length);
            }
            return autocorrelationAnalysis != null;
        }

        return false;
    }
}
