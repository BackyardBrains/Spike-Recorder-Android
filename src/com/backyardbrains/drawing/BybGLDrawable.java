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
	private static final String TAG = "BYBGLShape";

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

	private boolean autoScaled;

	private boolean mShouldDrawGridlines = false;

	/**
	 * Takes an array of floats and returns a buffer representing the same
	 * floats
	 * 
	 * @param array
	 *            to be converted
	 * @return converted array as FloatBuffer
	 * @deprecated Use
	 *             {@link com.backyardbrains.drawing.OscilloscopeGLThread#getFloatBufferFromFloatArray(float[])}
	 *             instead
	 */
	FloatBuffer getFloatBufferFromFloatArray(float array[]) {
		return parent.getFloatBufferFromFloatArray(array);
	}

	/**
	 * Draw this object on the provided {@link GL10} object. In addition, check
	 * to see if the frame has been autoscaled yet. If not, do so exactly once,
	 * and only after 100ms have passed.
	 * 
	 * @param gl_obj
	 */
	public void draw(GL10 gl_obj) {
		parent.initGL();
		FloatBuffer mVertexBuffer = getWaveformBuffer(mBufferToDraw);

		if (!autoScaled
				&& firstBufferDrawn != 0
				&& (SystemClock.currentThreadTimeMillis() - firstBufferDrawn) > 100) {
			autoSetFrame(mBufferToDraw);
			autoScaled = true;
		}
		gl_obj.glMatrixMode(GL10.GL_MODELVIEW);
		gl_obj.glLoadIdentity();

		gl_obj.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl_obj.glLineWidth(1f);
		gl_obj.glColor4f(0f, 1f, 0f, 1f);
		gl_obj.glVertexPointer(2, GL10.GL_FLOAT, 0, mVertexBuffer);
		gl_obj.glDrawArrays(GL10.GL_LINE_STRIP, 0, mVertexBuffer.limit() / 2);

		gl_obj.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		if (mShouldDrawGridlines)
			drawGridLines(gl_obj);

	}

	private void drawGridLines(GL10 gl_obj) {
		// @TODO make this line up with wave center line
		int numHorizontalGridLines = parent.numHorizontalGridLines;
		int numVerticalGridLines = parent.numVerticalGridLines;
		float[] gridVertexArray = new float[4 * (parent.numHorizontalGridLines + numVerticalGridLines)];
		for (int i = 0; i < numHorizontalGridLines; ++i) {
			// Fill in the horizontal grid lines
			float horzval = (float) (parent.getyBegin() + (i + 1.0)
					* (parent.getyEnd() - parent.getyBegin())
					/ (numHorizontalGridLines + 1.0));
			gridVertexArray[i * 4] = parent.getxBegin();
			gridVertexArray[i * 4 + 1] = horzval;
			gridVertexArray[i * 4 + 2] = parent.getxEnd();
			gridVertexArray[i * 4 + 3] = horzval;
		}

		int idx;
		for (int i = 0; i < numVerticalGridLines; ++i) {
			// Now the vertical lines
			float vertval = (float) (parent.getxBegin() + (i + 1.0)
					* (parent.getxEnd() - parent.getxBegin())
					/ (numVerticalGridLines + 1.0));
			idx = numHorizontalGridLines * 4;
			gridVertexArray[idx + i * 4] = vertval;
			gridVertexArray[idx + i * 4 + 1] = parent.getyBegin();
			gridVertexArray[idx + i * 4 + 2] = vertval;
			gridVertexArray[idx + i * 4 + 3] = parent.getyEnd();
		}
		gl_obj.glColor4f(.5f, .5f, .5f, .5f);
		gl_obj.glLineWidth(1f);
		FloatBuffer glVertexBuffer = parent
				.getFloatBufferFromFloatArray(gridVertexArray);
		gl_obj.glVertexPointer(2, GL10.GL_FLOAT, 0, glVertexBuffer);

		gl_obj.glDrawArrays(GL10.GL_LINES, 0,
				2 * (numHorizontalGridLines + numVerticalGridLines));

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
				newyMax = Math.abs(theMax) * 1.5f;
			} else {
				newyMax = Math.abs(theMin) * 1.5f;
			}
			if (-newyMax > parent.getyMin()) {
				Log.d(TAG, "Scaling window to " + -newyMax + " < y < " + newyMax);
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

		if (firstBufferDrawn == 0) {
			firstBufferDrawn = SystemClock.currentThreadTimeMillis();
		}

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
		autoScaled = false;
	}

	/**
	 * Called from the parent thread, this takes a {@link ByteBuffer} of audio
	 * data from the recording device and converts it into an array of 16-bit
	 * shorts, to later be processed by the drawing functions.
	 * 
	 * @param audioBuffer
	 *            {@link ByteBuffer} to be drawn
	 */
	public void setBufferToDraw(ByteBuffer audioBuffer) {
		if (audioBuffer != null) {
			audioBuffer.clear();
			int bufferCapacity = audioBuffer.asShortBuffer().capacity();
			mBufferToDraw = new short[bufferCapacity];
			audioBuffer.asShortBuffer().get(mBufferToDraw, 0,
					mBufferToDraw.length);
			// parent.setxBegin(0);
			parent.setxEnd(mBufferToDraw.length / 2);
			Log.i(TAG, "Got audio data: " + bufferCapacity + " samples, or "
					+ bufferCapacity / 44100.0f * 1000 + "ms");
		} else {
			Log.w(TAG, "Received null audioBuffer");
		}
	}
}
