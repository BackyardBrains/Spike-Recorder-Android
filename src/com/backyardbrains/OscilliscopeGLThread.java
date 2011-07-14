package com.backyardbrains;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLDebugHelper;
import android.opengl.GLU;
import android.util.Log;
import android.view.SurfaceView;

public class OscilliscopeGLThread extends Thread {

	SurfaceView parent;
	private boolean mDone = false; // signal whether thread is finished
	private EGL10 mEGL;
	private EGLDisplay mGLDisplay;
	private EGLConfig mGLConfig;
	private EGLSurface mGLSurface;
	private EGLContext mGLContext;
	private GL10 mGL;

	OscilliscopeGLThread(SurfaceView view) {
		parent = view;
	}

	public void run() {
		initEGL();
		initGL();
		
		GLU.gluLookAt(mGL, 0, 0, 5f, 0, 0, 0, 0, 1, 0f);
		mGL.glColor4f(0f, 1f, 0f, 1f);
		TestTriangle test_triangle = new TestTriangle();
		while (!mDone) {
			mGL.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
			//mGL.glRotatef(1f, 0, 0, 1f);
			test_triangle.draw(mGL);
			mEGL.eglSwapBuffers(mGLDisplay, mGLSurface);
		}
	}

	public void requestStop() {
		mDone = true;
		try {
			join();
		} catch (InterruptedException e) {
			Log.e("BYB", "GL Thread couldn't rejoin!", e);
		}
		cleanupGL();

	}

	private void cleanupGL() {
		// TODO Auto-generated method stub

	}

	private void initGL() {
		// set viewport
		int width = parent.getWidth();
		int height = parent.getHeight();
		mGL.glViewport(0,0,width,height);
		
		mGL.glMatrixMode(GL10.GL_PROJECTION);
		mGL.glLoadIdentity();
		// carry-over from GLUT
		GLU.gluPerspective(mGL, 45f, (float)width/height, 1f, 30f);
		
		// Blackout, then we're ready to draw! \o/
		mGL.glClearColor(0f, 0f, 0f, 1.0f);
		
	}

	private void initEGL() {
		/**
		 * Get EGL object, display object, and initialize
		 */
		mEGL = (EGL10) GLDebugHelper.wrap(EGLContext.getEGL(),
				GLDebugHelper.CONFIG_CHECK_GL_ERROR
						| GLDebugHelper.CONFIG_CHECK_THREAD, null);

		mGLDisplay = mEGL.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

		// pass an array to eglInitialize to get the current GL Version
		int[] curGLVersion = new int[2];
		mEGL.eglInitialize(mGLDisplay, curGLVersion);

		/**
		 * Create a spec for the EGL config we'd like, then pass it to
		 * ChooseConfig, which will populate our configs array with a closest
		 * match
		 */
		int[] mConfigSpec = { EGL10.EGL_RED_SIZE, 5, EGL10.EGL_GREEN_SIZE, 6,
				EGL10.EGL_BLUE_SIZE, 5, EGL10.EGL_DEPTH_SIZE, 16,
				EGL10.EGL_NONE };

		EGLConfig[] configs = new EGLConfig[1];
		int[] num_config = new int[1];
		mEGL.eglChooseConfig(mGLDisplay, mConfigSpec, configs, 1, num_config);
		mGLConfig = configs[0];

		/**
		 * Finally, get a GL Surface to draw on, a context, blit them together,
		 * and populate the GL object.
		 */
		mGLSurface = mEGL.eglCreateWindowSurface(mGLDisplay, mGLConfig,
				parent.getHolder(), null);
		mGLContext = mEGL.eglCreateContext(mGLDisplay, mGLConfig, EGL10.EGL_NO_CONTEXT, null);
		mEGL.eglMakeCurrent(mGLDisplay, mGLSurface, mGLSurface, mGLContext);
		mGL = (GL10) GLDebugHelper.wrap(
				mGLContext.getGL(),
				GLDebugHelper.CONFIG_CHECK_GL_ERROR
				| GLDebugHelper.CONFIG_CHECK_THREAD, null);

	}
	
	private class TestTriangle {
		
		private float[] vertices = {
				-3f, 0, 0,
				-2.75f, 0, 0,
				-2.5f, 0, 0,
				-2.25f, 0, 0,
				-2f, 0, 0,
				-1.75f, 0, 0,
				-1.5f, 0, 0,
				-1.25f, 0, 0,
				-1f, 0, 0,
				-0.75f, 0, 0,
				-0.5f, 0, 0,
				-0.25f, 0, 0,
				0f, 0, 0,
				0.25f, 0, 0,
				0.5f, 0, 0,
				0.75f, 0, 0,
				1f, 0, 0,
				1.25f, 0, 0,
				1.5f, 0, 0,
				1.75f, 0, 0,
				2f, 0, 0,
				2.25f, 0, 0,
				2.5f, 0, 0,
				2.75f, 0, 0,
				3f, 0, 0
			};
		
		FloatBuffer getFloatBufferFromFloatArray(float array[]) {
			ByteBuffer temp = ByteBuffer.allocateDirect(array.length * 4);
			temp.order(ByteOrder.nativeOrder());
			FloatBuffer buf = temp.asFloatBuffer();
			buf.put(array);
			buf.position(0);
			return buf;
		}
		
		public float[] transform(float array[]) {
			Random derp = new Random();
			for (int i=0 ; i<array.length-3; i++) {
				// ys
				if((i+2)%3 == 0) {
					// copy y from next element
					array[i] = array[i+3];
				}
				// randomize last y
			}
			array[array.length-2] = derp.nextFloat() *(float)derp.nextInt()%3 -1;
			return array;
		}
		
		public void draw(GL10 gl_obj) {
			vertices = transform(vertices);
			FloatBuffer mVertexBuffer = getFloatBufferFromFloatArray(vertices);
			gl_obj.glEnableClientState(GL10.GL_VERTEX_ARRAY);
			gl_obj.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);
			gl_obj.glDrawArrays(GL10.GL_LINE_STRIP, 0, vertices.length/3);
		}
	}
}
