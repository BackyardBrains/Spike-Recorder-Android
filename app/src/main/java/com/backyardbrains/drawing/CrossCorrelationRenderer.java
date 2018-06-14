package com.backyardbrains.drawing;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.MotionEvent;
import com.android.texample.GLText;
import com.backyardbrains.BaseFragment;
import com.backyardbrains.drawing.GlGraphThumbTouchHelper.Rect;
import com.backyardbrains.events.RedrawAudioAnalysisEvent;
import com.backyardbrains.utils.AnalysisUtils;
import com.backyardbrains.utils.GlUtils;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import org.greenrobot.eventbus.EventBus;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class CrossCorrelationRenderer extends BYBAnalysisBaseRenderer {

    private static final String TAG = makeLogTag(CrossCorrelationRenderer.class);

    private static final float[] H_GRAPH_AXIS_VALUES = new float[5];

    static {
        H_GRAPH_AXIS_VALUES[0] = -.1f;
        H_GRAPH_AXIS_VALUES[1] = -.05f;
        H_GRAPH_AXIS_VALUES[2] = 0f;
        H_GRAPH_AXIS_VALUES[3] = .05f;
        H_GRAPH_AXIS_VALUES[4] = .1f;
    }

    private static final String[] SPIKE_TRAIN_GRAPH_NAMES = new String[AnalysisUtils.MAX_SPIKE_TRAIN_COUNT];

    static {
        SPIKE_TRAIN_GRAPH_NAMES[0] = "Spike Train 1";
        SPIKE_TRAIN_GRAPH_NAMES[1] = "Spike Train 2";
        SPIKE_TRAIN_GRAPH_NAMES[2] = "Spike Train 3";
    }

    private static final String[] SPIKE_TRAIN_GRAPH_SHORT_NAMES = new String[AnalysisUtils.MAX_SPIKE_TRAIN_COUNT];

    static {
        SPIKE_TRAIN_GRAPH_SHORT_NAMES[0] = "ST1";
        SPIKE_TRAIN_GRAPH_SHORT_NAMES[1] = "ST2";
        SPIKE_TRAIN_GRAPH_SHORT_NAMES[2] = "ST3";
    }

    private boolean thumbsView = true;

    private Context context;
    private GlBarGraph glBarGraph;
    private GlBarGraphThumb glBarGraphThumb;
    private GLText glText;

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

        glBarGraph = new GlBarGraph(context, gl);
        glBarGraphThumb = new GlBarGraphThumb(context, gl);
        glText = new GLText(gl, context.getAssets());
        glText.load("dos-437.ttf", 32, 0, 0);
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
                        // draw graph thumbs grid
                        float d = (Math.min(surfaceWidth, surfaceHeight) / (float) trainCount) * 0.2f;
                        float margin = MARGIN;
                        // margin shouldn't be more than 20% of the graph size
                        if (d < MARGIN) margin = (int) d;

                        float textH = glText.getHeight();
                        float textOffset = textH + margin;

                        float x, y;
                        float w = (surfaceWidth - margin * (trainCount + 1) - textOffset) / (float) trainCount;
                        float h = (surfaceHeight - margin * (trainCount + 1) - textOffset) / (float) trainCount;

                        float textX, textY;
                        float textLongW = glText.getLength(SPIKE_TRAIN_GRAPH_NAMES[0]);
                        String[] spNamesH =
                            textLongW <= w * .8f ? SPIKE_TRAIN_GRAPH_NAMES : SPIKE_TRAIN_GRAPH_SHORT_NAMES;
                        String[] spNamesV =
                            textLongW <= h * .8f ? SPIKE_TRAIN_GRAPH_NAMES : SPIKE_TRAIN_GRAPH_SHORT_NAMES;

                        for (int i = 0; i < trainCount; i++) {
                            for (int j = 0; j < trainCount; j++) {
                                // draw thumb
                                x = textOffset + j * (w + margin) + margin;
                                y = surfaceHeight - (textOffset + (h + margin) * (i + 1));
                                thumbTouchHelper.registerGraphThumb(new Rect(x, y, w, h));
                                glBarGraphThumb.draw(gl, x, y, w, h, crossCorrelationAnalysis[i * trainCount + j],
                                    GlUtils.SPIKE_TRAIN_COLORS[i], "");

                                // draw spike train names
                                gl.glEnable(GL10.GL_TEXTURE_2D);
                                glText.begin(1f, 1f, 1f, 1f);
                                if (i == 0) {
                                    textX = x + w / 2f;
                                    textY = y + h + margin;
                                    glText.drawCX(spNamesH[j], textX, textY);
                                }
                                if (j == 0) {
                                    textX = margin + textH / 2f;
                                    textY = y + h / 2f;
                                    glText.drawCY(spNamesV[i], textX, textY, -90);
                                }
                                glText.end();
                                gl.glDisable(GL10.GL_TEXTURE_2D);
                            }
                        }
                    } else {
                        int selected = thumbTouchHelper.getSelectedGraphThumb();
                        glBarGraph.draw(gl, MARGIN, MARGIN, surfaceWidth - 2 * MARGIN, surfaceHeight - 2 * MARGIN,
                            crossCorrelationAnalysis[selected], H_GRAPH_AXIS_VALUES,
                            GlUtils.SPIKE_TRAIN_COLORS[selected / trainCount], "");
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
}
