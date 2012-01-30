package com.backyardbrains.drawing;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

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
	private static final String TAG = OscilloscopeGLThread.class
			.getCanonicalName();
	public static final int PCM_MAXIMUM_VALUE = (Short.MAX_VALUE * 3 / 2);

	protected boolean mDone = false;
	protected BybGLDrawable waveformShape;
	//protected float zoomMultiplier = 1.f;
	protected float bufferLengthDivisor = 1;
	protected int xEnd = 4000;
	private float minimumDetectedPCMValue = -5000000f;
	protected int yBegin = -5000;
	protected int yEnd = 5000;
	private boolean autoScaled;

	protected short[] mBufferToDraws;

	OscilloscopeGLSurfaceView parent;
	protected GlSurfaceManager glman;

	protected AudioService mAudioService;
	protected boolean mAudioServiceIsBound;

	OscilloscopeGLThread(OscilloscopeGLSurfaceView view) {
		parent = view;
	}

	/**
	 * Initialize GL bits, set up the GL area so that we're lookin at it
	 * properly, create a new {@link BybGLDrawable}, then commence drawing on it
	 * like the dickens.
	 * 
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		setupSurfaceAndDrawable();

		bindAudioService(true);
		registerScaleChangeReceiver(true);
		while (!mDone) {
			// grab current audio from audioservice
			if (mAudioServiceIsBound && mAudioService != null) {

				// Reset our Audio buffer
				ByteBuffer audioInfo = null;

				// Read new mic data
				synchronized (mAudioService) {
					audioInfo = ByteBuffer.wrap(mAudioService.getAudioBuffer());
				}

				if (audioInfo != null) {
					audioInfo.clear();

					// Convert audioInfo to a short[] named mBufferToDraw
					final ShortBuffer audioInfoasShortBuffer = audioInfo
							.asShortBuffer();
					final int bufferCapacity = audioInfoasShortBuffer
							.capacity();

					mBufferToDraws = convertToShortArray(
							audioInfoasShortBuffer, bufferCapacity);
					// scale the right side to the number of data points we have
					if (mBufferToDraws.length < xEnd) {
						setxEnd(mBufferToDraws.length);
					}

					synchronized (parent) {
						setLabels(xEnd);
					}

					// glman.glClear();
					waveformShape.setBufferToDraw(mBufferToDraws);
					setGlWindow(xEnd, mBufferToDraws.length);
					waveformShape.draw(glman.getmGL());
					glman.swapBuffers();
				}
				/*
				try {
					sleep(5);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				*/
			}
		}
		bindAudioService(false);
		registerScaleChangeReceiver(false);
		mConnection = null;
	}

	protected void setupSurfaceAndDrawable() {
		glman = new GlSurfaceManager(parent);
		waveformShape = new BybGLDrawable(this);
	}

	protected void setGlWindow(final int samplesToShow, final int lengthOfSampleSet) {
		glman.initGL(lengthOfSampleSet - xEnd, lengthOfSampleSet, yBegin, yEnd);
	}

	protected short[] convertToShortArray(final ShortBuffer shortBuffer,
			final int bufferCapacity) {
		final short[] mBufferToDraw = new short[bufferCapacity];
		shortBuffer.get(mBufferToDraw, 0, mBufferToDraw.length);
		return mBufferToDraw;
	}

	/**
	 * Set the labels that are hovering over the surface in the main activity
	 * for how many millivolts/milliseconds we're displaying
	 * 
	 * @param samplesToShow
	 */
	protected void setLabels(int samplesToShow) {
		final float millisecondsInThisWindow = samplesToShow / 44100.0f * 1000 / 3;
		parent.setMsText(millisecondsInThisWindow);
		if (!isDrawThresholdLine()) {
			float yPerDiv = (float) (yEnd - yBegin) / 4.0f / 24.5f /1000;
			parent.setmVText(yPerDiv);
		}
	}

	protected void registerScaleChangeReceiver(boolean on) {
		if (on) {
			scaleChangeReceiver = new ScaleChangeReceiver();
			parent.getContext().registerReceiver(scaleChangeReceiver,
					new IntentFilter("BYBScaleChange"));
		} else {

			parent.getContext().unregisterReceiver(scaleChangeReceiver);
		}

	}

	protected void bindAudioService(boolean on) {
		if (on) {
			Log.d(TAG, "Binding audio service.");
			Intent intent = new Intent(parent.getContext(),
					AudioService.class);
			parent.getContext().getApplicationContext()
					.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		} else {
			Log.d(TAG, "UnBinding audio service.");
			parent.getContext().getApplicationContext()
					.unbindService(mConnection);
		}
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
	FloatBuffer getFloatBufferFromFloatArray(final float[] array) {
		final ByteBuffer temp = ByteBuffer.allocateDirect(array.length * 4);
		temp.order(ByteOrder.nativeOrder());
		final FloatBuffer buf = temp.asFloatBuffer();
		buf.put(array);
		buf.position(0);
		return buf;
	}

	public void rescaleWaveform() {
		if (waveformShape != null)
			waveformShape.forceRescale();
	}

	/*
	 * ***************** Getters and setters and detritus *****************
	 */

	public boolean isDrawThresholdLine() {
		return false;
	}

	public int getxEnd() {
		return xEnd;
	}

	public void setxEnd(int xEnd) {
		if (xEnd < 16 || mBufferToDraws == null || xEnd > mBufferToDraws.length) return;
		this.xEnd = xEnd;
	}

	public float getMinimumDetectedPCMValue() {
		return minimumDetectedPCMValue;
	}

	public void setyBegin(int yBegin) {
		if (yBegin < -PCM_MAXIMUM_VALUE || yBegin > -400) return;
		this.yBegin = yBegin;
	}

	public int getyBegin() {
		return yBegin;
	}

	public int getyEnd() {
		return yEnd;
	}

	public void setyEnd(int yEnd) {
		if (yEnd > PCM_MAXIMUM_VALUE || yEnd < 400) return;
		this.yEnd = yEnd;
	}

	public boolean isAutoScaled() {
		return autoScaled;
	}

	public void setAutoScaled(boolean autoScaled) {
		this.autoScaled = autoScaled;
	}

	protected ServiceConnection mConnection = new ServiceConnection() {

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
		 * @see android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
		 */
		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mAudioServiceIsBound = false;
			mAudioService = null;
		}
	};

	private ScaleChangeReceiver scaleChangeReceiver;

	private class ScaleChangeReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(android.content.Context context,
				android.content.Intent intent) {
			//setmScaleFactor(intent.getFloatExtra("newScaleFactor", 1));

			/*
			float localBufferLengthDivisor = intent.getFloatExtra(
					"newBufferLengthDivisor", 1);
			setBufferLengthDivisor(localBufferLengthDivisor);
			*/
		};
	}

	public float getThresholdYValue() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void adjustThresholdValue(float dy) {
		// TODO Auto-generated method stub
		
	}

}
