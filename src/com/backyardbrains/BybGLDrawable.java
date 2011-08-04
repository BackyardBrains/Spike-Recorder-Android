package com.backyardbrains;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

import javax.microedition.khronos.opengles.GL10;

class BybGLDrawable {

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

	private float[] vertices = { -3f, 0, 0, -2.75f, 0, 0, -2.5f, 0, 0, -2.25f,
			0, 0, -2f, 0, 0, -1.75f, 0, 0, -1.5f, 0, 0, -1.25f, 0, 0, -1f, 0,
			0, -0.75f, 0, 0, -0.5f, 0, 0, -0.25f, 0, 0, 0f, 0, 0, 0.25f, 0, 0,
			0.5f, 0, 0, 0.75f, 0, 0, 1f, 0, 0, 1.25f, 0, 0, 1.5f, 0, 0, 1.75f,
			0, 0, 2f, 0, 0, 2.25f, 0, 0, 2.5f, 0, 0, 2.75f, 0, 0, 3f, 0, 0 };

	FloatBuffer getFloatBufferFromFloatArray(float array[]) {
		ByteBuffer temp = ByteBuffer.allocateDirect(array.length * 4);
		temp.order(ByteOrder.nativeOrder());
		FloatBuffer buf = temp.asFloatBuffer();
		buf.put(array);
		buf.position(0);
		return buf;
	}

	public float[] transform(float array[]) {
		Random derp = new Random();
		for (int i = 0; i < array.length - 3; i++) {
			// ys
			if ((i + 2) % 3 == 0) {
				// copy y from next element
				array[i] = array[i + 3];
			}
			// randomize last y
		}
		array[array.length - 2] = derp.nextFloat() * (float) derp.nextInt() % 3
				- 1;
		return array;
	}

	public void draw(GL10 gl_obj) {
		vertices = transform(vertices);
		FloatBuffer mVertexBuffer = getFloatBufferFromFloatArray(vertices);
		gl_obj.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl_obj.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);
		gl_obj.glDrawArrays(GL10.GL_LINE_STRIP, 0, vertices.length / 3);
	}
}