package com.backyardbrains.drawing;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.util.Log;

public class TriggerViewThread extends OscilloscopeGLThread {

	private static final String TAG = Class.class.getCanonicalName();

	TriggerViewThread(OscilloscopeGLSurfaceView view) {
		super(view);
		Log.d(TAG , "Creating TriggerViewThread");
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
		setupSurfaceAndDrawable();

		bindAudioService(true);
		registerScaleChangeReceiver(true);
		while (!mDone) {
			// grab current audio from audioservice
			if (mAudioServiceIsBound) {

				// Reset our Audio buffer
				ByteBuffer audioInfo = null;

				// Read new mic data
				synchronized (mAudioService) {
					audioInfo = ByteBuffer.wrap(mAudioService.getAudioBuffer());
				}

				if (audioInfo != null) {
					audioInfo.clear();

					// Convert audioInfo to a short[] named mBufferToDraw
					final ShortBuffer audioInfoasShortBuffer = audioInfo
							.asShortBuffer();
					final int bufferCapacity = audioInfoasShortBuffer
							.capacity();

					final short[] mBufferToDraw = convertToShortArray(
							audioInfoasShortBuffer, bufferCapacity);
					// scale the right side to the number of data points we have
					setxEnd(mBufferToDraw.length);
					int samplesToShow = Math.round(bufferCapacity
							/ bufferLengthDivisor);

					synchronized (parent) {
						setLabels(samplesToShow);
					}

					glman.glClear();
					waveformShape.setBufferToDraw(mBufferToDraw);
					setGlWindow(samplesToShow);
					waveformShape.draw(glman.getmGL());
					if (isDrawThresholdLine()) {
						drawThresholdLine();
					}
					glman.swapBuffers();
				}
				try {
					sleep(5);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		bindAudioService(false);
		registerScaleChangeReceiver(false);
		mConnection = null;
	}
	
	protected void drawThresholdLine() {
		float thresholdValue = (yEnd / 2 / mScaleFactor);
		float[] thresholdLine = new float[4];
		thresholdLine[0] = 0;
		thresholdLine[2] = getxEnd();
		thresholdLine[1] = thresholdValue;
		thresholdLine[3] = thresholdValue;
		FloatBuffer thl = getFloatBufferFromFloatArray(thresholdLine);
		glman.getmGL().glEnableClientState(GL10.GL_VERTEX_ARRAY);
		glman.getmGL().glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
		glman.getmGL().glLineWidth(1.0f);
		glman.getmGL().glVertexPointer(2, GL10.GL_FLOAT, 0, thl);
		glman.getmGL().glDrawArrays(GL10.GL_LINES, 0, 4);
		glman.getmGL().glDisableClientState(GL10.GL_VERTEX_ARRAY);
	}

}
