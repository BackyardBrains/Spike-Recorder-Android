package com.backyardbrains.drawing;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLDebugHelper;
import android.view.SurfaceView;

public class GlSurfaceManager {

	private EGL10 mEGL;
	private EGLDisplay mGLDisplay;
	private EGLConfig mGLConfig;
	private EGLSurface mGLSurface;
	private EGLContext mGLContext;
	private GL10 mGL;

	public GL10 getmGL() {
		return mGL;
	}

	private SurfaceView parent;

	public GlSurfaceManager(SurfaceView parent) {
		this.parent = parent;
		initEGL();
	}

	public void glClear() {
		mGL.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
	}

	/**
	 * Convenience function for {@link OscilloscopeGLThread#run()}. Builds basic
	 * GL Viewport and sets defaults.
	 * 
	 * @param xBegin
	 * @param xEnd
	 * @param scaledYBegin
	 * @param scaledYEnd
	 */
	void initGL(float xBegin, float xEnd, float scaledYBegin, float scaledYEnd) {
		// set viewport
		int width = parent.getWidth();
		int height = parent.getHeight();
		mGL.glViewport(0, 0, width, height);

		glClear();
		mGL.glMatrixMode(GL10.GL_PROJECTION);
		mGL.glLoadIdentity();
		mGL.glOrthof(xBegin, xEnd, scaledYBegin, scaledYEnd, -1f, 1f);
		mGL.glRotatef(0f, 0f, 0f, 1f);

		// Blackout, then we're ready to draw! \o/
		// mGL.glEnable(GL10.GL_TEXTURE_2D);
		mGL.glClearColor(0f, 0f, 0f, 0.5f);
		mGL.glClearDepthf(1.0f);
		mGL.glEnable(GL10.GL_DEPTH_TEST);
		mGL.glDepthFunc(GL10.GL_LEQUAL);
		mGL.glEnable(GL10.GL_LINE_SMOOTH );
		mGL.glHint(GL10.GL_LINE_SMOOTH_HINT, GL10.GL_NICEST);
		// Enable Blending
		mGL.glEnable(GL10.GL_BLEND);
		// Specifies pixel arithmetic
		mGL.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

		mGL.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);

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
		mGLContext = mEGL.eglCreateContext(mGLDisplay, mGLConfig,
				EGL10.EGL_NO_CONTEXT, null);
		mEGL.eglMakeCurrent(mGLDisplay, mGLSurface, mGLSurface, mGLContext);
		mGL = (GL10) GLDebugHelper.wrap(mGLContext.getGL(),
				GLDebugHelper.CONFIG_CHECK_GL_ERROR
						| GLDebugHelper.CONFIG_CHECK_THREAD, null);

	}

	public void swapBuffers() {
		mEGL.eglSwapBuffers(mGLDisplay, mGLSurface);
	}

	/**
	 * called by {@link OscilloscopeGLThread#requestStop()} to make sure the GL
	 * native interface isn't leaving behind turds.
	 */
	public void cleanupGL() {
		mEGL.eglMakeCurrent(mGLDisplay, EGL10.EGL_NO_SURFACE,
				EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
		mEGL.eglDestroySurface(mGLDisplay, mGLSurface);
		mEGL.eglDestroyContext(mGLDisplay, mGLContext);
		mEGL.eglTerminate(mGLDisplay);
	}
}
