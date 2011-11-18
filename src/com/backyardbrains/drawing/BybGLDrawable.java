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
	/**
	 * Tag for use in LogCat
	 */
	private static final String TAG = "BYBGLDrawable";

	/**
	 * Reference to the parent thread responsible for maintaining this object.
	 */
	private final OscilloscopeGLThread parent;

	/**
	 * Construct a drawable object, storing a reference to the parent thread.
	 * 
	 * @param oscilliscopeGLThread
	 *            the thread responsible for this object.
	 */
	BybGLDrawable(OscilloscopeGLThread oscilliscopeGLThread) {
		parent = oscilliscopeGLThread;
	}

	/**
	 * The array of 16-bit numbers that represents the data to be drawn
	 */
	private short[] mBufferToDraw;

	/**
	 * Figure out when the first buffer was drawn, so we can scale after 100ms
	 */
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

		if (firstBufferDrawn == 0) {
			firstBufferDrawn = SystemClock.currentThreadTimeMillis();
		}

		if (!parent.isAutoScaled()
				&& firstBufferDrawn != 0
				&& (SystemClock.currentThreadTimeMillis() - firstBufferDrawn) > 100) {
			autoSetFrame(mBufferToDraw);
			parent.setAutoScaled(true);
		}
		
		gl_obj.glMatrixMode(GL10.GL_MODELVIEW);
		gl_obj.glLoadIdentity();

		gl_obj.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl_obj.glLineWidth(1f);
		gl_obj.glColor4f(0f, 1f, 0f, 1f);
		gl_obj.glVertexPointer(2, GL10.GL_FLOAT, 0, mVertexBuffer);
		gl_obj.glDrawArrays(GL10.GL_LINE_STRIP, 0, mVertexBuffer.limit() / 2);

		drawThresholdLine(gl_obj);

		gl_obj.glDisableClientState(GL10.GL_VERTEX_ARRAY);
	}

	public void drawThresholdLine(GL10 gl_obj) {
		if (parent.isDrawThresholdLine()) {
			short thresholdValue = (short) (-parent.getyMin()/2);
			short[] thresholdLine = new short[4];
			thresholdLine[0] = 0;
			thresholdLine[2] = (short) parent.getxEnd();
			thresholdLine[1] = thresholdValue;
			thresholdLine[3] = thresholdValue;
			gl_obj.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
			gl_obj.glLineWidth(1.0f);
			gl_obj.glVertexPointer(2, GL10.GL_FLOAT, 0,
					getWaveformBuffer(thresholdLine));
			gl_obj.glDrawArrays(GL10.GL_LINES, 0, thresholdLine.length / 2);
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
		float theMax = 0;
		float theMin = 0;

		for (int i = 0; i < arrayToScaleTo.length; i++) {
			if (theMax < arrayToScaleTo[i])
				theMax = arrayToScaleTo[i];
			if (theMin > arrayToScaleTo[i])
				theMin = arrayToScaleTo[i];
		}

		float newyMax;
		if (theMax != 0 && theMin != 0) {

			if (Math.abs(theMax) >= Math.abs(theMin)) {
				newyMax = Math.abs(theMax) * 2f;
			} else {
				newyMax = Math.abs(theMin) * 2f;
			}
			if (-newyMax > parent.getyMin()) {
				Log.d(TAG, "Scaling window to " + -newyMax + " < y < "
						+ newyMax);
				parent.setyBegin(-newyMax);
				parent.setyEnd(newyMax);
			}

		}
		parent.setxEnd(arrayToScaleTo.length);

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
		if (shortArrayToDraw == null) {
			Log.w(TAG, "Drawing fake line with null data");
			float[] array = { 0.0f, 0.0f, 1.0f, 0.0f, 2.0f, 0.0f };
			return parent.getFloatBufferFromFloatArray(array);
		}
		// Log.d(TAG, "Received buffer to draw");

		float[] arr = new float[shortArrayToDraw.length * 2]; // array to fill
		int j = 0; // index of arr
		// float interval = parent.x_width / shortArrayToDraw.length;
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
