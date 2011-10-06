package com.backyardbrains.drawing;

import java.text.DecimalFormat;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

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

	private float startDistanceX;

	private float startDistanceY;

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
	}
	
	public void setMsText(Float ms) {
		Intent i = new Intent();
		i.setAction("BYBUpdateMillisecondsReciever");
		String msString = new DecimalFormat("#.#").format(ms);
		i.putExtra("millisecondsDisplayedString", msString + " ms");
		getContext().sendBroadcast(i);
	}

	public void shrinkXdimension() {
		mGLThread.setBufferLengthDivisor(mGLThread.getBufferLengthDivisor()+1);
	}
	
	public void growXdimension() {
		mGLThread.setBufferLengthDivisor(mGLThread.getBufferLengthDivisor()-1);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_POINTER_DOWN:
			startDistanceX = event.getX(0) - event.getX(1);
			startDistanceY = event.getY(0) - event.getY(1);
			break;
		
		case MotionEvent.ACTION_MOVE:
			float distanceX = event.getX(0) - event.getX(1);
			float distanceY = event.getY(0) - event.getY(1);
			if (distanceX > 10f && distanceY > 10f) { // de-bounce weird anomalies
				float scaleX = distanceX / startDistanceX;
				float scaleY = distanceY / startDistanceY;
				Log.d(TAG, "New X distance is " + distanceX + " -- New X scale is " + scaleX);
				Log.d(TAG, "New Y distance is " + distanceY + " -- New Y scale is " + scaleY);
				Intent i = new Intent ();
				i.setAction("BYBScaleChange");
				i.putExtra("scaleX", scaleX);
				i.putExtra("scaleY", scaleY);
				getContext().sendBroadcast(i);
			}
		}
		
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