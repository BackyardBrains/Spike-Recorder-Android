package com.backyardbrains.drawing;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.BaseFragment;
import com.backyardbrains.data.InterSpikeInterval;
import com.backyardbrains.drawing.GlGraphThumbTouchHelper.Rect;
import com.backyardbrains.utils.AnalysisUtils;
import com.backyardbrains.utils.BYBGlUtils;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class ISIRenderer extends BYBAnalysisBaseRenderer {

    private static final String TAG = makeLogTag(ISIRenderer.class);

    private static final String[] SPIKE_TRAIN_THUMB_GRAPH_NAMES = new String[AnalysisUtils.MAX_SPIKE_TRAIN_COUNT];

    static {
        SPIKE_TRAIN_THUMB_GRAPH_NAMES[0] = "ST1";
        SPIKE_TRAIN_THUMB_GRAPH_NAMES[1] = "ST2";
        SPIKE_TRAIN_THUMB_GRAPH_NAMES[2] = "ST3";
    }

    private Context context;
    private GlBarGraph glBarGraph;
    private GlBarGraphThumb glBarGraphThumb;

    @SuppressWarnings("WeakerAccess") InterSpikeInterval[][] isiAnalysis;

    public ISIRenderer(@NonNull BaseFragment fragment) {
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
        if (getInterSpikeIntervalAnalysis()) {
            int len = isiAnalysis.length;
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
                    glBarGraphThumb.draw(gl, x, y, w, h, normalize(isiAnalysis[i]), BYBGlUtils.SPIKE_TRAIN_COLORS[i],
                        SPIKE_TRAIN_THUMB_GRAPH_NAMES[i]);
                }
                x = MARGIN;
                y = portraitOrientation ? 2 * MARGIN + thumbSize : MARGIN;
                w = portraitOrientation ? surfaceWidth - 2 * MARGIN : surfaceWidth - 3 * MARGIN - thumbSize;
                h = portraitOrientation ? surfaceHeight - 3 * MARGIN - thumbSize : surfaceHeight - 2 * MARGIN;

                int selected = thumbTouchHelper.getSelectedGraphThumb();
                glBarGraph.draw(gl, x, y, w, h, normalize(isiAnalysis[selected]),
                    BYBGlUtils.SPIKE_TRAIN_COLORS[selected], SPIKE_TRAIN_THUMB_GRAPH_NAMES[selected]);
            }
        }
    }

    //=================================================
    //  PRIVATE METHODS
    //=================================================

    private boolean getInterSpikeIntervalAnalysis() {
        if (isiAnalysis != null && isiAnalysis.length > 0) return true;

        if (getAnalysisManager() != null) {
            isiAnalysis = getAnalysisManager().getISI();
            if (isiAnalysis != null) {
                LOGD(TAG, "ISI ANALYSIS RETURNED: " + isiAnalysis.length);
            }
            return isiAnalysis != null;
        }

        return false;
    }

    private float[] normalize(@Nullable InterSpikeInterval[] isi) {
        if (isi != null) {
            if (isi.length > 0) {
                int len = isi.length;
                float[] values = new float[len];
                int max = Integer.MIN_VALUE;
                for (InterSpikeInterval anIsi : isi) {
                    int y = anIsi.getY();
                    if (max < y) max = y;
                }
                if (max == 0) max = 1; // avoid division by zero
                for (int i = 0; i < len; i++) {
                    values[i] = ((float) isi[i].getY()) / (float) max;
                }

                return values;
            }
        }

        return new float[0];
    }
}
