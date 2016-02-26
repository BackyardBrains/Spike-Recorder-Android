package com.backyardbrains;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;

public class BYBUtils {

	// ----------------------------------------------------------------------------------------
	public static FloatBuffer getFloatBufferFromFloatArray(final float[] array) {
		final FloatBuffer buf;
		final ByteBuffer temp = ByteBuffer.allocateDirect(array.length * 4);
		temp.order(ByteOrder.nativeOrder());
		buf = temp.asFloatBuffer();
		buf.put(array);
		buf.position(0);
		return buf;
	}

	// ----------------------------------------------------------------------------------------
	public static boolean isValidAudioBuffer(float[] buffer) {
		if (buffer == null) {
			return false;
		}
		return buffer.length > 0;
	}

	// ----------------------------------------------------------------------------------------
	public static boolean isValidAudioBuffer(short[] buffer) {
		if (buffer == null) {
			return false;
		}
		return buffer.length > 0;
	}
	// ----------------------------------------------------------------------------------------
	public static void glClear(GL10 gl) {
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
	}

	// ----------------------------------------------------------------------------------------
	public static int map(float glHeight, int in_min, int in_max, int out_min, int out_max) {
		return (int) ((glHeight - in_min) * (out_max - out_min) / (in_max - in_min) + out_min);
	}

	public static FloatBuffer floatArrayListToFloatBuffer(ArrayList<float[]> arrayList){
		final FloatBuffer buf;
		int totalArraySize = 0;
		for (float[]lv:arrayList){
			totalArraySize += lv.length;
		}
		final ByteBuffer temp = ByteBuffer.allocateDirect(totalArraySize * 4);
		temp.order(ByteOrder.nativeOrder());
		buf = temp.asFloatBuffer();

		for (float[]lv:arrayList){
			for(float v: lv){
				buf.put(v);
			}
		}
		buf.position(0);
		return buf;
	}
}

