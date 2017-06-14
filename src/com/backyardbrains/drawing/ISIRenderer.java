package com.backyardbrains.drawing;

import android.support.annotation.NonNull;
import com.backyardbrains.BaseFragment;
import com.backyardbrains.analysis.BYBInterSpikeInterval;
import com.backyardbrains.view.ofRectangle;
import java.util.List;
import javax.microedition.khronos.opengles.GL10;

public class ISIRenderer extends BYBAnalysisBaseRenderer {

    // ----------------------------------------------------------------------------------------
    public ISIRenderer(@NonNull BaseFragment fragment) {
        super(fragment);
    }

    // ----------------------------------------------------------------------------------------
    @Override protected void drawingHandler(GL10 gl) {
        initGL(gl);

        makeThumbAndMainRectangles();
        if (getAnalysisManager() != null) {
            List<List<BYBInterSpikeInterval>> isi = getAnalysisManager().getISI();
            if (isi != null) {
                for (int i = 0; i < isi.size(); i++) {
                    drawISI(gl, isi.get(i), thumbRects[i], BYBColors.getColorAsGlById(i), true);
                }
                int s = selected;
                if (selected >= isi.size() || selected < 0) {
                    s = 0;
                }
                drawISI(gl, isi.get(s), mainRect, BYBColors.getColorAsGlById(s), true);
            }
        }
    }

    // ----------------------------------------------------------------------------------------
    private void drawISI(GL10 gl, List<BYBInterSpikeInterval> ISI, ofRectangle r, float[] color, boolean bDrawBox) {
        drawISI(gl, ISI, r.x, r.y, r.width, r.height, color, bDrawBox);
    }

    // ----------------------------------------------------------------------------------------
    private void drawISI(GL10 gl, List<BYBInterSpikeInterval> ISI, float px, float py, float w, float h, float[] color,
        boolean bDrawBox) {
        if (ISI != null) {
            if (ISI.size() > 0) {
                int s = ISI.size();
                float[] values = new float[s];
                int mx = Integer.MIN_VALUE;
                for (int i = 0; i < s; i++) {
                    int y = ISI.get(i).getY();
                    if (mx < y) mx = y;
                }
                if (mx == 0) mx = 1;// avoid division by zero
                for (int i = 0; i < s; i++) {
                    values[i] = ((float) ISI.get(i).getY()) / (float) mx;
                }

                BYBBarGraph graph =
                    new BYBBarGraph(values, px, py, w, h, color);// BYBColors.getColorAsGlById(BYBColors.yellow));
                if (bDrawBox) {
                    graph.makeBox(BYBColors.getColorAsGlById(BYBColors.white));
                }
                graph.draw(gl);
            }
        }
    }
}
