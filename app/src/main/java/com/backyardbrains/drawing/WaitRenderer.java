package com.backyardbrains.drawing;
//
//import java.nio.ByteBuffer;
//import java.nio.ByteOrder;
//import java.nio.FloatBuffer;
//import java.nio.ShortBuffer;

//import javax.microedition.khronos.egl.EGLConfig;

import android.support.annotation.NonNull;
import com.backyardbrains.BaseFragment;
import javax.microedition.khronos.opengles.GL10;

//
//import com.android.texample2.GLText;
//import android.opengl.GLES20;
//import android.opengl.GLU;
//import android.opengl.Matrix;
//import android.util.Log;

public class WaitRenderer extends BYBAnalysisBaseRenderer {

    private static final String TAG = WaitRenderer.class.getCanonicalName();
    //	FloatBuffer colorBuffer;
    //	FloatBuffer vertsBuffer;
    //	ShortBuffer indicesBuffer;
    //ByteBuffer indicesBuffer;

    BYBMesh mesh;

    //
    //	private GLText glText;                             // A GLText Instance
    //
    //	private int width = 100;                           // Updated to the Current Width + Height in onSurfaceChanged()
    //	private int height = 100;
    //	private float[] mProjMatrix = new float[16];
    //	private float[] mVMatrix = new float[16];
    //	private float[] mVPMatrix = new float[16];
    //
    //----------------------------------------------------------------------------------------
    public WaitRenderer(@NonNull BaseFragment fragment) {
        super(fragment);

        mesh = new BYBMesh(BYBMesh.TRIANGLES);
        //	mesh.addRectangle(100, 100, 200, 200, BYBColors.magenta, true);

        //mesh.addRectangle(-1.0f, -1.0f, 2, 2, BYBColors.white, true);

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

    @Override protected void draw(GL10 gl, int surfaceWidth, int surfaceHeight) {
        ////Log.d(TAG, "draw");
        //	initGL(gl);
		/*
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();






		int clearMask = GLES20.GL_COLOR_BUFFER_BIT;

		GLES20.glClear(clearMask);

		Matrix.multiplyMM(mVPMatrix, 0, mProjMatrix, 0, mVMatrix, 0);

		// TEST: render the entire font texture
		glText.drawTexture( width/2, height/2, mVPMatrix);            // Draw the Entire Texture

		// TEST: render some strings with the font
		glText.begin( 1.0f, 1.0f, 1.0f, 1.0f, mVPMatrix );         // Begin Text Rendering (Set Color WHITE)
		glText.drawC("Test String 3D!", 0f, 0f, 0f, 0, -30, 0);
//		glText.drawC( "Test String :)", 0, 0, 0 );          // Draw Test String
		glText.draw( "Diagonal 1", 40, 40, 40);                // Draw Test String
		glText.draw( "Column 1", 100, 100, 90);              // Draw Test String
		glText.end();                                   // End Text Rendering

		glText.begin( 0.0f, 0.0f, 1.0f, 1.0f, mVPMatrix );         // Begin Text Rendering (Set Color BLUE)
		glText.draw( "More Lines...", 50, 200 );        // Draw Test String
		glText.draw( "The End.", 50, 200 + glText.getCharHeight(), 180);  // Draw Test String
		glText.end();
		mesh.draw(gl);
		/*

		float [] values = new float [10];
		for(int i = 0; i < values.length; i++){
			values[i]=(float)Math.random();
		}
		BYBBarGraph b = new BYBBarGraph(values, 30, 30, 300, 200, BYBColors.red);
		b.setHorizontalAxis(0, 1, 5);
		b.setVerticalAxis(0, 2, 10);
		b.draw(gl);
		//*/
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

    /*
	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		if(height == 0) { 						//Prevent A Divide By Zero By
			height = 1; 						//Making Height Equal One
		}
/*
		gl.glViewport(0, 0, width, height); 	//Reset The Current Viewport
		gl.glMatrixMode(GL10.GL_PROJECTION); 	//Select The Projection Matrix
		gl.glLoadIdentity(); 					//Reset The Projection Matrix

		//Calculate The Aspect Ratio Of The Window
		GLU.gluPerspective(gl, 45.0f, (float)width / (float)height, 0.1f, 100.0f);

		gl.glMatrixMode(GL10.GL_MODELVIEW); 	//Select The Modelview Matrix
		gl.glLoadIdentity(); 					//Reset The Modelview Matrix
		
		

		
		GLES20.glViewport(0, 0, width, height);
		float ratio = (float) width / height;

		// Take into account device orientation
		if (width > height) {
			Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 1, 10);
		}
		else {
			Matrix.frustumM(mProjMatrix, 0, -1, 1, -1/ratio, 1/ratio, 1, 10);
		}
		
		// Save width and height
		this.width = width;                             // Save Current Width
		this.height = height;                           // Save Current Height
		
		int useForOrtho = Math.min(width, height);
		
		//TODO: Is this wrong?
		Matrix.orthoM(mVMatrix, 0,
				-useForOrtho/2,
				useForOrtho/2,
				-useForOrtho/2,
				useForOrtho/2, 0.1f, 100f);
		
		
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		// Load the texture for the square
		// Set the background frame color
		GLES20.glClearColor( 0.5f, 0.5f, 0.5f, 1.0f );
		
		// Create the GLText
		glText = new GLText(context.getAssets());

		// Load the font from file (set size + padding), creates the texture
		// NOTE: after a successful call to this the font is ready for rendering!
		glText.load( "Roboto-Regular.ttf", 14, 2, 2 );  // Create Font (Height: 14 Pixels / X+Y Padding 2 Pixels)

		// enable texture + alpha blending
		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
	
		
		gl.glEnable(GL10.GL_TEXTURE_2D);			//Enable Texture Mapping ( NEW )
		gl.glShadeModel(GL10.GL_SMOOTH); 			//Enable Smooth Shading
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.5f); 	//Black Background
		gl.glClearDepthf(1.0f); 					//Depth Buffer Setup
		gl.glEnable(GL10.GL_DEPTH_TEST); 			//Enables Depth Testing
		gl.glDepthFunc(GL10.GL_LEQUAL); 			//The Orientation Of Depth Testing To Do
		
		//Really Nice Perspective Calculations
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST); 

	}
	//*/
}