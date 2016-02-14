package com.backyardbrains.drawing;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;

public class CrossCorrelationRenderer  extends  BYBAnalysisBaseRenderer {

	private static final String TAG = CrossCorrelationRenderer.class.getCanonicalName();

	//----------------------------------------------------------------------------------------
	public CrossCorrelationRenderer(Context context){
		super(context);
	}
	//----------------------------------------------------------------------------------------
	//----------------------------------------------------------------------------------------
	@Override
	protected void postDrawingHandler(GL10 gl) {}
	//----------------------------------------------------------------------------------------
	@Override
	protected void drawingHandler(GL10 gl) {
	}
}
