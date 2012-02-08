package com.backyardbrains.drawing;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.backyardbrains.audio.TriggerAverager.TriggerHandler;

public class TriggerViewThread extends OscilloscopeGLThread {

	private static final String TAG = TriggerViewThread.class.getCanonicalName();
	private float thresholdPixelHeight;

	TriggerViewThread(OscilloscopeGLSurfaceView view) {
		super(view);
		Log.d(TAG, "Creating TriggerViewThread");
	}

	public boolean isDrawThresholdLine() {
		return true;
	}

	@Override
	protected void postRunLoopHandler() {
		super.postRunLoopHandler();
		broadcastToggleTrigger();
		registerThresholdChangeReceiver(false);
	}

	@Override
	protected void preRunLoopHandler() {
		registerThresholdChangeReceiver(true);
		broadcastToggleTrigger();
		setDefaultThresholdValue();
		super.preRunLoopHandler();
	}
	
	@Override
	protected void noValidBufferFallback() {
		drawBlankThresholdWindow();
		super.noValidBufferFallback();
	}
	
	@Override
	protected void postDrawingHandler() {
		drawThresholdLine();
	}
	
	@Override
	protected void getCurrentAudio() {
		synchronized (mAudioService) {
			mBufferToDraws = mAudioService.getTriggerBuffer();
		}
	}

	private void drawBlankThresholdWindow() {
		glman.glClear();
		setGlWindow(glWindowHorizontalSize, glWindowHorizontalSize);
		drawThresholdLine();
		glman.swapBuffers();
	}

	private void broadcastToggleTrigger() {
		Intent i = new Intent("BYBToggleTrigger").putExtra("triggerMode", true);
		parent.getContext().sendBroadcast(i);
	}

	protected void setLabels(final int samplesToShow) {
		setmVText();
		super.setLabels(samplesToShow);
	}

	private void setDefaultThresholdValue() {
		thresholdPixelHeight = glHeightToPixelHeight(getGlWindowVerticalSize()/4);
		setmVText();
	}

	protected void setGlWindow(final int samplesToShow, final int lengthOfSampleSet) {
		final int size = getGlWindowVerticalSize();
		glman.initGL(lengthOfSampleSet/2 - samplesToShow/2, lengthOfSampleSet/2 + samplesToShow/2, -size/2, size/2);
	}

	private void setmVText() {
		final float glHeight = pixelHeightToGlHeight(thresholdPixelHeight);
		final float yPerDiv = glHeight / 4 / 24.5f / 1000;
		if (mAudioService != null && mAudioServiceIsBound) {
			mAudioService.getTriggerHandler().post(new Runnable() {
				@Override public void run() {
					((TriggerHandler)mAudioService.getTriggerHandler()).setThreshold(glHeight);
				}
			});
		}
		parent.setmVText(yPerDiv);
	}
	
	long map(long x, long in_min, long in_max, long out_min, long out_max)
	{
	  return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
	}

	private float glHeightToPixelHeight(float glHeight) {
		//return (glHeight / getGlWindowVerticalSize()) * parent.getHeight();
		return map((long) glHeight, -getGlWindowVerticalSize()/2, getGlWindowVerticalSize()/2, parent.getHeight(), 0);
	}

	private float pixelHeightToGlHeight(float pxHeight) {
		return map((long) pxHeight, parent.getHeight(), 0, -getGlWindowVerticalSize()/2, getGlWindowVerticalSize()/2);
		//return pxHeight / parent.getHeight() * getGlWindowVerticalSize();
	}

	protected void drawThresholdLine() {
		final float thresholdLineLength = (mBufferToDraws == null) ? glWindowHorizontalSize : mBufferToDraws.length;
		float[] thresholdLine = new float[] { -thresholdLineLength*2, getThresholdValue(),
				thresholdLineLength*2, getThresholdValue() };
		FloatBuffer thl = getFloatBufferFromFloatArray(thresholdLine);
		glman.getmGL().glEnableClientState(GL10.GL_VERTEX_ARRAY);
		glman.getmGL().glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
		glman.getmGL().glLineWidth(1.0f);
		glman.getmGL().glVertexPointer(2, GL10.GL_FLOAT, 0, thl);
		glman.getmGL().glDrawArrays(GL10.GL_LINES, 0, 4);
		glman.getmGL().glDisableClientState(GL10.GL_VERTEX_ARRAY);
	}// ((?!GC_)).*

	public float getThresholdValue() {
		return pixelHeightToGlHeight(thresholdPixelHeight);
	}

	public float getThresholdYValue() {
		return thresholdPixelHeight;
	}

	public void adjustThresholdValue(float dy) {
		//if (dy < parent.getHeight() / 2)
			thresholdPixelHeight = dy;
	}

	protected void registerThresholdChangeReceiver(boolean on) {
		if (on) {
			thChangeReceiver = new ThresholdChangeReceiver();
			parent.getContext().registerReceiver(thChangeReceiver,
					new IntentFilter("BYBThresholdChange"));
		} else {
			parent.getContext().unregisterReceiver(thChangeReceiver);
		}

	}

	private ThresholdChangeReceiver thChangeReceiver;

	private class ThresholdChangeReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(android.content.Context context,
				android.content.Intent intent) {
			adjustThresholdValue(intent.getFloatExtra("deltathreshold", 1));
		};
	}

}
