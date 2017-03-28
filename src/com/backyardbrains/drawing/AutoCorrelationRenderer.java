package com.backyardbrains.drawing;

import android.support.annotation.NonNull;
import com.backyardbrains.BaseFragment;
import java.util.ArrayList;
import javax.microedition.khronos.opengles.GL10;

public class AutoCorrelationRenderer extends BYBAnalysisBaseRenderer {

    // ----------------------------------------------------------------------------------------
    public AutoCorrelationRenderer(@NonNull BaseFragment fragment) {
        super(fragment);
    }

    // ----------------------------------------------------------------------------------------
    @Override protected void drawingHandler(GL10 gl) {
        initGL(gl);
        makeThumbAndMainRectangles();
        if (getAnalysisManager() != null) {
            ArrayList<ArrayList<Integer>> AC = getAnalysisManager().getAutoCorrelation();
            if (AC != null) {
                for (int i = 0; i < AC.size(); i++) {
                    graphIntegerList(gl, AC.get(i), thumbRects[i], BYBColors.getColorAsGlById(i), true);
                }
                if (AC.size() > 0) {
                    int s = selected;
                    if (selected >= AC.size() || selected < 0) {
                        s = 0;
                    }
                    graphIntegerList(gl, AC.get(s), mainRect, BYBColors.getColorAsGlById(s), true);
                }
            }
        }
    }
}
