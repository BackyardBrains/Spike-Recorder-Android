package com.backyardbrains.drawing;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.text.DecimalFormat;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.opengl.GLSurfaceView;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import com.backyardbrains.audio.AudioService;
import com.backyardbrains.audio.AudioService.AudioServiceBinder;

public class OscilloscopeRenderer implements GLSurfaceView.Renderer {

	private static final String TAG = OscilloscopeRenderer.class
			.getCanonicalName();
	public static final int PCM_MAXIMUM_VALUE = (Short.MAX_VALUE * 3 / 2);

	protected AudioService mAudioService;
	protected int glWindowHorizontalSize = 4000;
	private float minimumDetectedPCMValue = -5000000f;
	protected int glWindowVerticalSize = 10000;
	private boolean autoScaled = false;
	protected Activity context;
	protected short[] mBufferToDraws;
	protected boolean mAudioServiceIsBound;
	protected int height;
	protected int width;
	private long firstBufferDrawn = 0;

	public OscilloscopeRenderer(Activity backyardAndroidActivity) {
		context = backyardAndroidActivity;
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		// grab current audio from audioservice
		if (!isServiceReady())
			return;

		getCurrentAudio();

		if (!isValidAudioBuffer())
			return;

		preDrawingHandler();
		glClear(gl);
		drawingHandler(gl);
		postDrawingHandler(gl);
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		this.width = width;
		this.height = height;
		setMillivoltLabelPosition(height);
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {

		bindAudioService(true);
		gl.glDisable(GL10.GL_DITHER);
		gl.glHint(GL10.GL_LINE_SMOOTH_HINT, GL10.GL_NICEST);
		gl.glEnable(GL10.GL_DEPTH_TEST);
	}
	
	void cleanUp() {
		bindAudioService(false);
	}

	private void glClear(GL10 gl) {
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
	}

	protected void preDrawingHandler() {
		// scale the right side to the number of data points we have
		if (mBufferToDraws.length < glWindowHorizontalSize) {
			setGlWindowHorizontalSize(mBufferToDraws.length);
		}
		setLabels(glWindowHorizontalSize);
	}

	protected void postDrawingHandler(GL10 gl) {
		// stub
	}

	private void drawingHandler(GL10 gl) {
		setGlWindow(gl, glWindowHorizontalSize, mBufferToDraws.length);
		FloatBuffer mVertexBuffer = getWaveformBuffer(mBufferToDraws);

		firstBufferDrawnCheck();
		autoScaleCheck();

		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();

		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glLineWidth(1f);
		gl.glColor4f(0f, 1f, 0f, 1f);
		gl.glVertexPointer(2, GL10.GL_FLOAT, 0, mVertexBuffer);
		gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, mVertexBuffer.limit() / 2);
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
	}

	private void firstBufferDrawnCheck() {
		if (firstBufferDrawn == 0) {
			firstBufferDrawn = SystemClock.currentThreadTimeMillis();
		}
	}

	private void autoScaleCheck() {
		if (!isAutoScaled()
				&& firstBufferDrawn != 0
				&& (SystemClock.currentThreadTimeMillis() - firstBufferDrawn) > 100) {
			autoSetFrame(mBufferToDraws);
		}
	}

	private void autoSetFrame(short[] arrayToScaleTo) {
		int theMax = 0;
		int theMin = 0;

		for (int i = 0; i < arrayToScaleTo.length; i++) {
			if (theMax < arrayToScaleTo[i])
				theMax = arrayToScaleTo[i];
			if (theMin > arrayToScaleTo[i])
				theMin = arrayToScaleTo[i];
		}

		int newyMax;
		if (theMax != 0 && theMin != 0) {

			if (Math.abs(theMax) >= Math.abs(theMin)) {
				newyMax = Math.abs(theMax) * 2;
			} else {
				newyMax = Math.abs(theMin) * 2;
			}
			if (-newyMax > getMinimumDetectedPCMValue()) {
				Log.d(TAG, "Scaling window to " + -newyMax + " < y < "
						+ newyMax);
				setGlWindowVerticalSize(newyMax * 2);
			}

		}
		setAutoScaled(true);
	}

