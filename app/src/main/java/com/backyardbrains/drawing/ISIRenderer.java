package com.backyardbrains.drawing;

import android.content.Context;
import androidx.annotation.NonNull;
import com.backyardbrains.drawing.gl.GlColumnGraph;
import com.backyardbrains.drawing.gl.GlColumnGraphThumb;
import com.backyardbrains.drawing.gl.Rect;
import com.backyardbrains.ui.BaseFragment;
import java.text.DecimalFormat;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class ISIRenderer extends BaseAnalysisRenderer {

    private static final String TAG = makeLogTag(ISIRenderer.class);

    private static final float[] H_GRAPH_AXIS_VALUES = new float[5];

    static {
        H_GRAPH_AXIS_VALUES[0] = (float) 1E-3;
        H_GRAPH_AXIS_VALUES[1] = (float) 1E-2;
        H_GRAPH_AXIS_VALUES[2] = (float) 1E-1;
        H_GRAPH_AXIS_VALUES[3] = (float) 1E0;
        H_GRAPH_AXIS_VALUES[4] = (float) 1E1;
    }

    private Context context;
    private GlColumnGraph glColumnGraph;
    private GlColumnGraphThumb glColumnGraphThumb;

    @SuppressWarnings("WeakerAccess") int[][] isiAnalysis;

    public ISIRenderer(@NonNull BaseFragment fragment) {
        super(fragment);

        context = fragment.getContext();
    }

    /**
     * {@inheritDoc}
     */
    @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        super.onSurfaceCreated(gl, config);

        glColumnGraph = new GlColumnGraph(context, gl, new DecimalFormat("0E0"));
        glColumnGraphThumb = new GlColumnGraphThumb(context, gl);
    }

    @Override protected void draw(GL10 gl, int surfaceWidth, int surfaceHeight) {
        if (getInterSpikeIntervalAnalysis()) {
            int len = isiAnalysis.length;
            if (len > 0) {
                final float thumbSize = getDefaultGraphThumbSize(surfaceWidth, surfaceHeight, len);
                boolean portraitOrientation = surfaceWidth < surfaceHeight;
                float x, y, w, h;
                for (int i = 0; i < len; i++) {
                    x = portraitOrientation ? MARGIN * (i + 1) + thumbSize * i
                        : (float) surfaceWidth - (thumbSize + MARGIN);
                    y = portraitOrientation ? MARGIN : (float) surfaceHeight - (MARGIN * (i + 1) + thumbSize * (i + 1));
                    w = h = thumbSize;
                    // pass thumb to parent class so we can detect thumb click
                    glGraphThumbTouchHelper.registerTouchableArea(new Rect(x, y, thumbSize, thumbSize));
                    glColumnGraphThumb.draw(gl, x, y, w, h, isiAnalysis[i],
                        Colors.CHANNEL_COLORS[i % Colors.CHANNEL_COLORS.length],
                        SPIKE_TRAIN_THUMB_GRAPH_NAME_PREFIX + (i + 1));
                }
                x = MARGIN;
                y = portraitOrientation ? 2 * MARGIN + thumbSize : MARGIN;
                w = portraitOrientation ? surfaceWidth - 2 * MARGIN : surfaceWidth - 3 * MARGIN - thumbSize;
                h = portraitOrientation ? surfaceHeight - 3 * MARGIN - thumbSize : surfaceHeight - 2 * MARGIN;

                int selected = glGraphThumbTouchHelper.getSelectedTouchableArea();
                glColumnGraph.draw(gl, x, y, w, h, isiAnalysis[selected], H_GRAPH_AXIS_VALUES,
                    Colors.CHANNEL_COLORS[selected % Colors.CHANNEL_COLORS.length]);
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
}
