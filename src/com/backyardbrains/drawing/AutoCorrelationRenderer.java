package com.backyardbrains.drawing;

import java.util.ArrayList;
import javax.microedition.khronos.opengles.GL10;


import android.content.Context;

public class AutoCorrelationRenderer extends BYBAnalysisBaseRenderer {

	private static final String	TAG	 = "AutoCorrelationRenderer";
	

	// ----------------------------------------------------------------------------------------
	public AutoCorrelationRenderer(Context context) {
		super(context);
	}

	// ----------------------------------------------------------------------------------------
	@Override
	protected void postDrawingHandler(GL10 gl) {
	}
	// ----------------------------------------------------------------------------------------
	@Override
	protected void drawingHandler(GL10 gl) {
		initGL(gl);

		makeThumbAndMainRectangles();
		if (getManager() != null) {
			ArrayList<ArrayList<Integer>> AC = getManager().getAutoCorrelation();
			if (AC != null) {
				for (int i = 0; i < AC.size(); i++) {
					graphIntegerList(gl, AC.get(i), thumbRects[i], BYBColors.getColorAsGlById(i), true);
				}
				int s = selected;
				if (selected >= AC.size() || selected <  0) {
					s = 0;
				}
				graphIntegerList(gl, AC.get(s), mainRect, BYBColors.getColorAsGlById(s), true);
			}
		}
	}

}
