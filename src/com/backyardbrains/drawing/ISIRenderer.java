package com.backyardbrains.drawing;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;

public class ISIRenderer  extends  BYBAnalysisBaseRenderer {

	private static final String TAG = ISIRenderer.class.getCanonicalName();

	//----------------------------------------------------------------------------------------
	public ISIRenderer(Context context){
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
