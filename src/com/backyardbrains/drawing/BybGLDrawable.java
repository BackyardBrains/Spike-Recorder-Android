package com.backyardbrains.drawing;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.os.SystemClock;
import android.util.Log;

/**
 * An object capable of drawing itself to a provided GL10 object
 * 
 * @author Nathan Dotz <nate@backyardbrains.com>
 * @version 1
 * 
 */
class BybGLDrawable {
	private static final String TAG = BybGLDrawable.class.getCanonicalName();
	private final OscilloscopeGLThread parent;

	/**
	 * @param oscilliscopeGLThread
	 *            the thread responsible for this object.
	 */
	BybGLDrawable(OscilloscopeGLThread oscilliscopeGLThread) {
		parent = oscilliscopeGLThread;
	}

	private short[] mBufferToDraw;
	private long firstBufferDrawn = 0;

	/**
	 * Draw this object on the provided {@link GL10} object. In addition, check
	 * to see if the frame has been autoscaled yet. If not, do so exactly once,
	 * and only after 100ms have passed.
	 * 
	 * @param gl_obj
	 */
	public void draw(GL10 gl_obj) {
		
		FloatBuffer mVertexBuffer = getWaveformBuffer(mBufferToDraw);

		firstBufferDrawnCheck();
		autoScaleCheck();
		
		gl_obj.glMatrixMode(GL10.GL_MODELVIEW);
		gl_obj.glLoadIdentity();

		gl_obj.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl_obj.glLineWidth(1f);
		gl_obj.glColor4f(0f, 1f, 0f, 1f);
		gl_obj.glVertexPointer(2, GL10.GL_FLOAT, 0, mVertexBuffer);
		gl_obj.glDrawArrays(GL10.GL_LINE_STRIP, 0, mVertexBuffer.limit() / 2);
		gl_obj.glDisableClientState(GL10.GL_VERTEX_ARRAY);
	}

	private void autoScaleCheck() {
		if (!parent.isAutoScaled()
				&& firstBufferDrawn != 0
				&& (SystemClock.currentThreadTimeMillis() - firstBufferDrawn) > 100) {
			autoSetFrame(mBufferToDraw);
			parent.setAutoScaled(true);
		}
	}

	private void firstBufferDrawnCheck() {
		if (firstBufferDrawn == 0) {
			firstBufferDrawn = SystemClock.currentThreadTimeMillis();
		}
	}

	/**
	 * Called exactly once, ~100 ms after each time the activity gets attention
	 * from the screen. Takes a min/max metric of the current buffer and scales
	 * the GL surface to the appropriate size.
	 * 
	 * @param arrayToScaleTo
	 */
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
			if (-newyMax > parent.getMinimumDetectedPCMValue()) {
				Log.d(TAG, "Scaling window to " + -newyMax + " < y < "
						+ newyMax);
				parent.setGlWindowVerticalSize(newyMax*2);
			}

		}
		// parent.setxEnd(arrayToScaleTo.length);

	}

	/**
	 * Convert the local buffer-to-draw data over to a {@link FloatBuffer}
	 * structure suitable for feeding to
	 * {@link GL10#glDrawArrays(int, int, int)}
	 * 
	 * @param shortArrayToDraw
	 *            containing raw audio data
	 * @return {@link FloatBuffer} ready to be fed to
	 *         {@link GL10#glDrawArrays(int, int, int)}
	 */
	private FloatBuffer getWaveformBuffer(short[] shortArrayToDraw) {
		float[] arr = new float[shortArrayToDraw.length * 2]; // array to fill
		int j = 0; // index of arr
		float interval = 1;
		for (int i = 0; i < shortArrayToDraw.length; i++) {
			arr[j++] = i * interval;
			arr[j++] = shortArrayToDraw[i];
		}
		return parent.getFloatBufferFromFloatArray(arr);
	}

	public void forceRescale() {
		parent.setAutoScaled(false);
	}

	/**
	 * Called from the parent thread, this takes a {@link ByteBuffer} of audio
	 * data from the recording device and converts it into an array of 16-bit
	 * shorts, to later be processed by the drawing functions.
	 * 
	 * @param audioBuffer
	 *            {@link ByteBuffer} to be drawn
	 */
	public void setBufferToDraw(short[] audioBuffer) {
		if (audioBuffer != null) {
			mBufferToDraw = audioBuffer;
		} else {
			Log.w(TAG, "Received null audioBuffer");
		}
	}
}
