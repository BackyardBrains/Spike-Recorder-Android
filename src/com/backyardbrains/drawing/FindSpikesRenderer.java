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

	private static final String TAG = "FindSpikesRenderer";
	private float playheadPosition = 0.5f;
	protected int glWidth;
	protected int glHeight;
	protected BYBSpike [] spikes;
	// ----------------------------------------------------------------------------------------
	public FindSpikesRenderer(Context context) {
		super(context);
	}
	// ----------------------------------------------------------------------------------------
//	@Override
//	public void setGlWindowHorizontalSize(int newX) {
//		Log.d(TAG, "SetGLHorizontalSize "+getGlWindowHorizontalSize()+ "  glWidth: " + glWidth + " newX: " + newX);
//	//	super.setGlWindowHorizontalSize(newX);
//		glWidth = newX;
//	
//	}
//	@Override
//	// ----------------------------------------------------------------------------------------
//	public void setGlWindowVerticalSize(int newY) {
//		super.setGlWindowVerticalSize(newY);
//		glHeight = newY;
//	}

	// ----------------------------------------------------------------------------------------
	@Override
	public void onDrawFrame(GL10 gl) {
		if (!getAudioSamples()) {
			Log.d(TAG, "No audio file!");
			return;
		}
		if (!BYBUtils.isValidAudioBuffer(mBufferToDraws)) {
	//		Log.d(TAG, "Invalid audio buffer!");
			return;
		}
		//Log.d(TAG, "SetGLHorizontalSize "+getGlWindowHorizontalSize()+ "  glWidth: " + glWidth);
		preDrawingHandler();
		BYBUtils.glClear(gl);
		drawingHandler(gl);
		postDrawingHandler(gl);
		Intent i = new Intent();
		i.setAction("DebugShit");
		i.putExtra("d", "GlWidth: " + glWidth +"\nGlHeight: " + glHeight);
		context.sendBroadcast(i);
		// }
	}
	// ----------------------------------------------------------------------------------------
		private boolean getSpikes(){
			
			if (((BackyardBrainsApplication) context).getAnalysisManager() != null) {
				spikes = ((BackyardBrainsApplication) context).getAnalysisManager().getSpikes();
				if(spikes.length>0){
					return true;	
				}
			}
			spikes = null;
			return false;

		}
	// ----------------------------------------------------------------------------------------
	public boolean getAudioSamples(){
		
		if (((BackyardBrainsApplication) context).getAnalysisManager() != null) {
			mBufferToDraws = ((BackyardBrainsApplication) context).getAnalysisManager().getReaderSamples();
			return true;
		}
		return false;

	}
	@Override
	protected void preDrawingHandler() {}
	// ----------------------------------------------------------------------------------------
	public void setStartSample(float pos){// normalized position
		playheadPosition = pos;
		Log.d(TAG, "setStartSample: " + pos);
	}
	// ----------------------------------------------------------------------------------------
	@Override
	protected void drawingHandler(GL10 gl) {
		setGlWindow(gl,getGlWindowHorizontalSize(), mBufferToDraws.length);
		FloatBuffer mVertexBuffer = getWaveformBuffer(mBufferToDraws);
		
		
		
		// firstBufferDrawnCheck();
	//	autoScaleCheck();

		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();

		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glLineWidth(1f);
		gl.glColor4f(0f, 1f, 0f, 1f);
		gl.glVertexPointer(2, GL10.GL_FLOAT, 0, mVertexBuffer);
		gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, mVertexBuffer.limit() / 2);
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		
		
		if(!getSpikes())return;
		FloatBuffer spikesBuffer = getPointsFromSpikes();
		
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glPointSize(10.0f);
		gl.glColor4f(1f, .25f, 1f, 1f);
		gl.glVertexPointer(2, GL10.GL_FLOAT, 0, spikesBuffer);
		gl.glDrawArrays(GL10.GL_POINTS, 0, spikesBuffer.limit() / 2);
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		
		
		
	}
	// -----------------------------------------------------------------------------------------------------------------------------
	// @Override
//	protected void postDrawingHandler(GL10 gl) {
//	//	Log.d(TAG, "glWidth: " +getGlWindowHorizontalSize() + "  glHeight: " + getGlWindowVerticalSize());
//	}
	protected FloatBuffer getPointsFromSpikes(){
		float[] arr = null;
		if(spikes != null){
			if(spikes.length > 0){
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
		if(arr == null)arr = new float [0];
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
		int startSample = (int) Math.floor((lengthOfSampleSet - samplesToShow)*playheadPosition);
		int endSample = startSample + samplesToShow;
		//initGL(gl, (lengthOfSampleSet - samplesToShow) / 2, (lengthOfSampleSet + samplesToShow) / 2, -size / 2, size / 2);
		initGL(gl, startSample, endSample, -size / 2, size / 2);
	}

}
