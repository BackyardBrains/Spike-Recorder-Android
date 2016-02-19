package com.backyardbrains.drawing;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.text.DecimalFormat;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import com.backyardbrains.BackyardBrainsApplication;
import com.backyardbrains.BackyardBrainsMain;
import com.backyardbrains.audio.AudioService;

import android.content.Context;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.SystemClock;
import android.util.Log;

public class BYBBaseRenderer implements GLSurfaceView.Renderer {
	protected int				glWindowHorizontalSize	= 4000;
	protected int				glWindowVerticalSize	= 10000;
	private static final String	TAG						= BYBBaseRenderer.class.getCanonicalName();
//	protected AudioService		audioService			= null;
	protected short[]			mBufferToDraws;
	protected boolean			mAudioServiceIsBound;
	protected int				height;
	protected int				width;
	protected boolean			autoScaled				= false;
	public static final int		PCM_MAXIMUM_VALUE		= (Short.MAX_VALUE * 3 / 2);
	protected float				minimumDetectedPCMValue	= -5000000f;

	protected long				firstBufferDrawn		= 0;
	protected Context context;
	// ----------------------------------------------------------------------------------------
	public BYBBaseRenderer(Context context){//, AudioService audioService) {
//		this.audioService = audioService;
		this.context = context.getApplicationContext();
	}
	// ----------------------------------------------------------------------------------------
	public void close(){}
	// ----------------------------------------------------------------------------------------
	public void setGlWindowHorizontalSize(final int newX) {
		int maxlength = 0;
		if (mBufferToDraws != null)
			maxlength = mBufferToDraws.length;
		if (newX < 16 || (maxlength > 0 && newX> maxlength))
			return;
		this.glWindowHorizontalSize = newX;
	}

	// ----------------------------------------------------------------------------------------
	public void setGlWindowVerticalSize(int newY) {
		if (newY < 800 || newY > PCM_MAXIMUM_VALUE * 2)
			return;
		glWindowVerticalSize = newY;
	}

	// ----------------------------------------------------------------------------------------
	public int getGlWindowVerticalSize() {
		return glWindowVerticalSize;
	}

	// ----------------------------------------------------------------------------------------
	public int getGlWindowHorizontalSize() {
		return glWindowHorizontalSize;
	}

	// ----------------------------------------------------------------------------------------
	protected FloatBuffer getWaveformBuffer(short[] shortArrayToDraw) {
		float[] arr;
		if (((BackyardBrainsApplication) context).getmAudioService() != null) {
			int micSize = ((BackyardBrainsApplication) context).getmAudioService().getMicListenerBufferSizeInSamples();
			arr = new float[(glWindowHorizontalSize + micSize) * 2];
			int j = 0; // index of arr
			try {
				for (int i = shortArrayToDraw.length - glWindowHorizontalSize - micSize; i < shortArrayToDraw.length; i++) {
					arr[j++] = i;
					arr[j++] = shortArrayToDraw[i];
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				Log.e(TAG, "Array size out of sync while building new waveform buffer");
			}
		} else {
			arr = new float[0];
		}
		return getFloatBufferFromFloatArray(arr);
	}

	// ----------------------------------------------------------------------------------------
	FloatBuffer getFloatBufferFromFloatArray(final float[] array) {
		final FloatBuffer buf ;
		final ByteBuffer temp = ByteBuffer.allocateDirect(array.length * 4);
		temp.order(ByteOrder.nativeOrder());
		buf= temp.asFloatBuffer();
		buf.put(array);
		buf.position(0);
		return buf;
	}

	// ----------------------------------------------------------------------------------------
	protected void firstBufferDrawnCheck() {
		if (firstBufferDrawn == 0) {
			firstBufferDrawn = SystemClock.currentThreadTimeMillis();
		}
	}

	// ----------------------------------------------------------------------------------------
	public float getMinimumDetectedPCMValue() {
		return minimumDetectedPCMValue;
	}

	// ----------------------------------------------------------------------------------------
	protected boolean isValidAudioBuffer() {
		return mBufferToDraws != null && mBufferToDraws.length > 0;
	}

	// ----------------------------------------------------------------------------------------
	protected long msToSamples(long timeSince) {
		return Math.round(44.1 * timeSince);
	}

	// ----------------------------------------------------------------------------------------
	protected boolean getCurrentAudio() {
		if (((BackyardBrainsApplication) context).getmAudioService() != null) {
			mBufferToDraws = ((BackyardBrainsApplication) context).getmAudioService().getAudioBuffer();
			return true;
		}
		return false;
	}

	// ----------------------------------------------------------------------------------------
	public boolean isAutoScaled() {
		return autoScaled;
	}

	// ----------------------------------------------------------------------------------------
	public void setAutoScaled(boolean isScaled) {
		autoScaled = isScaled;
	}

	// ----------------------------------------------------------------------------------------
	@Override
	public void onDrawFrame(GL10 gl) {
		// Log.d(TAG, "onDrawFrame");
		// grab current audio from audioservice
		if (!getCurrentAudio()) {
			Log.d(TAG, "AudioService is null!");
			return;
		}
		if (!isValidAudioBuffer()) {
			Log.d(TAG, "Invalid audio buffer!");
			return;
		}
		preDrawingHandler();
		glClear(gl);
		drawingHandler(gl);
		postDrawingHandler(gl);
	}

	// ----------------------------------------------------------------------------------------
	protected void preDrawingHandler() {
		// scale the right side to the number of data points we have
		if (mBufferToDraws.length < glWindowHorizontalSize) {
			setGlWindowHorizontalSize(mBufferToDraws.length);
		}
		setLabels(glWindowHorizontalSize);
	}

	// ----------------------------------------------------------------------------------------
	protected void setLabels(int samplesToShow) {
		setmVText();
		final float millisecondsInThisWindow = samplesToShow / 44100.0f * 1000 / 3;
		setMsText(millisecondsInThisWindow);
	}

	// ----------------------------------------------------------------------------------------
	protected void setmVText() {
		float yPerDiv = (float) getGlWindowVerticalSize() / 4.0f / 24.5f / 1000;
		setmVText(yPerDiv);
	}

	// ----------------------------------------------------------------------------------------
	public void setMsText(Float ms) {
		String msString = new DecimalFormat("#.#").format(ms);
		broadcastTextUpdate("BYBUpdateMillisecondsReciever", "millisecondsDisplayedString", msString + " ms");
	}

	// ----------------------------------------------------------------------------------------
	public void setmVText(Float ms) {
		String msString = new DecimalFormat("#.##").format(ms);
		broadcastTextUpdate("BYBUpdateMillivoltReciever", "millivoltsDisplayedString", msString + " mV");
	}

	// ----------------------------------------------------------------------------------------
	private void setMillivoltLabelPosition(int height) {
		Intent i = new Intent();
		i.setAction("BYBMillivoltsViewSize");
		i.putExtra("millivoltsViewNewSize", height / 2);
		context.sendBroadcast(i);
	}

	// ----------------------------------------------------------------------------------------
	private void broadcastTextUpdate(String action, String name, String data) {
		Intent i = new Intent();
		i.setAction(action);
		i.putExtra(name, data);
		context.sendBroadcast(i);
	}

	// ----------------------------------------------------------------------------------------
	protected void postDrawingHandler(GL10 gl) {
	}

	// ----------------------------------------------------------------------------------------
	protected void drawingHandler(GL10 gl) {
	}

	// ----------------------------------------------------------------------------------------
	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		this.width = width;
		this.height = height;
		setMillivoltLabelPosition(height);
	}

