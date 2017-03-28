package com.backyardbrains.drawing;

import android.support.annotation.NonNull;
import com.backyardbrains.BaseFragment;
import com.backyardbrains.analysis.ISIResult;
import com.backyardbrains.view.ofRectangle;
import java.util.ArrayList;
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
            ArrayList<ArrayList<ISIResult>> ISI = getAnalysisManager().getISI();
            if (ISI != null) {
                for (int i = 0; i < ISI.size(); i++) {
                    drawISI(gl, ISI.get(i), thumbRects[i], BYBColors.getColorAsGlById(i), true);
                }
                int s = selected;
                if (selected >= ISI.size() || selected < 0) {
                    s = 0;
                }
                drawISI(gl, ISI.get(s), mainRect, BYBColors.getColorAsGlById(s), true);
            }
        }
    }

    // ----------------------------------------------------------------------------------------
    private void drawISI(GL10 gl, ArrayList<ISIResult> ISI, ofRectangle r, float[] color, boolean bDrawBox) {
        drawISI(gl, ISI, r.x, r.y, r.width, r.height, color, bDrawBox);
    }

    // ----------------------------------------------------------------------------------------
    private void drawISI(GL10 gl, ArrayList<ISIResult> ISI, float px, float py, float w, float h, float[] color,
        boolean bDrawBox) {
        if (ISI != null) {
            if (ISI.size() > 0) {
                int s = ISI.size();
                float[] values = new float[s];
                int mx = Integer.MIN_VALUE;
                for (int i = 0; i < s; i++) {
                    int y = ISI.get(i).y;
                    if (mx < y) mx = y;
                }
                if (mx == 0) mx = 1;// avoid division by zero
                for (int i = 0; i < s; i++) {
                    values[i] = ((float) ISI.get(i).y) / (float) mx;
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
