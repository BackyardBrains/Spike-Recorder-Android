package com.backyardbrains.drawing;

import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.util.Log;

import com.backyardbrains.audio.TriggerAverager.TriggerHandler;

public class ThresholdRenderer extends OscilloscopeRenderer {

	private static final String TAG = ThresholdRenderer.class
			.getCanonicalName();
	private float thresholdPixelHeight;
	
	public ThresholdRenderer(Activity backyardAndroidActivity) {
		super(backyardAndroidActivity);
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		super.onSurfaceCreated(gl, config);
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		super.onSurfaceChanged(gl, width, height);
		adjustThresholdValue(glHeightToPixelHeight(getGlWindowVerticalSize()/4));
	}
	
	@Override
	protected void postDrawingHandler(GL10 gl) {
		super.postDrawingHandler(gl);
		final float thresholdLineLength = mBufferToDraws.length;
		float[] thresholdLine = new float[] { -thresholdLineLength*2, getThresholdValue(),
				thresholdLineLength*2, getThresholdValue() };
		FloatBuffer thl = getFloatBufferFromFloatArray(thresholdLine);
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
		gl.glLineWidth(1.0f);
		gl.glVertexPointer(2, GL10.GL_FLOAT, 0, thl);
		gl.glDrawArrays(GL10.GL_LINES, 0, 4);
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
	}

	@Override
	protected FloatBuffer getWaveformBuffer(short[] shortArrayToDraw) {
		float[] arr = new float[shortArrayToDraw.length * 2]; // array to fill
		int j = 0; // index of arr
		try {
			for (int i = 0; i < shortArrayToDraw.length; i++) {
				arr[j++] = i;
				arr[j++] = shortArrayToDraw[i];
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			Log.e(TAG, e.getMessage());
		}
		return getFloatBufferFromFloatArray(arr);
	}

	public float getThresholdValue() {
		return pixelHeightToGlHeight(thresholdPixelHeight);
	}
	
	@Override
	protected void getCurrentAudio() {
		synchronized (mAudioService) {
			mBufferToDraws = mAudioService.getTriggerBuffer();
		}
	}
	
	@Override
	protected void setmVText () {
		final float glHeight = pixelHeightToGlHeight(thresholdPixelHeight);
		final float yPerDiv = glHeight / 4 / 24.5f / 1000;

		super.setmVText(yPerDiv);
	}
	
	public float getThresholdYValue() {
		return thresholdPixelHeight;
	}

	protected void setGlWindow(GL10 gl, final int samplesToShow,
			final int lengthOfSampleSet) {
		final int size = getGlWindowVerticalSize();
		initGL(gl, (lengthOfSampleSet - samplesToShow)/2, (lengthOfSampleSet + samplesToShow)/2, -size/2, size/2);
	}
	
	int map(float glHeight, int in_min, int in_max, int out_min, int out_max)
	{
	  return (int) ((glHeight - in_min) * (out_max - out_min) / (in_max - in_min) + out_min);
	}

	private float glHeightToPixelHeight(float glHeight) {
		if (height <= 0) {
			Log.d(TAG, "Checked height and size was less than or equal to zero");
		}
		return map(glHeight, -getGlWindowVerticalSize()/2, getGlWindowVerticalSize()/2, height, 0);
	}

	private float pixelHeightToGlHeight(float pxHeight) {
		return map(pxHeight, height, 0, -getGlWindowVerticalSize()/2, getGlWindowVerticalSize()/2);
	}

	public void adjustThresholdValue(float dy) {
		thresholdPixelHeight = dy;
		Log.d(TAG, "Adjusted threshold by " + dy);
		if (mAudioService != null && mAudioServiceIsBound) {
			final float glHeight = pixelHeightToGlHeight(thresholdPixelHeight);
			mAudioService.getTriggerHandler().post(new Runnable() {
				@Override public void run() {
					((TriggerHandler)mAudioService.getTriggerHandler()).setThreshold(glHeight);
				}
			});
		}
		Log.d(TAG, "Threshold is now " + thresholdPixelHeight);
	}

}
