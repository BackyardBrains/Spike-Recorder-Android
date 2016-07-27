package com.backyardbrains.drawing;

import java.io.File;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import com.backyardbrains.BYBUtils;
import com.backyardbrains.BackyardBrainsApplication;
import com.backyardbrains.analysis.BYBSpike;
import com.backyardbrains.audio.TriggerAverager.TriggerHandler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class FindSpikesRenderer extends BYBBaseRenderer {

	private static final String		TAG					= "FindSpikesRenderer";
	private float					playheadPosition	= 0.5f;
	protected int					glWidth;
	protected int					glHeight;
	protected BYBSpike[]			spikes;
	private int[]					thresholds			= new int[2];

	public static final int			LEFT_THRESH_INDEX	= 0;
	public static final int			RIGHT_THRESH_INDEX	= 1;

	public static final String[]	thresholdsNames		= { "LeftSpikesHandle", "RightSpikesHandle" };


	private float[] currentColor = BYBColors.getColorAsGlById(BYBColors.red);
	
	private float[] whiteColor = BYBColors.getColorAsGlById(BYBColors.white);
	// ----------------------------------------------------------------------------------------
	public FindSpikesRenderer(Context context) {
		
		super(context);
	//	//Log.d(TAG, "CONSTRUCTOR!");
		updateThresholdHandles();
	}

	// ----------------------------------------------------------------------------------------
	@Override
	public void setGlWindowHorizontalSize(int newX) {
		////Log.d(TAG, "SetGLHorizontalSize " + getGlWindowHorizontalSize() + " glWidth: " + glWidth + " newX: " + newX);
		 super.setGlWindowHorizontalSize(Math.abs(newX));
		//this.glWindowHorizontalSize = Math.abs(newX);

	}
	@Override
	// ----------------------------------------------------------------------------------------
	public void setGlWindowVerticalSize(int newY) {
		super.setGlWindowVerticalSize(Math.abs(newY));
		//this.glWindowVerticalSize = Math.abs(newY);
		updateThresholdHandles();
	}
	// ----------------------------------------------------------------------------------------
	public void updateThresholdHandles() {
		updateThresholdHandle(LEFT_THRESH_INDEX);
		updateThresholdHandle(RIGHT_THRESH_INDEX);
	}
	// ----------------------------------------------------------------------------------------
	public int getThreshold(int index){
		if(index >= 0 && index < 2){
			return thresholds[index];
		}
		return 0;
	}
	// ----------------------------------------------------------------------------------------
		public int getThresholdScreenValue(int index){
			if(index >= 0 && index < 2){
			//	//Log.d(TAG, "getThreshold  ScreenValue: " + glHeightToPixelHeight(thresholds[index]) + "  glValue: " + thresholds[index] );
				return glHeightToPixelHeight(thresholds[index]);
			}
			return 0;
		}
	// ----------------------------------------------------------------------------------------
	public void updateThresholdHandle(int threshIndex) {
		if (threshIndex >= 0 && threshIndex < thresholds.length) {
			Intent j = new Intent();
			j.setAction("BYBUpdateThresholdHandle");
			j.putExtra("name", thresholdsNames[threshIndex]);
			j.putExtra("pos", glHeightToPixelHeight(thresholds[threshIndex]));
			context.sendBroadcast(j);
		}
	}

	// ----------------------------------------------------------------------------------------
	public void setThreshold(int t, int index) {
		setThreshold(t, index, false);
	}
	// ----------------------------------------------------------------------------------------
	public void setThreshold(int t, int index, boolean bBroadcast) {
		if (index >= 0 && index < 2) {
			thresholds[index] = t;
			if (bBroadcast) {
				updateThresholdHandle(index);
			}
		}
	}
	// ----------------------------------------------------------------------------------------
	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		super.onSurfaceChanged(gl, width, height);
		updateThresholdHandles();
	}
	// ----------------------------------------------------------------------------------------
	@Override
	public void onDrawFrame(GL10 gl) {
		if (!getAudioSamples()) {
			//Log.d(TAG, "No audio file!");
			return;
		}
		if (!BYBUtils.isValidAudioBuffer(mBufferToDraws)) {
			// //Log.d(TAG, "Invalid audio buffer!");
			return;
		}
		// //Log.d(TAG, "SetGLHorizontalSize "+getGlWindowHorizontalSize()+ "
		// glWidth: " + glWidth);
		preDrawingHandler();
		BYBUtils.glClear(gl);
		drawingHandler(gl);
		postDrawingHandler(gl);
		// }
	}

	// ----------------------------------------------------------------------------------------
	private boolean getSpikes() {

		if (((BackyardBrainsApplication) context).getAnalysisManager() != null) {
			spikes = ((BackyardBrainsApplication) context).getAnalysisManager().getSpikes();
			if (spikes.length > 0) {
				return true;
			}
		}
		spikes = null;
		return false;

	}

	// ----------------------------------------------------------------------------------------
	public boolean getAudioSamples() {

		if (((BackyardBrainsApplication) context).getAnalysisManager() != null) {
			mBufferToDraws = ((BackyardBrainsApplication) context).getAnalysisManager().getReaderSamples();
			return true;
		}
		return false;

	}

	@Override
	protected void preDrawingHandler() {
	}

	// ----------------------------------------------------------------------------------------
	public void setStartSample(float pos) {// normalized position
		playheadPosition = pos;
		////Log.d(TAG, "setStartSample: " + pos);
	}

	// ----------------------------------------------------------------------------------------
	@Override
	protected void drawingHandler(GL10 gl) {
		setGlWindow(gl, getGlWindowHorizontalSize(), mBufferToDraws.length);
		FloatBuffer mVertexBuffer = getWaveformBuffer(mBufferToDraws);

		// firstBufferDrawnCheck();
		// autoScaleCheck();

		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();

		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glLineWidth(1f);
		gl.glColor4f(0f, 1f, 0f, 1f);
		gl.glVertexPointer(2, GL10.GL_FLOAT, 0, mVertexBuffer);
		gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, mVertexBuffer.limit() / 2);
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);

		if (!getSpikes()) return;
		FloatBuffer spikesBuffer = getPointsFromSpikes();

		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
		gl.glPointSize(10.0f);
		// gl.glColor4f(1f, .25f, 1f, 1f);
		gl.glVertexPointer(2, GL10.GL_FLOAT, 0, spikesBuffer);
		gl.glColorPointer(4, GL10.GL_FLOAT, 0, getColorsFloatBuffer());
		gl.glDrawArrays(GL10.GL_POINTS, 0, spikesBuffer.limit() / 2);
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL10.GL_COLOR_ARRAY);

		final float thresholdLineLength = mBufferToDraws.length;

		float[] thresholdLine = new float[] { -thresholdLineLength * 2, thresholds[LEFT_THRESH_INDEX], thresholdLineLength * 2, thresholds[LEFT_THRESH_INDEX], -thresholdLineLength * 2, thresholds[RIGHT_THRESH_INDEX], thresholdLineLength * 2, thresholds[RIGHT_THRESH_INDEX] };

		FloatBuffer thl = BYBUtils.getFloatBufferFromFloatArray(thresholdLine);
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glColor4f(currentColor[0],currentColor[1],currentColor[2],currentColor[3]);
		gl.glLineWidth(2.0f);
		gl.glVertexPointer(2, GL10.GL_FLOAT, 0, thl);
		gl.glDrawArrays(GL10.GL_LINES, 0, thl.limit() / 2);
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);

	}