	protected FloatBuffer getWaveformBuffer(short[] shortArrayToDraw) {
		int micSize = mAudioService.getMicListenerBufferSizeInSamples();
		float[] arr = new float[(glWindowHorizontalSize + micSize) * 2];
		int j = 0; // index of arr
		try {
			for (int i = shortArrayToDraw.length - glWindowHorizontalSize
					- micSize; i < shortArrayToDraw.length; i++) {
				arr[j++] = i;
				arr[j++] = shortArrayToDraw[i];
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			Log.e(TAG, e.getMessage());
		}
		return getFloatBufferFromFloatArray(arr);
	}

	FloatBuffer getFloatBufferFromFloatArray(final float[] array) {
		final ByteBuffer temp = ByteBuffer.allocateDirect(array.length * 4);
		temp.order(ByteOrder.nativeOrder());
		final FloatBuffer buf = temp.asFloatBuffer();
		buf.put(array);
		buf.position(0);
		return buf;
	}

	protected boolean isValidAudioBuffer() {
		return mBufferToDraws != null && mBufferToDraws.length > 0;
	}

	protected boolean isServiceReady() {
		return mAudioServiceIsBound && mAudioService != null;
	}

	protected void setLabels(int samplesToShow) {
		setmVText();
		final float millisecondsInThisWindow = samplesToShow / 44100.0f * 1000 / 3;
		setMsText(millisecondsInThisWindow);
	}

	protected void setmVText() {
		float yPerDiv = (float) getGlWindowVerticalSize() / 4.0f / 24.5f / 1000;
		setmVText(yPerDiv);
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
		context.sendBroadcast(i);
	}
	
	private void broadcastTextUpdate(String action, String name, String data) {
		Intent i = new Intent();
		i.setAction(action);
		i.putExtra(name, data);
		context.sendBroadcast(i);
	}

	/*
	private long samplesToMs(long samps) {
		return Math.round(samps / 44.1);
	}
	*/

	private long msToSamples(long timeSince) {
		return Math.round(44.1 * timeSince);
	}

	protected void setGlWindow(GL10 gl, final int samplesToShow,
			final int lengthOfSampleSet) {
		
		final int micBufferSize = mAudioService.getMicListenerBufferSizeInSamples();
		final long lastTimestamp = mAudioService.getLastSamplesReceivedTimestamp();
		final long timeSince = System.currentTimeMillis() - lastTimestamp;
		
		long xEnd = lengthOfSampleSet - micBufferSize + msToSamples(timeSince);
		long xBegin = xEnd - glWindowHorizontalSize;
		initGL(gl, xBegin,
				xEnd, -getGlWindowVerticalSize() / 2,
				getGlWindowVerticalSize() / 2);
	}

	void initGL(GL10 gl, float xBegin, float xEnd, float scaledYBegin,
			float scaledYEnd) {
		// set viewport
		gl.glViewport(0, 0, width, height);

		glClear(gl);
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrthof(xBegin, xEnd, scaledYBegin, scaledYEnd, -1f, 1f);
		gl.glRotatef(0f, 0f, 0f, 1f);

		// Blackout, then we're ready to draw! \o/
		// mGL.glEnable(GL10.GL_TEXTURE_2D);
		gl.glClearColor(0f, 0f, 0f, 0.5f);
		gl.glClearDepthf(1.0f);
		gl.glEnable(GL10.GL_DEPTH_TEST);
		gl.glDepthFunc(GL10.GL_LEQUAL);
		gl.glEnable(GL10.GL_LINE_SMOOTH);
		gl.glHint(GL10.GL_LINE_SMOOTH_HINT, GL10.GL_NICEST);
		// Enable Blending
		gl.glEnable(GL10.GL_BLEND);
		// Specifies pixel arithmetic
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
	}

	public void setGlWindowHorizontalSize(final int newSize) {
		int maxlength = 0;
		if (mBufferToDraws != null)
			maxlength = mBufferToDraws.length;
		if (newSize < 16 || (maxlength > 0 && newSize > maxlength))
			return;
		this.glWindowHorizontalSize = newSize;
	}

	public void setGlWindowVerticalSize(int y) {
		if (y < 800 || y > PCM_MAXIMUM_VALUE * 2)
			return;

		glWindowVerticalSize = y;
	}

	public float getMinimumDetectedPCMValue() {
		return minimumDetectedPCMValue;
	}

	public int getGlWindowVerticalSize() {
		return glWindowVerticalSize;
	}

	public int getGlWindowHorizontalSize() {
		return glWindowHorizontalSize;
	}

	protected void getCurrentAudio() {
		synchronized (mAudioService) {
			mBufferToDraws = mAudioService.getAudioBuffer();
		}
	}

	public boolean isAutoScaled() {
		return autoScaled;
	}

	public void setAutoScaled(boolean isScaled) {
		autoScaled = isScaled;
	}

	protected void bindAudioService(boolean on) {
		if (on) {
			// Log.d(TAG, "Binding audio service.");
			Intent intent = new Intent(context, AudioService.class);
			context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		} else {
			// Log.d(TAG, "UnBinding audio service.");
			context.unbindService(mConnection);
		}
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
			mAudioService = null;
			mAudioServiceIsBound = false;
		}
	};

}
