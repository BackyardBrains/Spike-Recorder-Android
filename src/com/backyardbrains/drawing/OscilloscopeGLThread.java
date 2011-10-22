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
import android.view.SurfaceView;

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

	private float xBegin = 00f;
	private float xEnd = 4000f;

	public void setxEnd(float xEnd) {
		this.xEnd = xEnd;
	}

	private float yMin = -5000000f;
	private float yMax = 5000000f;

	public float getyMin() {
		return yMin;
	}

	private float yBegin = -5000f;
	private float yEnd = 5000f;

	public void setyBegin(float yBegin) {
		this.yBegin = yBegin;
	}

	public void setyEnd(float yEnd) {
		this.yEnd = yEnd;
	}

	/**
	 * reference to parent {@link OscilloscopeGLSurfaceView}
	 */
	SurfaceView parent;

	/**
	 * Is thread done processing yet? Used at requestStop
	 */
	private boolean mDone = false;
	private static final String TAG = "BYBOsciliscopeGlThread";
	private BybGLDrawable waveformShape;
	private float mScaleFactor = 1.f;

	public float getmScaleFactor() {
		return mScaleFactor;
	}

	public void setmScaleFactor(float mScaleFactor) {
		// Don't let the object get too small or too large.
		mScaleFactor = Math.max(0.01f, Math.min(mScaleFactor, 3.0f));

		this.mScaleFactor = mScaleFactor;
	}

	private AudioService mAudioService;
	private boolean mAudioServiceIsBound;

	/**
	 * @param view
	 *            reference to the parent view
	 */
	OscilloscopeGLThread(SurfaceView view) {
		parent = view;
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
		 * @TODO null out mAudioService
		 * 
		 * @see android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
		 */
		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mAudioServiceIsBound = false;
			mAudioService = null;
		}
	};
	private GlSurfaceManager glman;
	private float bufferLengthDivisor = 1;

	/**
	 * @return the bufferLengthDivisor
	 */
	public float getBufferLengthDivisor() {
		return bufferLengthDivisor;
	}

	/**
	 * @param bufferLengthDivisor
	 *            the bufferLengthDivisor to set
	 */
	public void setBufferLengthDivisor(float bufferLengthDivisor) {
		if (bufferLengthDivisor >= 1 && bufferLengthDivisor <= 16) {
			this.bufferLengthDivisor = bufferLengthDivisor;
		}
	}

	private class ScaleChangeReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(android.content.Context context,
				android.content.Intent intent) {
			mScaleFactor = intent.getFloatExtra("newScaleFactor", 1);

			bufferLengthDivisor = intent.getFloatExtra(
					"newBufferLengthDivisor", 1);

			Log.d(TAG, "Setting ScaleFactor to " + mScaleFactor
					+ " - bufferLengthDivisor to " + bufferLengthDivisor);
		};
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

		final Intent intent = new Intent(parent.getContext(),
				AudioService.class);
		parent.getContext().getApplicationContext()
				.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

		final ScaleChangeReceiver scaleChangeReceiver = new ScaleChangeReceiver();
		parent.getContext().registerReceiver(scaleChangeReceiver,
				new IntentFilter("BYBScaleChange"));
		bufferLengthDivisor = 1;
		while (!mDone) {
			// grab current audio from audioservice
			if (mAudioServiceIsBound) {

				// Reset our Audio buffer
				ByteBuffer audioInfo = null;

				// Read new mic data
				synchronized (this) {
					audioInfo = mAudioService.getCurrentAudioInfo();
				}

				if (audioInfo != null) {
					audioInfo.clear();

					// Convert audioInfo to a short[] named mBufferToDraw
					final ShortBuffer audioInfoasShortBuffer = audioInfo
							.asShortBuffer();
					final int bufferCapacity = audioInfoasShortBuffer
							.capacity();

					final int samplesToSend = bufferCapacity
							/ (int) bufferLengthDivisor;

					for (int i = 0; i + samplesToSend <= bufferCapacity; i += samplesToSend) {

						final short[] mBufferToDraw = new short[samplesToSend];
						final long currentTime = System.currentTimeMillis();
						final float millisecondsInThisBuffer = mBufferToDraw.length / 44100.0f * 1000;

						audioInfoasShortBuffer.get(mBufferToDraw, 0,
								mBufferToDraw.length);
						// scale the right side to the number of data points we
						// have
						setxEnd(mBufferToDraw.length / 2);
						glman.glClear();

						synchronized (parent) {
							((OscilloscopeGLSurfaceView) parent)
									.setMsText(millisecondsInThisBuffer);
						}
						waveformShape.setBufferToDraw(mBufferToDraw);
						glman.initGL(xBegin, xEnd, yBegin / mScaleFactor, yEnd
								/ mScaleFactor);
						waveformShape.draw(glman.getmGL());
						glman.swapBuffers();
						long newTime = System.currentTimeMillis();
						while (newTime - currentTime < millisecondsInThisBuffer) {
							try {
								Thread.sleep(1);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							newTime = System.currentTimeMillis();
						}
					}
				}

			}
		}
		parent.getContext().getApplicationContext().unbindService(mConnection);
		parent.getContext().unregisterReceiver(scaleChangeReceiver);
		mConnection = null;
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
}
