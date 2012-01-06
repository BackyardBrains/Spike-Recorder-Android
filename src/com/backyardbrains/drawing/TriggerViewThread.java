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

	/**
	 * Initialize GL bits, set up the GL area so that we're lookin at it
	 * properly, create a new {@link BybGLDrawable}, then commence drawing on it
	 * like the dickens.
	 * 
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
//		final int defaultSampleWidth = 4410;
		
		setupSurfaceAndDrawable();
		mAudioService = null;
		bindAudioService(true);
		registerScaleChangeReceiver(true);
		registerThresholdChangeReceiver(true);
		Intent i = new Intent("BYBToggleTrigger").putExtra("triggerMode", true);
		parent.getContext().sendBroadcast(i);
		setDefaultThresholdValue();
//		setTriggerWidth((int) (defaultSampleWidth/bufferLengthDivisor));
		while (!mDone) {
			// grab current audio from audioservice
			if (mAudioServiceIsBound && mAudioService != null) {

				// Reset our Audio buffer
				short[] mBufferToDraw = null;
				// Read new mic data
				synchronized (mAudioService) {
					mBufferToDraw = mAudioService.getTriggerBuffer();
				}
				
				if (mBufferToDraw == null || mBufferToDraw.length <= 0) {
					glman.glClear();
					setGlWindow((int) getxEnd());
					if (isDrawThresholdLine()) {
						drawThresholdLine();
					}
					glman.swapBuffers();
					continue;
				}

				setxEnd(mBufferToDraw.length);
				int samplesToShow = Math.round(mBufferToDraw.length / bufferLengthDivisor);
				
				synchronized (parent) {
					setLabels(samplesToShow);
				}

				glman.glClear();
				waveformShape.setBufferToDraw(mBufferToDraw);
				setGlWindow(samplesToShow, mBufferToDraw.length/2);
				waveformShape.draw(glman.getmGL());
				if (isDrawThresholdLine()) {
					drawThresholdLine();
				}
				glman.swapBuffers();
				try {
					sleep(20);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		i = new Intent("BYBToggleTrigger").putExtra("triggerMode", false);
		parent.getContext().sendBroadcast(i);
		bindAudioService(false);
		registerScaleChangeReceiver(false);
		registerThresholdChangeReceiver(false);
		mConnection = null;
	}

	protected void setLabels(final int samplesToShow) {
		setmVText();
		super.setLabels(samplesToShow);
	}

	private void setDefaultThresholdValue() {
		thresholdPixelHeight = glHeightToPixelHeight(yEnd / 2 * mScaleFactor);
		setmVText();
	}

	protected void setGlWindow(final int samplesToShow, int centeredAt) {
		glman.initGL(centeredAt - samplesToShow/2, centeredAt + samplesToShow/2, yBegin * mScaleFactor, yEnd
				* mScaleFactor);
	}

	private void setmVText() {
		final float glHeight = pixelHeightToGlHeight(thresholdPixelHeight);
		final float yPerDiv = glHeight / 24.5f;
		if (mAudioService != null && mAudioServiceIsBound) {
			mAudioService.getTriggerHandler().post(new Runnable() {
				@Override public void run() {
					((TriggerHandler)mAudioService.getTriggerHandler()).setThreshold(glHeight*5/2);
				}
			});
		}
		parent.setmVText(yPerDiv);
	}

	private float glHeightToPixelHeight(float glHeight) {
		return (glHeight / (yEnd * mScaleFactor * 2)) * parent.getHeight();
	}

	private float pixelHeightToGlHeight(float pxHeight) {
		return pxHeight / parent.getHeight() * (yEnd * mScaleFactor * 2);
	}

	protected void drawThresholdLine() {
		float[] thresholdLine = new float[] { 0, getThresholdValue(),
				getxEnd(), getThresholdValue() };
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
		return parent.getHeight() / 2 - thresholdPixelHeight;
	}

	public void adjustThresholdValue(float dy) {
		if (dy < parent.getHeight() / 2)
			thresholdPixelHeight = parent.getHeight() / 2 - dy;
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
