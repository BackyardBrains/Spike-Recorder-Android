package com.backyardbrains.drawing;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

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

	private boolean mDone = false;
	private BybGLDrawable waveformShape;
	private float mScaleFactor = 1.f;
	private float bufferLengthDivisor = 1;
	private float xEnd = 4000f;
	private float yMin = -5000000f;
	private float yBegin = -5000f;
	private float yEnd = 5000f;
	private boolean autoScaled;

	OscilloscopeGLSurfaceView parent;
	private GlSurfaceManager glman;

	private AudioService mAudioService;
	private boolean mAudioServiceIsBound;

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
		glman = new GlSurfaceManager(parent);
		waveformShape = new BybGLDrawable(this);

		bindAudioService(true);
		registerScaleChangeReceiver(true);
		while (!mDone) {
			// grab current audio from audioservice
			if (mAudioServiceIsBound) {

				// Reset our Audio buffer
				ByteBuffer audioInfo = null;

				// Read new mic data
				synchronized (mAudioService) {
					// audioInfo = mAudioService.getCurrentAudioInfo();
					audioInfo = ByteBuffer.wrap(mAudioService.getAudioBuffer());
				}

				if (audioInfo != null) {
					audioInfo.clear();

					// Convert audioInfo to a short[] named mBufferToDraw
					final ShortBuffer audioInfoasShortBuffer = audioInfo
							.asShortBuffer();
					final int bufferCapacity = audioInfoasShortBuffer
							.capacity();

					final short[] mBufferToDraw = new short[bufferCapacity];
					int samplesToShow = Math.round(bufferCapacity
							/ bufferLengthDivisor);

					audioInfoasShortBuffer.get(mBufferToDraw, 0,
							mBufferToDraw.length);
					// scale the right side to the number of data points we have
					setxEnd(mBufferToDraw.length);
					glman.glClear();

					synchronized (parent) {
						final float millisecondsInThisWindow = samplesToShow / 44100.0f * 1000;
						((OscilloscopeGLSurfaceView) parent)
								.setMsText(millisecondsInThisWindow);
						float yPerDiv = (float) ((yEnd / mScaleFactor - yBegin
								/ mScaleFactor) / (4.0f * 24.5));
						((OscilloscopeGLSurfaceView) parent).setmVText(yPerDiv);
					}

					waveformShape.setBufferToDraw(mBufferToDraw);
					glman.initGL(xEnd - samplesToShow, xEnd, yBegin
							/ mScaleFactor, yEnd / mScaleFactor);
					waveformShape.draw(glman.getmGL());

					if (isDrawThresholdLine()) {
						drawThresholdLine();
					}
					glman.swapBuffers();
				}
				try {
					sleep(5);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		bindAudioService(false);
		registerScaleChangeReceiver(false);
		mConnection = null;
	}

	private void drawThresholdLine() {
		float thresholdValue = (yEnd / 2 / mScaleFactor);
		float[] thresholdLine = new float[4];
		thresholdLine[0] = 0;
		thresholdLine[2] = getxEnd();
		thresholdLine[1] = thresholdValue;
		thresholdLine[3] = thresholdValue;
		FloatBuffer thl = getFloatBufferFromFloatArray(thresholdLine);
		glman.getmGL().glEnableClientState(GL10.GL_VERTEX_ARRAY);
		glman.getmGL().glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
		glman.getmGL().glLineWidth(1.0f);
		glman.getmGL().glVertexPointer(2, GL10.GL_FLOAT, 0, thl);
		glman.getmGL().glDrawArrays(GL10.GL_LINES, 0, 4);
		glman.getmGL().glDisableClientState(GL10.GL_VERTEX_ARRAY);
	}

	private void registerScaleChangeReceiver(boolean on) {
		if (on) {
			scaleChangeReceiver = new ScaleChangeReceiver();
			parent.getContext().registerReceiver(scaleChangeReceiver,
					new IntentFilter("BYBScaleChange"));
		} else {

			parent.getContext().unregisterReceiver(scaleChangeReceiver);
		}

	}

	private void bindAudioService(boolean on) {
		if (on) {
			final Intent intent = new Intent(parent.getContext(),
					AudioService.class);
			parent.getContext().getApplicationContext()
					.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		} else {
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

	/*
	 * ***************** Getters and setters and detritus *****************
	 */

	public boolean isDrawThresholdLine() {
		return false;
	}

	public float getxEnd() {
		return xEnd;
	}

	public void setxEnd(float xEnd) {
		this.xEnd = xEnd;
	}

	public float getyMin() {
		return yMin;
	}

	public void setyBegin(float yBegin) {
		this.yBegin = yBegin;
	}

	public void setyEnd(float yEnd) {
		this.yEnd = yEnd;
	}

	public float getmScaleFactor() {
		return mScaleFactor;
	}

	public void setmScaleFactor(float mScaleFactor) {
		// Don't let the object get too small or too large.
		mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 5.0f));

		this.mScaleFactor = mScaleFactor;
	}

	public float getBufferLengthDivisor() {
		return bufferLengthDivisor;
	}

	public void setBufferLengthDivisor(float bufferLengthDivisor) {
		if (bufferLengthDivisor >= 1 && bufferLengthDivisor <= 64) {
			this.bufferLengthDivisor = bufferLengthDivisor;
		}
	}

	public boolean isAutoScaled() {
		return autoScaled;
	}

	public void setAutoScaled(boolean autoScaled) {
		this.autoScaled = autoScaled;
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
			setmScaleFactor(intent.getFloatExtra("newScaleFactor", 1));

			float localBufferLengthDivisor = intent.getFloatExtra(
					"newBufferLengthDivisor", 1);
			setBufferLengthDivisor(localBufferLengthDivisor);
			Log.d(TAG, "Setting ScaleFactor to " + mScaleFactor
					+ " - bufferLengthDivisor to " + bufferLengthDivisor);
		};
	}

}
