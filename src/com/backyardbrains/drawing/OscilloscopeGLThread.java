package com.backyardbrains.drawing;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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
	protected int glWindowHorizontalSize = 4000;
	private float minimumDetectedPCMValue = -5000000f;
	private int glWindowVerticalSize = 10000;
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
		preRunLoopHandler();
		while (!mDone) {
			// grab current audio from audioservice
			if (!isServiceReady()) continue;

			getCurrentAudio();

			if (!isValidAudioBuffer()) {
				noValidBufferFallback();
				continue;
			}

			preDrawingHandler();
			glman.glClear();
			drawingHandler();
			glman.swapBuffers();
			try {
				sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		postRunLoopHandler();
	}

	protected void postRunLoopHandler() {
		bindAudioService(false);
		mConnection = null;
	}

	protected void preRunLoopHandler() {
		bindAudioService(true);
		setupSurfaceAndDrawable();
	}
	
	protected void postDrawingHandler() {
		// stub
	}

	private void drawingHandler() {
		waveformShape.setBufferToDraw(mBufferToDraws);
		setGlWindow(glWindowHorizontalSize, mBufferToDraws.length);
		waveformShape.draw(glman.getmGL());
	}

	protected void preDrawingHandler() {
		// scale the right side to the number of data points we have
		if (mBufferToDraws.length < glWindowHorizontalSize) {
			setGlWindowHorizontalSize(mBufferToDraws.length);
		}

		synchronized (parent) {
			setLabels(glWindowHorizontalSize);
		}
	}
	
	protected void noValidBufferFallback() {
		// this is a stub for derivative classes to override
		// and should contain the default activity for 
	}

	protected void getCurrentAudio () {
		synchronized (mAudioService) {
			mBufferToDraws = mAudioService.getAudioBuffer();
		}
	}

	protected boolean isValidAudioBuffer() {
		return mBufferToDraws != null && mBufferToDraws.length > 0;
	}

	protected boolean isServiceReady() {
		return mAudioServiceIsBound && mAudioService != null;
	}

	protected void setupSurfaceAndDrawable() {
		glman = new GlSurfaceManager(parent);
		waveformShape = new BybGLDrawable(this);
	}

	protected void setGlWindow(final int samplesToShow, final int lengthOfSampleSet) {
		glman.initGL(lengthOfSampleSet - glWindowHorizontalSize, lengthOfSampleSet, -getGlWindowVerticalSize()/2, getGlWindowVerticalSize()/2);
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
			float yPerDiv = (float) getGlWindowVerticalSize() / 4.0f / 24.5f /1000;
			parent.setmVText(yPerDiv);
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

	public int getGlWindowHorizontalSize() {
		return glWindowHorizontalSize;
	}

	public void setGlWindowHorizontalSize (final int newSize) {
		int maxlength = 0;
		if (mBufferToDraws != null)
			maxlength = mBufferToDraws.length;
		if (newSize < 16 || (maxlength > 0 && newSize > maxlength)) return;
		this.glWindowHorizontalSize = newSize;
	}

	public float getMinimumDetectedPCMValue() {
		return minimumDetectedPCMValue;
	}

	public int getGlWindowVerticalSize() {
		return glWindowVerticalSize;
	}
	
	public void setGlWindowVerticalSize(int y) {
		if (y < 800 || y > PCM_MAXIMUM_VALUE * 2)
			return;
		
		glWindowVerticalSize = y;
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

	public float getThresholdYValue() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void adjustThresholdValue(float dy) {
		// TODO Auto-generated method stub
	}

	protected void drawThresholdLine () {
		// stub
	}

}
