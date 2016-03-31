
package com.backyardbrains.drawing;


import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.backyardbrains.BYBUtils;
import com.backyardbrains.BackyardBrainsApplication;
import com.backyardbrains.analysis.BYBAnalysisManager;
import com.backyardbrains.analysis.BYBSpike;
import com.backyardbrains.view.ofRectangle;

import android.content.Context;
import android.content.Intent;
import android.opengl.GLSurfaceView;

import android.util.Log;
import android.view.MotionEvent;

public class BYBAnalysisBaseRenderer implements GLSurfaceView.Renderer {
	private static final String	TAG	= BYBAnalysisBaseRenderer.class.getCanonicalName();

	protected int				height;
	protected int				width;

	protected Context			context;
	protected ofRectangle mainRect;
	protected ofRectangle[] thumbRects;
	protected int					selected	= 0;
	
	protected int touchDownRect = -1;
	

	// ----------------------------------------------------------------------------------------
	public BYBAnalysisBaseRenderer(Context context) {
		this.context = context.getApplicationContext();
	}

	// ----------------------------------------------------------------------------------------
	public void close() {
	}
	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- TOUCH
	// -----------------------------------------------------------------------------------------------------------------------------
	private int checkInsideAllThumbRects(float x, float y) {
		if (thumbRects != null) {
			for (int i = 0; i < thumbRects.length; i++) {
				if (thumbRects[i] != null) {
					if (thumbRects[i].inside(x, y)) {
						return i;
					}
				}
			}
		}
		return -1;
	}
	// ----------------------------------------------------------------------------------------
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getActionIndex() == 0) {
			int insideRect = checkInsideAllThumbRects(event.getX(), event.getY());
			switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				touchDownRect = insideRect;
				break;
			case MotionEvent.ACTION_MOVE:
				if(insideRect == -1 || insideRect != touchDownRect){
					touchDownRect = -1;
				}
				break;
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_OUTSIDE:
				touchDownRect = -1;
				break;
			case MotionEvent.ACTION_UP:
				if(insideRect == touchDownRect){
					//valid clic!!!
					thumbRectClicked(insideRect);
				}
				break;
			}
		}
		return false;
	}
	// ----------------------------------------------------------------------------------------
	protected void thumbRectClicked(int i){
		setSelected(i);
	}
	// ----------------------------------------------------------------------------------------
	@Override
	public void onDrawFrame(GL10 gl) {
		preDrawingHandler();
		BYBUtils.glClear(gl);
		drawingHandler(gl);
		postDrawingHandler(gl);
	}

	// ----------------------------------------------------------------------------------------
	public BYBAnalysisManager getManager() {
		if (context != null) {
			return ((BackyardBrainsApplication) context.getApplicationContext()).getAnalysisManager();
		}
		return null;
	}

	// ----------------------------------------------------------------------------------------
	protected void preDrawingHandler() {
	}

	// ----------------------------------------------------------------------------------------
	protected void postDrawingHandler(GL10 gl) {

	}

	// ----------------------------------------------------------------------------------------
	protected void drawingHandler(GL10 gl) {
	}

	// ----------------------------------------------------------------------------------------
	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		Log.d(TAG, "onSurfaceChanged " + width + ", " + height);
		this.width = width;
		this.height = height;
	}

	// ----------------------------------------------------------------------------------------
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		gl.glDisable(GL10.GL_DITHER);
		gl.glHint(GL10.GL_LINE_SMOOTH_HINT, GL10.GL_NICEST);
		gl.glEnable(GL10.GL_DEPTH_TEST);
	}

	// ----------------------------------------------------------------------------------------
	protected void initGL(GL10 gl) {/// , float xBegin, float xEnd, float
									/// scaledYBegin, float scaledYEnd) {
		// set viewport
		gl.glViewport(0, 0, width, height);

		BYBUtils.glClear(gl);
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrthof(0f, width, height, 0f, -1f, 1f);
		gl.glRotatef(0f, 0f, 0f, 1f);

		// Blackout, then we're ready to draw! \o/
		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glClearColor(0f, 0f, 0f, 1.0f);
		gl.glClearDepthf(1.0f);
		gl.glEnable(GL10.GL_DEPTH_TEST);
		gl.glDepthFunc(GL10.GL_LEQUAL);
		gl.glEnable(GL10.GL_LINE_SMOOTH);
		gl.glHint(GL10.GL_LINE_SMOOTH_HINT, GL10.GL_NICEST);
		// Enable Blending
		gl.glEnable(GL10.GL_BLEND);
		// Specifies pixel arithmetic
		// gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
	}
	// ----------------------------------------------------------------------------------------
	protected void renderSpikeTrain(GL10 gl){
	if(getManager() != null){
		ArrayList<ArrayList<BYBSpike>> spikes = getManager().getSpikesTrains();
		float mn = getManager().getMinSpikeValue();
		float mx = getManager().getMaxSpikeValue();
		int tot = getManager().getTotalNumSamples();
		BYBMesh mesh = new BYBMesh(BYBMesh.LINES);			
		if(spikes != null){
			for(int i = 0; i < spikes.size(); i++){
				float [] color = BYBColors.getColorAsGlById(i);
				for(int j = 0; j < spikes.get(i).size(); j++){
					float x = ((float)spikes.get(i).get(j).index / (float)tot)*width;
					float y = height - (Math.abs(spikes.get(i).get(j).value) /mx)*height;
					mesh.addLine(x, y, x, height, color);
				//	Log.d(TAG, "addLine: " + x + " " + y);
				}
			}
		}else{
			Log.d(TAG, "spikes == null");
		}
		mesh.draw(gl);
	}
	}

	// ----------------------------------------------------------------------------------------
	protected void makeThumbAndMainRectangles() {
		int margin = 20;
		int maxSpikeTrains = 3;
		float d = (Math.min(width, height) / (float) (maxSpikeTrains + 1)) * 0.2f;
		if (d < margin) {
			margin = (int) d;
		}
		float s = (Math.min(width, height) - margin * (maxSpikeTrains + 1)) / (float) maxSpikeTrains;

		boolean bVert = width > height;

		thumbRects = new ofRectangle[maxSpikeTrains];
		for (int i = 0; i < maxSpikeTrains; i++) {

			float x = (float) margin;
			float y = (float) margin;

			if (bVert) {
				y += (s + margin) * i;
			} else {
				y = height - s - margin;
				x += (s + margin) * i;
			}

			thumbRects[i] = new ofRectangle(x, y, s, s);

		}

		float x = (float) margin;
		float y = (float) margin;
		float w = width;
		float h = height;

		if (bVert) {
			x += margin + s;
			h -= margin * 2;
			w = width - (margin * 3) - s;

		} else {
			w -= x - margin;
			h -= (margin * 3) - s;

		}
		mainRect = new ofRectangle(x, y, w, h);

	}
	

	// ----------------------------------------------------------------------------------------
	protected void graphIntegerList(GL10 gl, ArrayList<Integer> ac, ofRectangle r, float[] color, boolean bDrawBox) {
		graphIntegerList(gl, ac, r.x, r.y, r.width, r.height, color, bDrawBox);
	}

	// ----------------------------------------------------------------------------------------
	protected void graphIntegerList(GL10 gl, ArrayList<Integer> ac, float px, float py, float w, float h, float[] color, boolean bDrawBox) {
		if (ac != null) {
			if (ac.size() > 0) {
				int s = ac.size();
				float[] values = new float[s];
				int mx = Integer.MIN_VALUE;
				for (int i = 0; i < s; i++) {
					int y = ac.get(i);
					if (mx < y) mx = y;
				}
				if (mx == 0) mx = 1;// avoid division by zero
				for (int i = 0; i < s; i++) {
					values[i] = ((float) ac.get(i)) / (float) mx;
				}

				BYBBarGraph graph = new BYBBarGraph(values, px, py, w, h, color);// BYBColors.getColorAsGlById(BYBColors.yellow));
				if (bDrawBox) {
					graph.makeBox(BYBColors.getColorAsGlById(BYBColors.white));
				}
				graph.draw(gl);
			}
		}
	}
	public void setSelected(int s) {
		selected = s;
		Intent i = new Intent();
		i.setAction("BYBRenderAnalysis");
		i.putExtra("requestRender", true);
		context.sendBroadcast(i);
	}
}
