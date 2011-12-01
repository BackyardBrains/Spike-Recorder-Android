package com.backyardbrains.drawing;

import java.text.DecimalFormat;

import com.backyardbrains.BybConfigHolder;
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

	private static final String TAG = OscilloscopeGLSurfaceView.class.getCanonicalName();

	private TwoDimensionScaleGestureDetector mScaleDetector;
	SurfaceHolder mAndroidHolder;
	private OscilloscopeGLThread mGLThread;

	private boolean triggerView;
	private boolean didWeAlreadyAutoscale;
	private float scaleFactor = 1;
	private float bufferLengthDivisor = 8;

	public float getScaleFactor() {
		return scaleFactor;
	}

	public void setScaleFactor(float scaleFactor) {
		this.scaleFactor = scaleFactor;
	}

	public float getBufferLengthDivisor() {
		return bufferLengthDivisor;
	}

	public void setBufferLengthDivisor(float bufferLengthDivisor) {
		this.bufferLengthDivisor = bufferLengthDivisor;
	}

	public OscilloscopeGLThread getGLThread() {
		return mGLThread;
	}

	public OscilloscopeGLSurfaceView(Context context) {
		this(context, null, 0);
	}
	
	public OscilloscopeGLSurfaceView(Context context, boolean isTriggerMode) {
		this(context, null, 0);
		triggerView = isTriggerMode;
		Log.d(TAG, "Starting surface with triggerView set to " + triggerView);
	}
	
	public OscilloscopeGLSurfaceView(Context context, BybConfigHolder bch, boolean isTriggerMode) {
		this(context, isTriggerMode);
		scaleFactor = bch.configScaleFactor;
		bufferLengthDivisor = bch.configBufferLengthDivisor;
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
		this.bufferLengthDivisor = bufferLengthDivisor;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		mScaleDetector.onTouchEvent(event);
		return super.onTouchEvent(event);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		mGLThread.setBufferLengthDivisor(bufferLengthDivisor);
		mGLThread.setmScaleFactor(scaleFactor);
		mGLThread.rescaleWaveform();
		Intent i = new Intent();
		i.setAction("BYBMillivoltsViewSize");
		i.putExtra("millivoltsViewNewSize", height / 2);
		getContext().sendBroadcast(i);
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
		} else return didWeAlreadyAutoscale;
	}
	
	public void setAutoScaled(boolean autoScaled) {
		didWeAlreadyAutoscale = autoScaled;
		if(haveThread()) {
			mGLThread.setAutoScaled(autoScaled);
		}
	}

	private void resetDrawingThread() {
		Log.d(TAG, "Resetting drawing thread with triggerView set to "+ triggerView);
		if (haveThread()) {
			Log.d(TAG, "We have already had a GL Thread, so let's reset its scaling");
			isAutoScaled();
		}
		if (triggerView) {
			mGLThread = new TriggerViewThread(this);
		} else {
			mGLThread = new OscilloscopeGLThread(this);
		}
		synchronized (this) {
			mGLThread.setAutoScaled(didWeAlreadyAutoscale);
			mGLThread.setBufferLengthDivisor(bufferLengthDivisor);
			mGLThread.setmScaleFactor(scaleFactor);
			Log.d(TAG, "Started GL thread with scale of " + scaleFactor
					+ " and bufferLengthDivisor of " + bufferLengthDivisor);
			mGLThread.start();
		}
	}

	public void setMsText(Float ms) {
		String msString = new DecimalFormat("#.#").format(ms);
		broadcastTextUpdate("BYBUpdateMillisecondsReciever", "millisecondsDisplayedString", msString + " ms");
	}

	public void setmVText(Float ms) {
		String msString = new DecimalFormat("#.#").format(ms);
		broadcastTextUpdate("BYBUpdateMillivoltReciever", "millivoltsDisplayedString", msString + " mV");
	}

	private void broadcastTextUpdate(String action, String name, String data) {
		Intent i = new Intent();
		i.setAction(action);
		i.putExtra(name, data);
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

	private class ScaleListener extends Simple2DOnScaleGestureListener {
		@Override
		public boolean onScale(TwoDimensionScaleGestureDetector detector) {
			// float mScaleFactor = mGLThread.getmScaleFactor();
			// float scaleModifier = detector.getScaleFactor();
			try {
				final Pair<Float, Float> scaleModifier = detector
						.getScaleFactor();
				final float scaleModifierX = Math.max(0.95f,
						Math.min(scaleModifier.first, 1.05f));
				final float scaleModifierY = Math.max(0.95f,
						Math.min(scaleModifier.second, 1.05f));
				bufferLengthDivisor *= scaleModifierX;
				scaleFactor *= scaleModifierY;
				Log.d(TAG, "Receiving touch event. scale factor is now "
						+ scaleFactor + "and buffer divisor is "
						+ bufferLengthDivisor);
			} catch (IllegalStateException e) {
				Log.e(TAG, "Got invalid values back from Scale listener!");
			}
			broadcastScaleChange();

			return super.onScale(detector);
		}

		private void broadcastScaleChange() {
			Intent i = new Intent();
			i.setAction("BYBScaleChange");
			i.putExtra("newBufferLengthDivisor", bufferLengthDivisor);
			i.putExtra("newScaleFactor", scaleFactor);
			getContext().sendBroadcast(i);
		}
	}

}