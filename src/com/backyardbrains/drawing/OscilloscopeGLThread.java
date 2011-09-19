package com.backyardbrains.drawing;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.opengl.GLDebugHelper;
import android.os.IBinder;
import android.util.Log;
import android.view.SurfaceView;

import com.backyardbrains.audio.AudioService;
import com.backyardbrains.audio.AudioService.AudioServiceBinder;

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
	private float xEnd = 4000f;

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
		mScaleFactor = Math.max(0.01f, Math.min(mScaleFactor, 3.0f));

		this.mScaleFactor = mScaleFactor;
	}

	private AudioService mAudioService;
	private boolean mAudioServiceIsBound;

	/**
	 * @param view
	 *            reference to the parent view
	 */
	OscilloscopeGLThread(SurfaceView view) {
		parent = view;
	}

	private ServiceConnection mConnection = new ServiceConnection() {

		/**
		 * Sets a reference in this activity to the {@link AudioService}, which
		 * allows for {@link ByteBuffer}s full of audio information to be passed
		 * from the {@link AudioService} down into the local
		 * {@link OscilloscopeGLSurfaceView}
		 * 
		 * @see android.content.ServiceConnection#onServiceConnected(android.content.ComponentName,
		 *      android.os.IBinder)
		 */
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// We've bound to LocalService, cast the IBinder and get
			// LocalService instance
			AudioServiceBinder binder = (AudioServiceBinder) service;
			mAudioService = binder.getService();
			mAudioServiceIsBound = true;
		}

		/**
		 * Clean up bindings
		 * 
		 * @TODO null out mAudioService
		 * 
		 * @see android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
		 */
		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mAudioServiceIsBound = false;
		}
	};
	private GlSurfaceManager glman;

	/**
	 * Initialize GL bits, set up the GL area so that we're lookin at it
	 * properly, create a new {@link BybGLDrawable}, then commence drawing on it
	 * like the dickens.
	 * 
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		glman = new GlSurfaceManager(parent);
		waveformShape = new BybGLDrawable(this);

		Intent intent = new Intent(parent.getContext(), AudioService.class);
		parent.getContext().getApplicationContext()
				.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		while (!mDone) {
			// grab current audio from audioservice
			if (mAudioServiceIsBound) {
				byte[] audioInfo = null;
				synchronized (this) {
					audioInfo = mAudioService.getCurrentAudioInfo();
				}
				if (audioInfo != null) {
					Log.v(TAG, "Got byte array: " + audioInfo[0] + ", "
							+ audioInfo[0] + ", ");
					ByteBuffer tmp = ByteBuffer.wrap(audioInfo);
					ShortBuffer tmp2 = tmp.asShortBuffer();
					short[] mBufferToDraw = new short[tmp2.capacity()];
					tmp2.get(mBufferToDraw, 0, mBufferToDraw.length);
					setxEnd(mBufferToDraw.length / 2);

					glman.glClear();
					waveformShape.setBufferToDraw(mBufferToDraw);
					glman.initGL(xBegin, xEnd, yBegin / mScaleFactor, yEnd
							/ mScaleFactor);
					waveformShape.draw(glman.getmGL());
				}

			}
			glman.swapBuffers();
		}
		parent.getContext().getApplicationContext().unbindService(mConnection);
	}

	/**
	 * Properly clean up GL stuff when exiting
	 * 
	 * @see OscilloscopeGLThread#cleanupGL()
	 */
	public void requestStop() {
		mDone = true;
		glman.cleanupGL();
		try {
			join();
		} catch (InterruptedException e) {
			Log.e(TAG, "GL Thread couldn't rejoin!", e);
		}
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
