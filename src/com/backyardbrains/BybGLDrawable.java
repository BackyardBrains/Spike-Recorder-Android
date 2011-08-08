package com.backyardbrains;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.util.Log;

class BybGLDrawable {
	private static final String TAG = "BYBGLShape";

	private final float Y_SCALING = .001f;

	/**
	 * 
	 */
	private final OscilliscopeGLThread parent;

	/**
	 * @param oscilliscopeGLThread
	 */
	BybGLDrawable(OscilliscopeGLThread oscilliscopeGLThread) {
		parent = oscilliscopeGLThread;
	}

	private short[] mBufferToDraw;

	FloatBuffer getFloatBufferFromFloatArray(float array[]) {
		ByteBuffer temp = ByteBuffer.allocateDirect(array.length * 4);
		temp.order(ByteOrder.nativeOrder());
		FloatBuffer buf = temp.asFloatBuffer();
		buf.put(array);
		buf.position(0);
		return buf;
	}

	public void draw(GL10 gl_obj) {
		// vertices = transform(vertices);
		// FloatBuffer mVertexBuffer = getFloatBufferFromFloatArray(vertices);
		FloatBuffer mVertexBuffer = getWaveformBuffer(mBufferToDraw);
		gl_obj.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl_obj.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);
		gl_obj.glDrawArrays(GL10.GL_LINE_STRIP, 0, mVertexBuffer.limit() / 3);
	}

	private FloatBuffer getWaveformBuffer(short[] shortArrayToDraw) {
		if (shortArrayToDraw == null) {
			Log.w(TAG, "Drawing fake line with null data");
			float[] array = { 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 2.0f, 0.0f,
					0.0f };
			return getFloatBufferFromFloatArray(array);
		}
		Log.d(TAG, "Received buffer to draw");

		float[] arr = new float[shortArrayToDraw.length * 3]; // array to fill
		int j = 0; // index of arr
		float interval = parent.x_width / shortArrayToDraw.length;
		for (int i = 0; i < shortArrayToDraw.length; i++) {
			arr[j++] = i * interval;
			arr[j++] = shortArrayToDraw[i] * Y_SCALING;
			arr[j++] = 0f;
		}
		return getFloatBufferFromFloatArray(arr);
	}

	public void setBufferToDraw(ByteBuffer audioBuffer) {
		if (audioBuffer != null) {
			audioBuffer.clear();
			mBufferToDraw = new short[audioBuffer.asShortBuffer().capacity()];
			audioBuffer.asShortBuffer().get(mBufferToDraw, 0,
					mBufferToDraw.length);
			Log.i(TAG, "Got audio data");
		} else {
			Log.w(TAG, "Received null audioBuffer");
		}
	}
}