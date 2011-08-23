package com.backyardbrains.drawing;

import android.content.Context;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * OscilliscopeGLSurfaceView is a custom SurfaceView that implements callbacks
 * for its own holder, and manages its own GL drawing thread.
 * 
 * @author Nathan Dotz <nate@backyardbrains.com>
 * 
 */
public class OscilliscopeGLSurfaceView extends SurfaceView implements
		SurfaceHolder.Callback {

	/**
	 * SurfaceView's SurfaceHolder, to catch callbacks
	 */
	SurfaceHolder mAndroidHolder;

	/**
	 * The {@link OscilliscopeGLThread} we'll be instantiating.
	 */
	private OscilliscopeGLThread mGLThread;

	/**
	 * Used by instantiating activity to reach down in to drawing thread
	 * 
	 * @return the mGLThread
	 */
	public OscilliscopeGLThread getGLThread() {
		return mGLThread;
	}

	public OscilliscopeGLSurfaceView(Context context) {
		super(context);
		mAndroidHolder = getHolder();
		mAndroidHolder.addCallback(this);
		mAndroidHolder.setType(SurfaceHolder.SURFACE_TYPE_GPU);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		mGLThread.rescaleWaveform();
	}

	/**
	 * When we're created, immediately spin up a new OscilloscopeGLThread
	 * 
	 * @see android.view.SurfaceHolder.Callback#surfaceCreated(android.view.SurfaceHolder)
	 */
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mGLThread = new OscilliscopeGLThread(this);
		mGLThread.start();
	}

	/**
	 * Require cleanup of GL resources before exiting.
	 * 
	 * @see android.view.SurfaceHolder.Callback#surfaceDestroyed(android.view.SurfaceHolder)
	 */
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (mGLThread != null) {
			mGLThread.requestStop();
		}
	}
}