	// ----------------------------------------------------------------------------------------
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		gl.glDisable(GL10.GL_DITHER);
		gl.glHint(GL10.GL_LINE_SMOOTH_HINT, GL10.GL_NICEST);
		gl.glEnable(GL10.GL_DEPTH_TEST);
	}

	// ----------------------------------------------------------------------------------------
	protected void initGL(GL10 gl, float xBegin, float xEnd, float scaledYBegin, float scaledYEnd) {
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
		// gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
	}

	// ----------------------------------------------------------------------------------------
	protected void glClear(GL10 gl) {
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
	}

	// ----------------------------------------------------------------------------------------
	protected void autoScaleCheck() {
		if (!isAutoScaled() && firstBufferDrawn != 0 && (SystemClock.currentThreadTimeMillis() - firstBufferDrawn) > 100) {
			autoSetFrame(mBufferToDraws);
		}
	}

	// ----------------------------------------------------------------------------------------
	protected void autoSetFrame(short[] arrayToScaleTo) {
		int theMax = 0;
		int theMin = 0;

		for (int i = 0; i < arrayToScaleTo.length; i++) {
			if (theMax < arrayToScaleTo[i]) theMax = arrayToScaleTo[i];
			if (theMin > arrayToScaleTo[i]) theMin = arrayToScaleTo[i];
		}

		int newyMax;
		if (theMax != 0 && theMin != 0) {

			if (Math.abs(theMax) >= Math.abs(theMin)) {
				newyMax = Math.abs(theMax) * 2;
			} else {
				newyMax = Math.abs(theMin) * 2;
			}
			if (-newyMax > getMinimumDetectedPCMValue()) {
				Log.d(TAG, "Scaling window to " + -newyMax + " < y < " + newyMax);
				setGlWindowVerticalSize(newyMax * 2);
			}

		}
		setAutoScaled(true);
	}

	// ----------------------------------------------------------------------------------------
	protected void setGlWindow(GL10 gl, final int samplesToShow, final int lengthOfSampleSet) {
		if (((BackyardBrainsApplication) context).getmAudioService() != null) {
		final int micBufferSize = ((BackyardBrainsApplication) context).getmAudioService().getMicListenerBufferSizeInSamples();
		final long lastTimestamp = ((BackyardBrainsApplication) context).getmAudioService().getLastSamplesReceivedTimestamp();
		final long timeSince = System.currentTimeMillis() - lastTimestamp;

		long xEnd = Math.min(lengthOfSampleSet, lengthOfSampleSet - micBufferSize + msToSamples(timeSince));
		long xBegin = Math.min(lengthOfSampleSet - glWindowHorizontalSize, xEnd - glWindowHorizontalSize);
		xBegin = Math.max(0, xBegin);
		initGL(gl, xBegin, xEnd, -getGlWindowVerticalSize() / 2, getGlWindowVerticalSize() / 2);
		}
	}

	// ----------------------------------------------------------------------------------------
	int map(float glHeight, int in_min, int in_max, int out_min, int out_max) {
		return (int) ((glHeight - in_min) * (out_max - out_min) / (in_max - in_min) + out_min);
	}

	// ----------------------------------------------------------------------------------------
	protected int glHeightToPixelHeight(float glHeight) {
		if (height <= 0) {
			// Log.d(TAG, "Checked height and size was less than or equal to
			// zero");
		}
		return map(glHeight, -getGlWindowVerticalSize() / 2, getGlWindowVerticalSize() / 2, height, 0);
	}

	// ----------------------------------------------------------------------------------------
	protected float pixelHeightToGlHeight(float pxHeight) {
		return map(pxHeight, height, 0, -getGlWindowVerticalSize() / 2, getGlWindowVerticalSize() / 2);
	}
}
