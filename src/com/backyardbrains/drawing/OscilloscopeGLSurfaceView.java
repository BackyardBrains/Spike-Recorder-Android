package com.backyardbrains.drawing;

import java.text.DecimalFormat;

import com.backyardbrains.view.TwoDimensionScaleGestureDetector;
import com.backyardbrains.view.TwoDimensionScaleGestureDetector.Simple2DOnScaleGestureListener;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
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

	private float bufferLengthDivisor = 1;

	public float getBufferLengthDivisor() {
		return bufferLengthDivisor;
	}

	public void setBufferLengthDivisor(float bufferLengthDivisor) {
		this.bufferLengthDivisor = bufferLengthDivisor;
	}

	private float scaleFactor = 1;

	private TwoDimensionScaleGestureDetector mScaleDetector;

	public float getScaleFactor() {
		return scaleFactor;
	}

	public void setScaleFactor(float scaleFactor) {
		this.scaleFactor = scaleFactor;
	}

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
		mScaleDetector = new TwoDimensionScaleGestureDetector(context, new ScaleListener());
	}

	public OscilloscopeGLSurfaceView(Context context, float scaleFactor,
			float bufferLengthDivisor) {
		this(context);
		this.scaleFactor = scaleFactor;
		this.bufferLengthDivisor = bufferLengthDivisor;
	}

	public void setMsText(Float ms) {
		Intent i = new Intent();
		i.setAction("BYBUpdateMillisecondsReciever");
		String msString = new DecimalFormat("#.#").format(ms);
		i.putExtra("millisecondsDisplayedString", msString + " ms");
		getContext().sendBroadcast(i);
	}

	public void shrinkXdimension() {
		mGLThread
				.setBufferLengthDivisor(mGLThread.getBufferLengthDivisor() + 1);
	}

	public void growXdimension() {
		mGLThread
				.setBufferLengthDivisor(mGLThread.getBufferLengthDivisor() - 1);
	}

	private class ScaleListener extends
			Simple2DOnScaleGestureListener {
		@Override
		public boolean onScale(TwoDimensionScaleGestureDetector detector) {
			//float mScaleFactor = mGLThread.getmScaleFactor();
			//float scaleModifier = detector.getScaleFactor();
			final Pair<Float, Float> scaleModifier = detector.getScaleFactor();
			final float scaleModifierX = Math.max(0.98f, Math.min(scaleModifier.first, 1.02f));
			final float scaleModifierY = Math.max(0.98f, Math.min(scaleModifier.second, 1.02f));
			bufferLengthDivisor *= scaleModifierX;
			scaleFactor *= scaleModifierY;

			Log.d(TAG, "Receiving touch event. scale factor is now " + scaleFactor + "and buffer divisor is " + bufferLengthDivisor);
			/*
			synchronized (mGLThread) {
				mGLThread.setmScaleFactor(scaleFactor);
				mGLThread.setBufferLengthDivisor(bufferLengthDivisor);
			}
			*/
			Intent i = new Intent();
			i.setAction("BYBScaleChange");
			i.putExtra("newBufferLengthDivisor", bufferLengthDivisor);
			i.putExtra("newScaleFactor", scaleFactor);
			getContext().sendBroadcast(i);

			return super.onScale(detector);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		/*
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_POINTER_DOWN:
			startDistanceX = event.getX(0) - event.getX(1);
			startDistanceY = event.getY(0) - event.getY(1);
			break;

		case MotionEvent.ACTION_MOVE:
			float distanceX = event.getX(0) - event.getX(1);
			float distanceY = event.getY(0) - event.getY(1);
			if (distanceX > 10f && distanceY > 10f) { // de-bounce weird
														// anomalies
				float scaleX = distanceX / startDistanceX;
				float scaleY = distanceY / startDistanceY;
				Log.d(TAG, "New X distance is " + distanceX
						+ " -- New X scale is " + scaleX);
				Log.d(TAG, "New Y distance is " + distanceY
						+ " -- New Y scale is " + scaleY);

				if (scaleY > 1.25) {
					scaleY = Math.min(scaleY - 0.25f, 1.025f);
				} else if (scaleY < 0.75) {
					scaleY = Math.max(scaleY + 0.25f, 0.975f);
				}
				scaleFactor *= scaleY;
				scaleFactor = Math.max(0.01f, Math.min(scaleFactor, 3.0f));

				if (scaleX > 1.2 && bufferLengthDivisor <= 15.8) {
					bufferLengthDivisor = (bufferLengthDivisor + 0.2f);
				} else if (scaleX < 0.8 && bufferLengthDivisor >= 1.2) {
					bufferLengthDivisor = (bufferLengthDivisor - 0.2f);
				}

				Intent i = new Intent();
				i.setAction("BYBScaleChange");
				i.putExtra("newBufferLengthDivisor", bufferLengthDivisor);
				i.putExtra("newScaleFactor", scaleFactor);
				getContext().sendBroadcast(i);
			}
		}
		*/
		mScaleDetector.onTouchEvent(event);
		return super.onTouchEvent(event);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		mGLThread.setBufferLengthDivisor(bufferLengthDivisor);
		mGLThread.setmScaleFactor(scaleFactor);
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
		synchronized (mGLThread) {
			mGLThread.setBufferLengthDivisor(bufferLengthDivisor);
			mGLThread.setmScaleFactor(scaleFactor);
			Log.d(TAG, "Started GL thread with scale of " + scaleFactor
					+ " and bufferLengthDivisor of " + bufferLengthDivisor);
			mGLThread.start();
		}
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