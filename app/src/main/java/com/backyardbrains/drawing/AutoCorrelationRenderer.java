package com.backyardbrains.drawing;

import android.support.annotation.NonNull;
import com.backyardbrains.BaseFragment;
import com.backyardbrains.utils.BYBGlUtils;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class AutoCorrelationRenderer extends BYBAnalysisBaseRenderer {

    static final String TAG = makeLogTag(AutoCorrelationRenderer.class);

    @SuppressWarnings("WeakerAccess") int[][] autocorrelationAnalysis;

    public AutoCorrelationRenderer(@NonNull BaseFragment fragment) {
        super(fragment);
    }

    @Override protected void draw(GL10 gl, int surfaceWidth, int surfaceHeight) {
        // draw thumb rectangles and main rectangle
        makeThumbsAndMainRectangle();

        if (getAutocorrelationAnalysis()) {
            int len = autocorrelationAnalysis.length;
            if (len > 0) {
                for (int i = 0; i < len; i++) {
                    graphIntegerList(gl, autocorrelationAnalysis[i], thumbRects[i], BYBGlUtils.SPIKE_TRAIN_COLORS[i],
                        true);
                }
                int s = selected;
                if (selected >= len || selected < 0) s = 0;

                graphIntegerList(gl, autocorrelationAnalysis[s], mainRect, BYBGlUtils.SPIKE_TRAIN_COLORS[s], true);
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
