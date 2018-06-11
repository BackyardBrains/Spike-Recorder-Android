package com.backyardbrains.drawing;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.MotionEvent;
import com.android.texample.GLText;
import com.backyardbrains.BaseFragment;
import com.backyardbrains.drawing.GlGraphThumbTouchHelper.Rect;
import com.backyardbrains.events.RedrawAudioAnalysisEvent;
import com.backyardbrains.utils.BYBGlUtils;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import org.greenrobot.eventbus.EventBus;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class CrossCorrelationRenderer extends BYBAnalysisBaseRenderer {

    private static final String TAG = makeLogTag(CrossCorrelationRenderer.class);

    private boolean thumbsView = true;

    private Context context;
    private GLText text;
    private GlBarGraph glBarGraph;
    private GlBarGraphThumb glBarGraphThumb;

    @SuppressWarnings("WeakerAccess") int[][] crossCorrelationAnalysis;

    public CrossCorrelationRenderer(@NonNull BaseFragment fragment) {
        super(fragment);

        context = fragment.getContext();
    }

    /**
     * {@inheritDoc}
     */
    @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        super.onSurfaceCreated(gl, config);

        text = new GLText(gl, context.getAssets());
        text.load("dos-437.ttf", 24, 5, 5);
        glBarGraph = new GlBarGraph(context, gl);
        glBarGraphThumb = new GlBarGraphThumb(context, gl);
    }

    /**
     * Whether thumbs view is currently rendered or not
     */
    public boolean isThumbsView() {
        return thumbsView;
    }

    /**
     * Sets whether thumbs view should be rendered or not
     */
    public void setThumbsView() {
        if (!thumbsView) thumbsView = true;
    }

    @Override public void onTouchEvent(MotionEvent event) {
        if (thumbsView) {
            boolean graphThumbTouched = thumbTouchHelper.onTouch(event);
            if (graphThumbTouched) {
                thumbsView = false;
                EventBus.getDefault().post(new RedrawAudioAnalysisEvent());
            }
        }
    }

    @Override protected void draw(GL10 gl, int surfaceWidth, int surfaceHeight) {
        if (getCrossCorrelationAnalysis()) {
            if (crossCorrelationAnalysis != null) {
                final int len = crossCorrelationAnalysis.length;
                if (len > 0) {
                    final int trainCount = (int) Math.sqrt(len);
                    if (thumbsView) {
                        float d = (Math.min(surfaceWidth, surfaceHeight) / (float) trainCount) * 0.2f;
                        float margin = MARGIN;
                        // margin shouldn't be more than 20% of the graph size
                        if (d < MARGIN) margin = (int) d;

                        float x, y;
                        float w = (surfaceWidth - margin * (trainCount + 1)) / (float) trainCount;
                        float h = (surfaceHeight - margin * (trainCount + 1)) / (float) trainCount;
                        for (int i = 0; i < trainCount; i++) {
                            for (int j = 0; j < trainCount; j++) {
                                x = j * (w + margin) + margin;
                                y = surfaceHeight - (h + margin) * (i + 1);
                                thumbTouchHelper.registerGraphThumb(new Rect(x, y, w, h));
                                glBarGraphThumb.draw(gl, x, y, w, h,
                                    normalize(crossCorrelationAnalysis[i * trainCount + j]),
                                    BYBGlUtils.SPIKE_TRAIN_COLORS[i], "");
                            }
                        }
                    } else {
                        int selected = thumbTouchHelper.getSelectedGraphThumb();
                        glBarGraph.draw(gl, MARGIN, MARGIN, surfaceWidth - MARGIN, surfaceHeight - MARGIN,
                            normalize(crossCorrelationAnalysis[selected]),
                            BYBGlUtils.SPIKE_TRAIN_COLORS[selected / trainCount], "");
                        //graph = new Rect(margin, margin, surfaceWidth - 2 * margin, surfaceHeight - 2 * margin);
                        //graphIntegerList(gl, normalize(crossCorrelationAnalysis[selected]), graph,
                        //    BYBGlUtils.SPIKE_TRAIN_COLORS[selected / divider], true);
                    }
                }
            }
        }
    }

    //=================================================
    //  PRIVATE METHODS
    //=================================================

    private boolean getCrossCorrelationAnalysis() {
        if (crossCorrelationAnalysis != null && crossCorrelationAnalysis.length > 0) return true;

        if (getAnalysisManager() != null) {
            crossCorrelationAnalysis = getAnalysisManager().getCrossCorrelation();
            if (crossCorrelationAnalysis != null) {
                LOGD(TAG, "CROSS-CORRELATION ANALYSIS RETURNED: " + crossCorrelationAnalysis.length);
            }
            return crossCorrelationAnalysis != null;
        }

        return false;
    }

    private float[] normalize(int[] ac) {
        if (ac != null) {
            if (ac.length > 0) {
                int len = ac.length;
                float[] values = new float[len];
                int max = Integer.MIN_VALUE;
                for (int y : ac) {
                    if (max < y) max = y;
                }
                if (max == 0) max = 1;// avoid division by zero
                for (int i = 0; i < len; i++) {
                    values[i] = ((float) ac[i]) / (float) max;
                }

                return values;
            }
        }

        return new float[0];
    }
}
