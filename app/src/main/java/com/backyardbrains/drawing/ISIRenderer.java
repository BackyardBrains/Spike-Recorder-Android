package com.backyardbrains.drawing;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.BaseFragment;
import com.backyardbrains.data.InterSpikeInterval;
import com.backyardbrains.utils.BYBGlUtils;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class ISIRenderer extends BYBAnalysisBaseRenderer {

    private static final String TAG = makeLogTag(ISIRenderer.class);

    @SuppressWarnings("WeakerAccess") InterSpikeInterval[][] isiAnalysis;

    public ISIRenderer(@NonNull BaseFragment fragment) {
        super(fragment);
    }

    @Override protected void draw(GL10 gl, int surfaceWidth, int surfaceHeight) {
        // draw thumb rectangles and main rectangle
        makeThumbRectangles(surfaceWidth, surfaceHeight);

        if (getInterSpikeIntervalAnalysis()) {
            int len = isiAnalysis.length;
            if (len > 0) {
                Rect thumb;
                for (int i = 0; i < len; i++) {
                    thumb = getThumb(i);
                    if (thumb != null) drawISI(gl, isiAnalysis[i], thumb, BYBGlUtils.SPIKE_TRAIN_COLORS[i]);
                }
                int s = selected;
                if (selected >= len || selected < 0) s = 0;

                drawISI(gl, isiAnalysis[s], graph, BYBGlUtils.SPIKE_TRAIN_COLORS[s]);
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

    private void drawISI(GL10 gl, InterSpikeInterval[] isi, Rect r, float[] color) {
        drawISI(gl, isi, r.x, r.y, r.width, r.height, color);
    }

    private void drawISI(@NonNull GL10 gl, @Nullable InterSpikeInterval[] isi, float px, float py, float w, float h,
        float[] color) {
        if (isi != null) {
            int len = isi.length;
            if (len > 0) {
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

                final BYBBarGraph graph = new BYBBarGraph(values, px, py, w, h, color);
                graph.makeBox(BYBColors.getColorAsGlById(BYBColors.white));
                graph.draw(gl);
            }
        }
    }
}
