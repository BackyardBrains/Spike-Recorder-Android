package com.backyardbrains.drawing;

import java.nio.ByteBuffer;

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

/**
 * A {@link Thread} which manages continuous drawing of a {@link BybGLDrawable}
 * onto a {@link OscilliscopeGLSurfaceView}
 * 
 * @author Nathan Dotz <nate@backyardbrains.com>
 * @version 1
 * 
 */
public class OscilliscopeGLThread extends Thread {

	/**
	 * reference to parent {@link OscilliscopeGLSurfaceView}
	 */
	SurfaceView parent;

	/**
	 * Is thread done processing yet? Used at requestStop
	 */
	private boolean mDone = false;

	/**
	 * Necessary GL detritus.
	 */
	private EGL10 mEGL;
	private EGLDisplay mGLDisplay;
	private EGLConfig mGLConfig;
	private EGLSurface mGLSurface;
	private EGLContext mGLContext;
	private GL10 mGL;

	/**
	 * storage for {@link ByteBuffer} to be transfered to {@link BybGLDrawable}
	 */
	private ByteBuffer audioBuffer;
	public float x_width = 100f;

	/**
	 * Called by the instantiating activity, this sets to {@link ByteBuffer} to
	 * be sent down to the {@link BybGLDrawable} on the new call to
	 * {@link BybGLDrawable#draw(GL10)}
	 * 
	 * @param audioBuffer
	 * 
	 */
	public void setAudioBuffer(ByteBuffer audioBuffer) {
		this.audioBuffer = audioBuffer;
	}

	/**
	 * @param view
	 *            reference to the parent view
	 */
	OscilliscopeGLThread(SurfaceView view) {
		parent = view;
	}

	/**
	 * Initialize GL bits, set up the GL area so that we're lookin at it
	 * properly, create a new {@link BybGLDrawable}, then commence drawing on it
	 * like the dickens.
	 * 
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		initEGL();
		initGL();
		float leftside = 25;
		float height = 27;
		GLU.gluLookAt(mGL, leftside, 0, height, leftside, 0, 0, 0, 1, 0f);
		mGL.glColor4f(0f, 1f, 0f, 1f);
		BybGLDrawable waveform_shape = new BybGLDrawable(this);
		while (!mDone) {
			mGL.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

			waveform_shape.setBufferToDraw(audioBuffer);
			waveform_shape.draw(mGL);

			mEGL.eglSwapBuffers(mGLDisplay, mGLSurface);
		}
	}

	/**
	 * Properly clean up GL stuff when exiting
	 * 
	 * @see OscilliscopeGLThread#cleanupGL()
	 */
	public void requestStop() {
		mDone = true;
		try {
			join();
		} catch (InterruptedException e) {
			Log.e("BYB", "GL Thread couldn't rejoin!", e);
		}
		cleanupGL();
	}

	/**
	 * called by {@link OscilliscopeGLThread#requestStop()} to make sure the GL native interface isn't leaving
	 * behind turds.
	 */
	private void cleanupGL() {
		mEGL.eglMakeCurrent(mGLDisplay, EGL10.EGL_NO_SURFACE,
				EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
		mEGL.eglDestroySurface(mGLDisplay, mGLSurface);
		mEGL.eglDestroyContext(mGLDisplay, mGLContext);
		mEGL.eglTerminate(mGLDisplay);
	}

	/**
	 * Convenience function for {@link OscilliscopeGLThread#run()}. Builds basic
	 * GL Viewport and sets defaults.
	 */
	private void initGL() {
		// set viewport
		int width = parent.getWidth();
		int height = parent.getHeight();
		mGL.glViewport(0, 0, width, height);

		mGL.glMatrixMode(GL10.GL_PROJECTION);
		mGL.glLoadIdentity();
		// carry-over from GLUT
		GLU.gluPerspective(mGL, 45f, (float) width / height, 1f, 30f);

		// Blackout, then we're ready to draw! \o/
		mGL.glClearColor(0f, 0f, 0f, 1.0f);

	}

	/**
	 * Convenience function for {@link OscilliscopeGLThread#run()}. Builds basic
	 * surface we need to draw on. Mostly boilerplate. No Touchy.
	 */
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
		mGLContext = mEGL.eglCreateContext(mGLDisplay, mGLConfig,
				EGL10.EGL_NO_CONTEXT, null);
		mEGL.eglMakeCurrent(mGLDisplay, mGLSurface, mGLSurface, mGLContext);
		mGL = (GL10) GLDebugHelper.wrap(mGLContext.getGL(),
				GLDebugHelper.CONFIG_CHECK_GL_ERROR
						| GLDebugHelper.CONFIG_CHECK_THREAD, null);

	}
}
