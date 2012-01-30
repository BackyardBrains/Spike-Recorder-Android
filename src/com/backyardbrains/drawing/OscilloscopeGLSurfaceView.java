package com.backyardbrains.drawing;

import java.text.DecimalFormat;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.backyardbrains.BybConfigHolder;
import com.backyardbrains.view.TwoDimensionScaleGestureDetector;
import com.backyardbrains.view.TwoDimensionScaleGestureDetector.Simple2DOnScaleGestureListener;

/**
 * OscilloscopeGLSurfaceView is a custom SurfaceView that implements callbacks
 * for its own holder, and manages its own GL drawing thread.
 * 
 * @author Nathan Dotz <nate@backyardbrains.com>
 * 
 */
public class OscilloscopeGLSurfaceView extends SurfaceView implements
		SurfaceHolder.Callback {

	private static final String TAG = OscilloscopeGLSurfaceView.class
			.getCanonicalName();

	private TwoDimensionScaleGestureDetector mScaleDetector;
	SurfaceHolder mAndroidHolder;
	private OscilloscopeGLThread mGLThread;

	private boolean triggerView;
	private boolean didWeAlreadyAutoscale;
	private float scaleFactor = 1;

	private float initialThresholdTouch = -1;

	public OscilloscopeGLSurfaceView(Context context) {
		this(context, null, 0);
	}

	public OscilloscopeGLSurfaceView(Context context, boolean isTriggerMode) {
		this(context, null, 0);
		triggerView = isTriggerMode;
		Log.d(TAG, "Starting surface with triggerView set to " + triggerView);
	}

	public OscilloscopeGLSurfaceView(Context context, BybConfigHolder bch,
			boolean isTriggerMode) {
		this(context, isTriggerMode);
		scaleFactor = bch.configScaleFactor;
		/*
		 * @TODO no longer restores X on orientation change
		 * bufferLengthDivisor = bch.configBufferLengthDivisor;
		 */
		didWeAlreadyAutoscale = bch.configAlreadyAutoScaled;
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
		mScaleDetector = new TwoDimensionScaleGestureDetector(context,
				new ScaleListener());
	}

	public OscilloscopeGLSurfaceView(Context context, float scaleFactor,
			float bufferLengthDivisor) {
		this(context);
		this.scaleFactor = scaleFactor;
		/*
		 * @TODO doesn't preserve X distance on orientation change
		 * this.bufferLengthDivisor = bufferLengthDivisor;
		 */
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		mScaleDetector.onTouchEvent(event);
		final int action = event.getAction();
		if (triggerView) {
			switch (action & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN: {
				if (Math.abs(event.getY() - mGLThread.getThresholdYValue()) < 30) {
					initialThresholdTouch = event.getY();
				}
				break;
			}
			case MotionEvent.ACTION_MOVE: {
				if (!mScaleDetector.isInProgress()
						&& initialThresholdTouch != -1) {

					final float y = event.getY();
					getContext().sendBroadcast(
							new Intent("BYBThresholdChange").putExtra(
									"deltathreshold", y));

				}
				break;
			}
			case MotionEvent.ACTION_POINTER_UP:
			case MotionEvent.ACTION_UP: {
				initialThresholdTouch = -1;
				break;
			}
			}

		}
		return super.onTouchEvent(event);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		/*
		 * @TODO fix this to restore buffer length on orientation change
		 * mGLThread.setBufferLengthDivisor(bufferLengthDivisor);
		 */
		mGLThread.rescaleWaveform();
		setMillivoltLabelPosition(height);
	}

	/**
	 * When we're created, immediately spin up a new OscilloscopeGLThread
	 * 
	 * @see android.view.SurfaceHolder.Callback#surfaceCreated(android.view.SurfaceHolder)
	 */
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		resetDrawingThread();
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
			if (haveThread()) {
				mGLThread.requestStop();
			}
		}
		setKeepScreenOn(false);
	}

	public boolean haveThread() {
		return mGLThread != null;
	}

	public boolean isAutoScaled() {
		if (haveThread()) {
			didWeAlreadyAutoscale = mGLThread.isAutoScaled();
			return didWeAlreadyAutoscale;
		} else
			return didWeAlreadyAutoscale;
	}

	public void setAutoScaled(boolean autoScaled) {
		didWeAlreadyAutoscale = autoScaled;
		if (haveThread()) {
			mGLThread.setAutoScaled(autoScaled);
		}
	}

	private void resetDrawingThread() {
		Log.d(TAG, "Resetting drawing thread with triggerView set to "
				+ triggerView);
		if (haveThread()) {
			Log.d(TAG,
					"We have already had a GL Thread, so let's reset its scaling");
			isAutoScaled();
		}
		if (triggerView) {
			mGLThread = new TriggerViewThread(this);
		} else {
			mGLThread = new OscilloscopeGLThread(this);
		}
		synchronized (this) {
			mGLThread.setAutoScaled(didWeAlreadyAutoscale);
			/* @TODO fix this to reset X distance on orientation change
			 * mGLThread.setBufferLengthDivisor(bufferLengthDivisor);
			 */
			Log.d(TAG, "Started GL thread with scale of " + scaleFactor
					+ " and X distance of " + mGLThread.getxEnd());
			mGLThread.start();
		}
	}

	private void broadcastTextUpdate(String action, String name, String data) {
		Intent i = new Intent();
		i.setAction(action);
		i.putExtra(name, data);
		getContext().sendBroadcast(i);
	}

	public void setMsText(Float ms) {
		String msString = new DecimalFormat("#.#").format(ms);
		broadcastTextUpdate("BYBUpdateMillisecondsReciever",
				"millisecondsDisplayedString", msString + " ms");
	}

	public void setmVText(Float ms) {
		String msString = new DecimalFormat("#.##").format(ms);
		broadcastTextUpdate("BYBUpdateMillivoltReciever",
				"millivoltsDisplayedString", msString + " mV");
	}

	private void setMillivoltLabelPosition(int height) {
		Intent i = new Intent();
		i.setAction("BYBMillivoltsViewSize");
		i.putExtra("millivoltsViewNewSize", height / 2);
		getContext().sendBroadcast(i);
	}

	public float getScaleFactor() {
		return scaleFactor;
	}

	public void setScaleFactor(float scaleFactor) {
		this.scaleFactor = scaleFactor;
	}

	public OscilloscopeGLThread getGLThread() {
		return mGLThread;
	}

	private class ScaleListener extends Simple2DOnScaleGestureListener {
		
		int xSizeAtBeginning = -1;
		int ySizeAtBeginning = -1;
		
		@Override
		public boolean onScaleBegin(TwoDimensionScaleGestureDetector detector) {
			xSizeAtBeginning = mGLThread.getxEnd();
			ySizeAtBeginning = mGLThread.getGlWindowVerticalSize();
			return super.onScaleBegin(detector);
		}
		
		@Override
		public boolean onScale(TwoDimensionScaleGestureDetector detector) {
			
			try {
				final Pair<Float, Float> scaleModifier = detector
						.getScaleFactor();
				int newXsize = (int) (xSizeAtBeginning/scaleModifier.first);
				mGLThread.setxEnd(newXsize);
				
				int newYsize = (int) (ySizeAtBeginning*scaleModifier.second);
				
				mGLThread.setGlWindowVerticalSize(newYsize);
			} catch (IllegalStateException e) {
				Log.e(TAG, "Got invalid values back from Scale listener!");
			}
			return super.onScale(detector);
		}
		
		@Override
		public void onScaleEnd(TwoDimensionScaleGestureDetector detector) {
			super.onScaleEnd(detector);
		}
	}

}