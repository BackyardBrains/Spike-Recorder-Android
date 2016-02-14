package com.backyardbrains.drawing;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;

public class WaitRenderer extends  BYBAnalysisBaseRenderer {

	private static final String TAG = WaitRenderer.class.getCanonicalName();
//	FloatBuffer colorBuffer;
//	FloatBuffer vertsBuffer;
//	ShortBuffer indicesBuffer;
	//ByteBuffer indicesBuffer;
	//----------------------------------------------------------------------------------------
	public WaitRenderer(Context context){

		super(context);
//		int circleRes = 120;
//		float [] verts;
//		float outerRadius = 100;
//		float innerRadius = outerRadius*0.7f;
//		float [] colors;
//		short [] indices;
////*
//		verts = new float[circleRes*2*2*4];
//		colors = new float[verts.length*2];
//		double inc = Math.PI*2/circleRes;
//		for(int i =0; i < verts.length; i+=8){
//			float x = (float)Math.cos(i*inc);
//			float y = (float)Math.sin(i*inc);
//			verts[i] = x * (innerRadius -1);
//			verts[i+1] = y * (innerRadius -1);
//			verts[i+2] = x * innerRadius;
//			verts[i+3] = y * innerRadius;		
//			verts[i+4] = x * outerRadius;
//			verts[i+5] = y * outerRadius;
//			verts[i+6] = x * (outerRadius+1);
//			verts[i+7] = y * (outerRadius+1);
//		}
//		float colInc = 1.0f/circleRes;
//		int colInd = 0;
//		for(int i =0; i < colors.length; i+=16){
//			float c = colInd*colInc;
//			for(int j = 0;j < 16; j++ ){
//				colors[i+j] = c;
//			}
//			colors[i+3]=0;
//			colors[i+15]=0;
//			colInd++;
//		}
//		//indices = new short [(circleRes + 1)*18];
//		float [] newVerts = new float[(circleRes + 1)*18*2];
//		float [] newCols = new float[(circleRes + 1)*18*2*2];
//		
//		for(int i =0; i < circleRes*2; i++){
//			for(int j =0; j < 3; j++){
//				newVerts[i*18 + j*6 + 0] = verts[(i*4 +j + 0)];
//				newVerts[i*18 + j*6 + 1] = verts[(i*4 +j + 1)];
//				newVerts[i*18 + j*6 + 2] = verts[(i*4 +j + 5)];
//				newVerts[i*18 + j*6 + 3] = verts[(i*4 +j + 0)];
//				newVerts[i*18 + j*6 + 4] = verts[(i*4 +j + 5)];
//				newVerts[i*18 + j*6 + 5] = verts[(i*4 +j + 4)];
//			}			
//		}
		//*/
		/*
		
		verts = new float [8];
		colors = new float [16];
		indices = new short [6];
		
		verts[0] = 100;
		verts[1] = 100;
		verts[2] = 300;
		verts[3] = 100;
		verts[4] = 100;
		verts[5] = 300;
		verts[6] = 300;
		verts[7] = 300;
		
		colors[0] = 1.0f;
		colors[1] = 0.0f;
		colors[2] = 0.0f;
		colors[3] = 1.0f;

		colors[4] = 1.0f;
		colors[5] = 1.0f;
		colors[6] = 0.0f;
		colors[7] = 1.0f;

		colors[8] = 1.0f;
		colors[8] = 0.0f;
		colors[10] = 1.0f;
		colors[11] = 1.0f;

		colors[12] = 0.0f;
		colors[13] = 0.0f;
		colors[14] = 1.0f;
		colors[15] = 1.0f;
//*/
		/*
		final ByteBuffer temp = ByteBuffer.allocateDirect(indices.length * 2);
		temp.order(ByteOrder.nativeOrder());
		indicesBuffer = temp.asShortBuffer();
		indicesBuffer.put(indices);
		indicesBuffer.position(0);

		//*/
		
//		vertsBuffer = getFloatBufferFromFloatArray(newVerts);
//		colorBuffer = getFloatBufferFromFloatArray(colors);
		
	}
	//----------------------------------------------------------------------------------------
	//----------------------------------------------------------------------------------------
	@Override
	protected void postDrawingHandler(GL10 gl) {}
	//----------------------------------------------------------------------------------------
	@Override
	protected void drawingHandler(GL10 gl) {
		initGL(gl);
		
//		gl.glMatrixMode(GL10.GL_MODELVIEW);
//		gl.glLoadIdentity();
//
//		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
//		gl.glLineWidth(1f);
//		gl.glTranslatef(width*0.5f, height*0.5f, 0);
//	//	gl.glColor4f(0f, 1f, 0f, 1f);
//		//gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
//		//gl.glColorPointer(4, GL10.GL_FLOAT, 0, colorBuffer);
//		gl.glVertexPointer(2, GL10.GL_FLOAT, 0, vertsBuffer);
//		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, vertsBuffer.limit() / 2);
//		//gl.glDrawElements(GL10.GL_TRIANGLES, indicesBuffer.limit(), GL10.GL_BYTE, indicesBuffer);
//		//gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
//		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
	}
}