package com.backyardbrains.drawing;

import android.content.Context;
import android.support.annotation.NonNull;
import com.backyardbrains.BaseFragment;
import com.backyardbrains.utils.AnalysisUtils;
import com.backyardbrains.utils.BYBGlUtils;
import com.backyardbrains.utils.LogUtils;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class AutoCorrelationRenderer extends BYBAnalysisBaseRenderer {

    static final String TAG = makeLogTag(AutoCorrelationRenderer.class);

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
        if (getAutocorrelationAnalysis(surfaceWidth, surfaceHeight)) {
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
                    registerThumb(new Rect(x, y, thumbSize, thumbSize));
                    glBarGraphThumb.draw(gl, x, y, w, h, normalize(autocorrelationAnalysis[i]),
                        BYBGlUtils.SPIKE_TRAIN_COLORS[i], SPIKE_TRAIN_THUMB_GRAPH_NAMES[i]);
                }
                x = MARGIN;
                y = portraitOrientation ? 2 * MARGIN + thumbSize : MARGIN;
                w = portraitOrientation ? surfaceWidth - 2 * MARGIN : surfaceWidth - 3 * MARGIN - thumbSize;
                h = portraitOrientation ? surfaceHeight - 3 * MARGIN - thumbSize : surfaceHeight - 2 * MARGIN;

                int s = getSelectedGraph();
                if (selected >= len || selected < 0) s = 0;
                registerGraph(new Rect(x, y, w, h));
                glBarGraph.draw(gl, x, y, w, h, normalize(autocorrelationAnalysis[s]), BYBGlUtils.SPIKE_TRAIN_COLORS[s],
                    SPIKE_TRAIN_THUMB_GRAPH_NAMES[s]);
                //graphIntegerList(gl, autocorrelationAnalysis[s], rect, BYBGlUtils.SPIKE_TRAIN_COLORS[s], true);
            }
        }
    }

    private float[] normalize(int[] ac) {
        if (ac != null) {
            if (ac.length > 0) {
                int s = ac.length;
                float[] values = new float[s];
                int max = Integer.MIN_VALUE;
                for (int i = 0; i < s; i++) {
                    int y = ac[i];
                    if (max < y) max = y;
                }
                if (max == 0) max = 1;// avoid division by zero
                LogUtils.LOGD("GlBarGraphThumb", "VALUE: " + max);
                for (int i = 0; i < s; i++) {
                    values[i] = ((float) ac[i]) / (float) max;
                }

                return values;
            }
        }

        return new float[0];
    }

    //=================================================
    //  PRIVATE METHODS
    //=================================================

    private boolean getAutocorrelationAnalysis(int surfaceWidth, int surfaceHeight) {
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
