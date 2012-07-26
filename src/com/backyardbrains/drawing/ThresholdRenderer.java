/*
 * Backyard Brains Android App
 * Copyright (C) 2011 Backyard Brains
 * by Nathan Dotz <nate (at) backyardbrains.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.backyardbrains.drawing;

import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.util.Log;

import com.backyardbrains.BackyardAndroidActivity;
import com.backyardbrains.audio.TriggerAverager.TriggerHandler;

public class ThresholdRenderer extends OscilloscopeRenderer {

	private static final String TAG = ThresholdRenderer.class
			.getCanonicalName();
	private float thresholdPixelHeight;
	private boolean drewFirstFrame;
	
	public ThresholdRenderer(BackyardAndroidActivity backyardAndroidActivity) {
		super(backyardAndroidActivity);
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		super.onSurfaceCreated(gl, config);
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		super.onSurfaceChanged(gl, width, height);
		drewFirstFrame = false;
	}

	public void defaultThresholdValue() {
		adjustThresholdValue(glHeightToPixelHeight(getGlWindowVerticalSize()/4));
	}
	
	@Override
	protected void preDrawingHandler() {
		super.preDrawingHandler();
		if(!drewFirstFrame) {
			if (thresholdPixelHeight == 0) {
				defaultThresholdValue();
			} else {
				adjustThresholdValue(thresholdPixelHeight);
			}
			drewFirstFrame = true;
		}
	}
	
	@Override
	protected void postDrawingHandler(GL10 gl) {
		super.postDrawingHandler(gl);
		final float thresholdLineLength = mBufferToDraws.length;
		final float thresholdValue = getThresholdValue();
		float[] thresholdLine = new float[] { -thresholdLineLength*2, thresholdValue,
				thresholdLineLength*2, thresholdValue };
		FloatBuffer thl = getFloatBufferFromFloatArray(thresholdLine);
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
		gl.glLineWidth(2.0f);
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
		mBufferToDraws = context.getmAudioService().getTriggerBuffer();
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
		if(dy == 0) { return; }
		thresholdPixelHeight = dy;
		Log.d(TAG, "Adjusted threshold by " + dy);
		if (context.getmAudioService() != null) {
			final float glHeight = pixelHeightToGlHeight(thresholdPixelHeight);
			context.getmAudioService().getTriggerHandler().post(new Runnable() {
				@Override public void run() {
					((TriggerHandler)context.getmAudioService().getTriggerHandler()).setThreshold(glHeight);
				}
			});
		}
		Log.d(TAG, "Threshold is now " + thresholdPixelHeight);
	}

}
