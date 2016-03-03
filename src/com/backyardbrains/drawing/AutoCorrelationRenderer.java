package com.backyardbrains.drawing;

import java.util.ArrayList;
import javax.microedition.khronos.opengles.GL10;
import com.backyardbrains.view.ofRectangle;

import android.content.Context;

public class AutoCorrelationRenderer extends BYBAnalysisBaseRenderer {

	private static final String	TAG			= "AutoCorrelationRenderer";
	

	// ----------------------------------------------------------------------------------------
	public AutoCorrelationRenderer(Context context) {
		super(context);
	}

	// ----------------------------------------------------------------------------------------
	@Override
	protected void postDrawingHandler(GL10 gl) {
	}

	
// //
// ----------------------------------------------------------------------------------------
// protected void drawAC(GL10 gl, ArrayList<Integer> ac, ofRectangle r, float[]
// color, boolean bDrawBox) {
// drawAC(gl, ac, r.x, r.y, r.width, r.height, color, bDrawBox);
// }
//
// //
// ----------------------------------------------------------------------------------------
// protected void drawAC(GL10 gl, ArrayList<Integer> ac, float px, float py,
// float w, float h, float[] color, boolean bDrawBox) {
// if (ac != null) {
// if (ac.size() > 0) {
// int s = ac.size();
// float[] values = new float[s];
// int mx = Integer.MIN_VALUE;
// for (int i = 0; i < s; i++) {
// int y = ac.get(i);
// if (mx < y) mx = y;
// }
// if (mx == 0) mx = 1;// avoid division by zero
// for (int i = 0; i < s; i++) {
// values[i] = ((float) ac.get(i)) / (float) mx;
// }
//
// BYBBarGraph graph = new BYBBarGraph(values, px, py, w, h, color);//
// BYBColors.getColorAsGlById(BYBColors.yellow));
// if (bDrawBox) {
// graph.makeBox(BYBColors.getColorAsGlById(BYBColors.white));
// }
// graph.draw(gl);
// }
// }
// }

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
