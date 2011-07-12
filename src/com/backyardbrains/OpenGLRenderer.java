package com.backyardbrains;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLU;
import android.opengl.GLSurfaceView.Renderer;

public class OpenGLRenderer implements Renderer {

	// Initialize our WaveView.
	WaveView waveview = new WaveView();
	
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		// Set the background color to black ( rgba ).
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.5f);  // OpenGL docs.
		// Enable Smooth Shading, default not really needed.
		gl.glShadeModel(GL10.GL_SMOOTH);// OpenGL docs.
		// Depth buffer setup.
		gl.glClearDepthf(1.0f);// OpenGL docs.
		// Enables depth testing.
		gl.glEnable(GL10.GL_DEPTH_TEST);// OpenGL docs.
		// The type of depth testing to do.
		gl.glDepthFunc(GL10.GL_LEQUAL);// OpenGL docs.
		// Really nice perspective calculations.
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, // OpenGL docs.
                          GL10.GL_NICEST);
	}
	

	public void onDrawFrame(GL10 gl) {
		// Clears the screen and depth buffer.
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | // OpenGL docs.
                           GL10.GL_DEPTH_BUFFER_BIT);

		// Translates 4 units into the screen.
		gl.glTranslatef(0, 0, -3); // OpenGL docs
					
		// Draw our spike.
		waveview.draw(gl); // ( NEW )
		
		// Replace the current matrix with the identity matrix
		gl.glLoadIdentity(); // OpenGL docs
	}

	public void onSurfaceChanged(GL10 gl, int width, int height) {
//		// Sets the current view port to the new size.
		gl.glViewport(0, 0, width, height);
//		// Select the projection matrix
		gl.glMatrixMode(GL10.GL_PROJECTION);
//		// Reset the projection matrix
		gl.glLoadIdentity();// OpenGL docs.
//		// Calculate the aspect ratio of the window
		GLU.gluPerspective(gl, 45.0f,
                           (float) width / (float) height,
                           0.1f, 100.0f);
		// Select the modelview matrix
		gl.glMatrixMode(GL10.GL_MODELVIEW);
//		// Reset the modelview matrix
		gl.glLoadIdentity();
	}
}