// -----------------------------------------------------------------------------------------------------------------------------
	public void setCurrentColor(float[] color) {
		if(currentColor.length == color.length && currentColor.length == 4){
			for(int i=0; i < currentColor.length;i++){
				currentColor[i] = color[i];
			}
		}
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	protected FloatBuffer getColorsFloatBuffer() {
		float[] arr = null;
		if (spikes != null) {
			if (spikes.length > 0) {
				arr = new float[spikes.length * 4];
				int j = 0; // index of arr
				int mn = Math.min(thresholds[0], thresholds[1]);
				int mx = Math.max(thresholds[0], thresholds[1]);
				try {
					for (int i = 0; i < spikes.length; i++) {
						float v = spikes[i].value;
						float[] colorToSet = whiteColor;
						if (v >= mn && v < mx) {
							colorToSet = currentColor;
						}
						for (int k = 0; k < 4; k++) {
							arr[j++] = colorToSet[k];
						}
					}
				} catch (ArrayIndexOutOfBoundsException e) {
					Log.e(TAG, e.getMessage());
				}
			}
		}
		if (arr == null) arr = new float[0];
		return BYBUtils.getFloatBufferFromFloatArray(arr);
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	protected FloatBuffer getPointsFromSpikes() {
		float[] arr = null;
		if (spikes != null) {
			if (spikes.length > 0) {
				arr = new float[spikes.length * 2];
				int j = 0; // index of arr
				try {
					for (int i = 0; i < spikes.length; i++) {
						arr[j++] = spikes[i].index;
						arr[j++] = spikes[i].value;
					}
				} catch (ArrayIndexOutOfBoundsException e) {
					Log.e(TAG, e.getMessage());
				}
			}
		}
		if (arr == null) arr = new float[0];
		return BYBUtils.getFloatBufferFromFloatArray(arr);
	}

	// -----------------------------------------------------------------------------------------------------------------------------
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
		return BYBUtils.getFloatBufferFromFloatArray(arr);
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	@Override
	protected void setGlWindow(GL10 gl, final int samplesToShow, final int lengthOfSampleSet) {
		final int size = getGlWindowVerticalSize();
		int startSample = (int) Math.floor((lengthOfSampleSet - samplesToShow) * playheadPosition);
		int endSample = startSample + samplesToShow;
		// initGL(gl, (lengthOfSampleSet - samplesToShow) / 2,
		// (lengthOfSampleSet + samplesToShow) / 2, -size / 2, size / 2);
		initGL(gl, startSample, endSample, -size / 2, size / 2);
	}

}
