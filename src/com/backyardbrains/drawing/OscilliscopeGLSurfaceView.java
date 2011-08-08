package com.backyardbrains.drawing;

import android.content.Context;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * OscilliscopeGLSurfaceView is a custom SurfaceView that implements callbacks
 * for its own holder, and manages its own GL drawing thread.
 * 
 * @author nate
 * 
 */
class OscilliscopeGLSurfaceView extends SurfaceView implements
		SurfaceHolder.Callback {

	SurfaceHolder mAndroidHolder;
	private OscilliscopeGLThread mGLThread;

	/**
	 * @return the mGLThread
	 */
	public OscilliscopeGLThread getGLThread() {
		return mGLThread;
	}

	OscilliscopeGLSurfaceView(Context context) {
		super(context);
		mAndroidHolder = getHolder();
		mAndroidHolder.addCallback(this);
		mAndroidHolder.setType(SurfaceHolder.SURFACE_TYPE_GPU);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mGLThread = new OscilliscopeGLThread(this);
		mGLThread.start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (mGLThread != null) {
			mGLThread.requestStop();
		}
	}
}