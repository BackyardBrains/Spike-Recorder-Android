package com.backyardbrains.drawing;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;

public class AverageSpikeRenderer  extends  BYBAnalysisBaseRenderer {

	private static final String TAG = AverageSpikeRenderer.class.getCanonicalName();

	//----------------------------------------------------------------------------------------
	public AverageSpikeRenderer(Context context){
		super(context);
	}
	//----------------------------------------------------------------------------------------
	//----------------------------------------------------------------------------------------
	@Override
	protected void postDrawingHandler(GL10 gl) {}
	//----------------------------------------------------------------------------------------
	@Override
	protected void drawingHandler(GL10 gl) {

//		firstBufferDrawnCheck();
//		autoScaleCheck();
//
//		gl.glMatrixMode(GL10.GL_MODELVIEW);
//		gl.glLoadIdentity();
//
//		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
//		gl.glLineWidth(1f);
//		gl.glColor4f(0f, 1f, 0f, 1f);
//		gl.glVertexPointer(2, GL10.GL_FLOAT, 0, mVertexBuffer);
//		gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, mVertexBuffer.limit() / 2);
//		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
	}
}
