package com.backyardbrains.drawing;

import android.support.annotation.NonNull;
import com.backyardbrains.BaseFragment;
import com.backyardbrains.utils.BYBGlUtils;
import com.backyardbrains.view.ofRectangle;
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
        int maxSpikeTrains = 3;
        if (getCrossCorrelationAnalysis()) {
            if (crossCorrelationAnalysis != null) {
                final int len = crossCorrelationAnalysis.length;
                final int divider = (int) Math.sqrt(len);
                if (thumbsView) {
                    float d = (Math.min(surfaceWidth, surfaceHeight) / (float) (maxSpikeTrains + 1)) * 0.2f;
                    if (d < margin) {
                        margin = (int) d;
                    }
                    float w = (surfaceWidth - margin * (maxSpikeTrains + 1)) / (float) maxSpikeTrains;
                    float h = (surfaceHeight - (margin * 1.5f) * (maxSpikeTrains + 1)) / (float) maxSpikeTrains;

                    thumbRects = new ofRectangle[maxSpikeTrains * maxSpikeTrains];

                    for (int i = 0; i < maxSpikeTrains; i++) {
                        for (int j = 0; j < maxSpikeTrains; j++) {
                            thumbRects[i * maxSpikeTrains + j] =
                                new ofRectangle(j * (w + margin) + margin, (h + (margin * 1.5f)) * i + (margin * 1.5f),
                                    w, h);
                        }
                    }

                    for (int i = 0; i < len; i++) {
                        graphIntegerList(gl, crossCorrelationAnalysis[i], thumbRects[i],
                            BYBGlUtils.SPIKE_TRAIN_COLORS[i / divider], true);
                    }
                } else {
                    int s = selected;
                    if (selected < 0 || selected >= maxSpikeTrains * maxSpikeTrains || selected >= len) {
                        s = 0;
                    }
                    mainRect = new ofRectangle(margin, margin, surfaceWidth - 2 * margin, surfaceHeight - 2 * margin);
                    graphIntegerList(gl, crossCorrelationAnalysis[s], mainRect,
                        BYBGlUtils.SPIKE_TRAIN_COLORS[s / divider], true);
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
