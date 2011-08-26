package com.backyardbrains.drawing;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

import com.backyardbrains.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLDebugHelper;
import android.opengl.GLUtils;
import android.util.Log;
import android.view.SurfaceView;

/**
 * A {@link Thread} which manages continuous drawing of a {@link BybGLDrawable}
 * onto a {@link OscilloscopeGLSurfaceView}
 * 
 * @author Nathan Dotz <nate@backyardbrains.com>
 * @version 1
 * 
 */
public class OscilloscopeGLThread extends Thread {

	private float xBegin = 00f;
	private float xEnd = 4000f;;

	public float getxBegin() {
		return xBegin;
	}

	public void setxBegin(float xBegin) {
		this.yBegin = xBegin;
	}

	public float getxEnd() {
		return xEnd;
	}

	public void setxEnd(float xEnd) {
		this.xEnd = xEnd;
	}

	private float yMin = -5000000f;
	private float yMax = 5000000f;

	public float getyMin() {
		return yMin;
	}

	public void setyMin(float yMin) {
		this.yMin = yMin;
	}

	public float getyMax() {
		return yMax;
	}

	public void setyMax(float yMax) {
		this.yMax = yMax;
	}

	private float yBegin = -5000f;
	private float yEnd = 5000f;

	public float getyBegin() {
		return yBegin;
	}

	public void setyBegin(float yBegin) {
		this.yBegin = yBegin;
	}

	public float getyEnd() {
		return yEnd;
	}

	public void setyEnd(float yEnd) {
		this.yEnd = yEnd;
	}

	/**
	 * reference to parent {@link OscilloscopeGLSurfaceView}
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
	public float x_width = 100;
	public int numVerticalGridLines = 9;
	int numHorizontalGridLines = 6;
	private static final String TAG = "BYBOsciliscopeGlThread";
	private BybGLDrawable waveformShape;
	private float mScaleFactor = 1.f;

	public float getmScaleFactor() {
		return mScaleFactor;
	}

	public void setmScaleFactor(float mScaleFactor) {
		// Don't let the object get too small or too large.
		mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 5.0f));
		
		this.mScaleFactor = mScaleFactor;
	}

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
	OscilloscopeGLThread(SurfaceView view) {
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
		waveformShape = new BybGLDrawable(this);
		while (!mDone) {
			mGL.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

			waveformShape.setBufferToDraw(audioBuffer);
			waveformShape.draw(mGL);

			mEGL.eglSwapBuffers(mGLDisplay, mGLSurface);
		}
	}

	/**
	 * Properly clean up GL stuff when exiting
	 * 
	 * @see OscilloscopeGLThread#cleanupGL()
	 */
	public void requestStop() {
		mDone = true;
		try {
			join();
		} catch (InterruptedException e) {
			Log.e(TAG, "GL Thread couldn't rejoin!", e);
		}
		cleanupGL();
	}

	/**
	 * called by {@link OscilloscopeGLThread#requestStop()} to make sure the GL
	 * native interface isn't leaving behind turds.
	 */
	private void cleanupGL() {
		mEGL.eglMakeCurrent(mGLDisplay, EGL10.EGL_NO_SURFACE,
				EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
		mEGL.eglDestroySurface(mGLDisplay, mGLSurface);
		mEGL.eglDestroyContext(mGLDisplay, mGLContext);
		mEGL.eglTerminate(mGLDisplay);
	}

	/**
	 * Convenience function for {@link OscilloscopeGLThread#run()}. Builds basic
	 * GL Viewport and sets defaults.
	 */
	void initGL() {
		// set viewport
		int width = parent.getWidth();
		int height = parent.getHeight();
		mGL.glViewport(0, 0, width, height);

		mGL.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		mGL.glMatrixMode(GL10.GL_PROJECTION);
		mGL.glLoadIdentity();
		mGL.glOrthof(xBegin, xEnd, yBegin / mScaleFactor, yEnd / mScaleFactor, -1f, 1f);
		mGL.glRotatef(0f, 0f, 0f, 1f);

		// Blackout, then we're ready to draw! \o/
		// mGL.glEnable(GL10.GL_TEXTURE_2D);
		mGL.glClearColor(0f, 0f, 0f, 0.5f);
		mGL.glClearDepthf(1.0f);
		mGL.glEnable(GL10.GL_DEPTH_TEST);
		mGL.glDepthFunc(GL10.GL_LEQUAL);

		mGL.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
		drawTickmarks(mGL);
		
		

	}

	private void drawTickmarks(GL10 glObj) {
		float height = Math.abs(yBegin - yEnd);
		float width_scale = 16.0f / parent.getWidth();
		float width = Math.abs(xBegin - xEnd) * width_scale;
		int[] textures = new int[1];

		float[] vertices = { 0f, -height / 2f, 0f, width, -height / 2f, 0f, 0f,
				height / 2f, 0f, width, height / 2f, 0f };

		float[] texture = { 0f, -height / 2f, 0f, height / 2f, width,
				-height / 2f, width, height / 2f };

		Context context = parent.getContext();
		Bitmap bmp = BitmapFactory.decodeResource(context.getResources(),
				R.drawable.tickmarks);
		
		glObj.glEnable(GL10.GL_BLEND);
        glObj.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);

		glObj.glGenTextures(1, textures, 0);
		glObj.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);

		glObj.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER,
				GL10.GL_NEAREST);
		glObj.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER,
				GL10.GL_LINEAR);
		GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bmp, 0);
		bmp.recycle();

		glObj.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);
		// Point to our buffers
		glObj.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		glObj.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

		// Set the face rotation
		glObj.glFrontFace(GL10.GL_CW);

		// Point to our vertex buffer
		glObj.glVertexPointer(3, GL10.GL_FLOAT, 0,
				getFloatBufferFromFloatArray(vertices));
		glObj.glTexCoordPointer(2, GL10.GL_FLOAT, 0,
				getFloatBufferFromFloatArray(texture));

		// Draw the vertices as triangle strip
		glObj.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, vertices.length / 3);
		// Disable the client state before leaving
		glObj.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		glObj.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

	}

	/**
	 * Convenience function for {@link OscilloscopeGLThread#run()}. Builds basic
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
		mGLSurface = mEGL.eglCreateWindowSurface(mGLDisplay, mGLConfig, parent
				.getHolder(), null);
		mGLContext = mEGL.eglCreateContext(mGLDisplay, mGLConfig,
				EGL10.EGL_NO_CONTEXT, null);
		mEGL.eglMakeCurrent(mGLDisplay, mGLSurface, mGLSurface, mGLContext);
		mGL = (GL10) GLDebugHelper.wrap(mGLContext.getGL(),
				GLDebugHelper.CONFIG_CHECK_GL_ERROR
						| GLDebugHelper.CONFIG_CHECK_THREAD, null);

	}

	/**
	 * Takes an array of floats and returns a buffer representing the same
	 * floats
	 * 
	 * @param array
	 *            to be converted
	 * @return converted array as FloatBuffer
	 */
	FloatBuffer getFloatBufferFromFloatArray(float[] array) {
		ByteBuffer temp = ByteBuffer.allocateDirect(array.length * 4);
		temp.order(ByteOrder.nativeOrder());
		FloatBuffer buf = temp.asFloatBuffer();
		buf.put(array);
		buf.position(0);
		return buf;
	}

	public void rescaleWaveform() {
		if (waveformShape != null)
			waveformShape.forceRescale();
	}
}
