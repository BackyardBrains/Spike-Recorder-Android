package com.backyardbrains.drawing;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

/**
 * OscilloscopeGLSurfaceView is a custom SurfaceView that implements callbacks
 * for its own holder, and manages its own GL drawing thread.
 * 
 * @author Nathan Dotz <nate@backyardbrains.com>
 * 
 */
public class OscilloscopeGLSurfaceView extends SurfaceView implements
		SurfaceHolder.Callback {

	private static final String TAG = "OsciliscopeGLSurfaceView";

	/**
	 * SurfaceView's SurfaceHolder, to catch callbacks
	 */
	SurfaceHolder mAndroidHolder;

	/**
	 * The {@link OscilloscopeGLThread} we'll be instantiating.
	 */
	private OscilloscopeGLThread mGLThread;

	private ScaleGestureDetector mScaleDetector;

	/**
	 * Used by instantiating activity to reach down in to drawing thread
	 * 
	 * @return the mGLThread
	 */
	public OscilloscopeGLThread getGLThread() {
		return mGLThread;
	}

	public OscilloscopeGLSurfaceView(Context context) {
		this(context, null, 0);
	}

	public OscilloscopeGLSurfaceView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public OscilloscopeGLSurfaceView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		mAndroidHolder = getHolder();
		mAndroidHolder.addCallback(this);
		mAndroidHolder.setType(SurfaceHolder.SURFACE_TYPE_GPU);
		
		setKeepScreenOn(true);
		mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
	}
	
	public void setMsText(Float ms) {
		Intent i = new Intent();
		i.setAction("BYBUpdateMillisecondsReciever");
		i.putExtra("millisecondsDisplayedString", ms.toString() + " ms");
		getContext().sendBroadcast(i);
	}

	private class ScaleListener extends
			ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			float mScaleFactor = mGLThread.getmScaleFactor();
			float scaleModifier = detector.getScaleFactor();
			scaleModifier = Math.max(0.95f, Math.min(scaleModifier, 1.05f));
			mScaleFactor *= scaleModifier;

			Log.d(TAG, "Receiving touch event with scale factor of "
					+ scaleModifier + "- scale factor is now " + mScaleFactor);

			synchronized (mGLThread) {
				mGLThread.setmScaleFactor(mScaleFactor);
			}
			return super.onScale(detector);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		mScaleDetector.onTouchEvent(event);
		return super.onTouchEvent(event);
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
		mGLThread = new OscilloscopeGLThread(this);
		mGLThread.start();
		setKeepScreenOn(true);
	}

	/**
	 * Require cleanup of GL resources before exiting.
	 * 
	 * @see android.view.SurfaceHolder.Callback#surfaceDestroyed(android.view.SurfaceHolder)
	 */
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		synchronized (mGLThread) {
			if (mGLThread != null) {
				mGLThread.requestStop();
			}	
		}
		setKeepScreenOn(false);
	}
}