package com.backyardbrains.drawing;

import android.support.annotation.NonNull;
import com.backyardbrains.BaseFragment;
import com.backyardbrains.utils.BYBGlUtils;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class CrossCorrelationRenderer extends BYBAnalysisBaseRenderer {

    private static final String TAG = makeLogTag(CrossCorrelationRenderer.class);

    private boolean thumbsView = true;

    @SuppressWarnings("WeakerAccess") int[][] crossCorrelationAnalysis;

    public CrossCorrelationRenderer(@NonNull BaseFragment fragment) {
        super(fragment);
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

    @Override protected void thumbRectClicked(int i) {
        if (thumbsView) {
            thumbsView = false;
            super.thumbRectClicked(i);
        }
    }

    @Override protected void draw(GL10 gl, int surfaceWidth, int surfaceHeight) {
        int margin = 20;
        if (getCrossCorrelationAnalysis()) {
            if (crossCorrelationAnalysis != null) {
                final int len = crossCorrelationAnalysis.length;
                final int divider = (int) Math.sqrt(len);
                if (thumbsView) {
                    float d = (Math.min(surfaceWidth, surfaceHeight) / (float) (divider + 1)) * 0.2f;
                    if (d < margin) {
                        margin = (int) d;
                    }
                    float w = (surfaceWidth - margin * (divider + 1)) / (float) divider;
                    float h = (surfaceHeight - (margin * 1.5f) * (divider + 1)) / (float) divider;

                    for (int i = 0; i < divider; i++) {
                        for (int j = 0; j < divider; j++) {
                            registerThumb(
                                new Rect(j * (w + margin) + margin, (h + (margin * 1.5f)) * i + (margin * 1.5f), w, h));
                            //graphThumbs[i * divider + j]
                        }
                    }

                    Rect thumb;
                    for (int i = 0; i < len; i++) {
                        thumb = getThumb(i);
                        if (thumb != null) {
                            graphIntegerList(gl, crossCorrelationAnalysis[i], thumb,
                                BYBGlUtils.SPIKE_TRAIN_COLORS[i / divider], true);
                        }
                    }
                } else {
                    int s = selected;
                    if (selected < 0 || selected >= divider * divider || selected >= len) {
                        s = 0;
                    }
                    graph = new Rect(margin, margin, surfaceWidth - 2 * margin, surfaceHeight - 2 * margin);
                    graphIntegerList(gl, crossCorrelationAnalysis[s], graph, BYBGlUtils.SPIKE_TRAIN_COLORS[s / divider],
                        true);
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
