package com.backyardbrains.drawing;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.text.DecimalFormat;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import com.backyardbrains.BackyardBrainsApplication;
import com.backyardbrains.BackyardBrainsMain;
import com.backyardbrains.audio.AudioService;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.opengl.GLSurfaceView;
import android.os.SystemClock;
import android.util.Log;
import com.backyardbrains.BYBUtils;

public class BYBBaseRenderer implements GLSurfaceView.Renderer {
	private static final String	TAG						= BYBBaseRenderer.class.getCanonicalName();
	protected int				glWindowHorizontalSize	= 4000;
	protected int				glWindowVerticalSize	= 10000;
	protected int				glOffsetX				= 0;
	protected int				glOffsetY				= 0;

	protected short[]			mBufferToDraws;
	protected boolean			mAudioServiceIsBound;
	protected int				height;
	protected int				width;
	protected boolean			autoScaled				= false;
	public static final int		PCM_MAXIMUM_VALUE		= (Short.MAX_VALUE * 3 / 2);
	protected float				minimumDetectedPCMValue	= -5000000f;

	protected long				firstBufferDrawn		= 0;
	protected Context context;
	// ----------------------------------------------------------------------------------------
	public BYBBaseRenderer(){
		Log.d(TAG,"Constructor");
		this.context = null;
	}
	public BYBBaseRenderer(Context context){
		Log.d(TAG,"Constructor (context)");
		if(context != null) {
			this.context = context.getApplicationContext();
		}
	}
	public void setContext(Context ctx){
		context = ctx;
	}
	public void setup(Context context) {
		setContext(context);
	}
	// ----------------------------------------------------------------------------------------
	public void close(){}
	// ----------------------------------------------------------------------------------------
	public void setGlWindowHorizontalSize(int newX) {
		int maxlength = 0;
		if (mBufferToDraws != null)
			maxlength = mBufferToDraws.length;
		if (newX < 16 || (maxlength > 0 && newX> maxlength))
			return;
		this.glWindowHorizontalSize = newX;
		//Log.d(TAG, "SetGLHorizontalSize "+glWindowHorizontalSize);
	}
	// ----------------------------------------------------------------------------------------
	public void setGlWindowVerticalSize(int newY) {
		if (newY < 800 || newY > PCM_MAXIMUM_VALUE * 2)
			return;
		glWindowVerticalSize = newY;
		//Log.d(TAG, "SetGLVerticalSize "+glWindowVerticalSize);
	}
	// ----------------------------------------------------------------------------------------
	public int getGlWindowVerticalSize() {
		return glWindowVerticalSize;
	}
	// ----------------------------------------------------------------------------------------
	public int getGlWindowHorizontalSize() {
		return glWindowHorizontalSize;
	}
	// ----------------------------------------------------------------------------------------
	public void addToGlOffset(float dx, float dy ){
		if(context != null) {
			if (((BackyardBrainsApplication) context).getmAudioService() != null) {
				if (((BackyardBrainsApplication) context).getmAudioService().getMode() == AudioService.PLAYBACK_MODE &&
						!((BackyardBrainsApplication) context).getmAudioService().isAudioPlayerPlaying()) {
					glOffsetX += dx;
					glOffsetY += dy;
				}
			}
		}
	}
	// ----------------------------------------------------------------------------------------
	protected FloatBuffer getWaveformBuffer(short[] shortArrayToDraw) {
		if(context != null) {
			float[] arr;
				if (((BackyardBrainsApplication) context).getmAudioService() != null) {
				boolean bDrawFullArray = ((BackyardBrainsApplication) context).getmAudioService().getMode() == AudioService.PLAYBACK_MODE &&
						!((BackyardBrainsApplication) context).getmAudioService().isAudioPlayerPlaying();
				int micSize = ((BackyardBrainsApplication) context).getmAudioService().getMicListenerBufferSizeInSamples();
				int startIndex = 0;
				if (bDrawFullArray) {
					arr = new float[shortArrayToDraw.length * 2];
				} else {
					if (glWindowHorizontalSize > shortArrayToDraw.length) {
						setGlWindowHorizontalSize(shortArrayToDraw.length);
					}
					startIndex = shortArrayToDraw.length - glWindowHorizontalSize;// - micSize;
					arr = new float[(glWindowHorizontalSize) * 2];//+ micSize) * 2];
				}
				int j = 0; // index of arr
				try {
					for (int i = startIndex; i < shortArrayToDraw.length; i++) {
						arr[j++] = i;
						arr[j++] = shortArrayToDraw[i];
					}
				} catch (ArrayIndexOutOfBoundsException e) {
					Log.e(TAG, "Array size out of sync while building new waveform buffer");
				}
			} else {
				arr = new float[0];
			}
			return BYBUtils.getFloatBufferFromFloatArray(arr);
		}
		return null;
	}
	// ----------------------------------------------------------------------------------------
	public float getMinimumDetectedPCMValue() {
		return minimumDetectedPCMValue;
	}
	// ----------------------------------------------------------------------------------------
	protected long msToSamples(long timeSince) {
		return Math.round(44.1 * timeSince);
	}
	// ----------------------------------------------------------------------------------------
	protected boolean getCurrentAudio() {
		if(context != null) {
			if (((BackyardBrainsApplication) context).getmAudioService() != null) {
				mBufferToDraws = ((BackyardBrainsApplication) context).getmAudioService().getAudioBuffer();
				return true;
			}
		}
		return false;
	}
	// ----------------------------------------------------------------------------------------
	public boolean isAutoScaled() {
		return autoScaled;
	}
	// ----------------------------------------------------------------------------------------
	public void setAutoScaled(boolean isScaled) {
		autoScaled = isScaled;
	}
	// ----------------------------------------------------------------------------------------
	@Override
	public void onDrawFrame(GL10 gl) {
		// //Log.d(TAG, "onDrawFrame");
		// grab current audio from audioservice
		if (!getCurrentAudio()) {
			//Log.d(TAG, "AudioService is null!");
			return;
		}
		if (!BYBUtils.isValidAudioBuffer(mBufferToDraws )) {
			//Log.d(TAG, "Invalid audio buffer!");
			return;
		}
		preDrawingHandler();
		BYBUtils.glClear(gl);
		drawingHandler(gl);
		postDrawingHandler(gl);
	}
	// ----------------------------------------------------------------------------------------
	protected void preDrawingHandler() {
		// scale the right side to the number of data points we have
		if (mBufferToDraws.length < glWindowHorizontalSize) {
			setGlWindowHorizontalSize(mBufferToDraws.length);
		}
		setLabels(glWindowHorizontalSize);
	}
	// ----------------------------------------------------------------------------------------
	protected void setLabels(int samplesToShow) {
		setmVText();
		final float millisecondsInThisWindow = samplesToShow / 44100.0f * 1000 / 3;
		setMsText(millisecondsInThisWindow);
	}
	// ----------------------------------------------------------------------------------------
	protected void setmVText() {
		float yPerDiv = (float) getGlWindowVerticalSize() / 4.0f / 24.5f / 1000;
		setmVText(yPerDiv);
	}
	// ----------------------------------------------------------------------------------------
	public void setMsText(Float ms) {
		String msString = new DecimalFormat("#.#").format(ms);
		broadcastTextUpdate("BYBUpdateMillisecondsReciever", "millisecondsDisplayedString", msString + " ms");
	}
	// ----------------------------------------------------------------------------------------
	public void setmVText(Float ms) {
		String msString = new DecimalFormat("#.##").format(ms);
		broadcastTextUpdate("BYBUpdateMillivoltReciever", "millivoltsDisplayedString", msString + " mV");
	}
	// ----------------------------------------------------------------------------------------
	private void setMillivoltLabelPosition(int height) {
		if(context != null) {
			Intent i = new Intent();
			i.setAction("BYBMillivoltsViewSize");
			i.putExtra("millivoltsViewNewSize", height / 2);
			context.sendBroadcast(i);
		}
	}
	// ----------------------------------------------------------------------------------------
	private void broadcastTextUpdate(String action, String name, String data) {
		if(context != null) {
			Intent i = new Intent();
			i.setAction(action);
			i.putExtra(name, data);
			context.sendBroadcast(i);
		}
	}
	// ----------------------------------------------------------------------------------------
	protected void postDrawingHandler(GL10 gl) {}
	// ----------------------------------------------------------------------------------------
	protected void drawingHandler(GL10 gl) {}
	// ----------------------------------------------------------------------------------------
	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		////Log.d(TAG, "----------------------------------------- onSurfaceChanged begin");
		////Log.d(TAG, "width: " + width + "  height: " + height);
		this.width = width;
		this.height = height;
		setMillivoltLabelPosition(height);
		////Log.d(TAG, "***************************************** onSurfaceChanged end");
	}
	// ----------------------------------------------------------------------------------------
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		gl.glDisable(GL10.GL_DITHER);
		gl.glHint(GL10.GL_LINE_SMOOTH_HINT, GL10.GL_NICEST);
		gl.glEnable(GL10.GL_DEPTH_TEST);
	}
	// ----------------------------------------------------------------------------------------
	protected void initGL(GL10 gl, float xBegin, float xEnd, float scaledYBegin, float scaledYEnd) {

		float scaledOffsetX = 0;
		
		float scaledOffsetY = 0;
		if(context != null) {
		if (((BackyardBrainsApplication) context).getmAudioService() != null) {
			if(((BackyardBrainsApplication) context).getmAudioService().getMode() == AudioService.PLAYBACK_MODE && 
					!((BackyardBrainsApplication) context).getmAudioService().isAudioPlayerPlaying()){
				float scaleX = (xEnd - xBegin)/width;
				
				scaledOffsetX = glOffsetX*scaleX;
				
				float scaleY = (scaledYEnd - scaledYBegin)/height;
				
				scaledOffsetY = glOffsetY*scaleY;
			}
		}
		}
		//---check if offset will not draw the complete window. if it does, ease until not.
		if(xBegin - scaledOffsetX < 0 || xEnd - scaledOffsetX > mBufferToDraws.length){
//			glOffsetX *= 0.5;
//			scaledOffsetX *= 0.5;
//			if(Math.abs(glOffsetX) < 1){
//				glOffsetX = 0;
				scaledOffsetX = 0;
			//}
		}
//
//		if(scaledYBegin+ scaledOffsetY < 0 || scaledYEnd + scaledOffsetY > mBufferToDraws.length){
//			glOffsetX *= 0.5;
//			scaledOffsetX *= 0.5;
//			if(Math.abs(glOffsetX) < 1){
//				glOffsetX = 0;
//				scaledOffsetX = 0;
//			}
//		}
		//------
		
		gl.glViewport(0, 0, width, height);

		BYBUtils.glClear(gl);
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrthof(xBegin-scaledOffsetX, xEnd-scaledOffsetX, scaledYBegin+scaledOffsetY, scaledYEnd+scaledOffsetY, -1f, 1f);
		gl.glRotatef(0f, 0f, 0f, 1f);

		// Blackout, then we're ready to draw! \o/
		// mGL.glEnable(GL10.GL_TEXTURE_2D);
		gl.glClearColor(0f, 0f, 0f, 0.5f);
		gl.glClearDepthf(1.0f);
		gl.glEnable(GL10.GL_DEPTH_TEST);
		gl.glDepthFunc(GL10.GL_LEQUAL);
		gl.glEnable(GL10.GL_LINE_SMOOTH);
		gl.glHint(GL10.GL_LINE_SMOOTH_HINT, GL10.GL_NICEST);
		// Enable Blending
		gl.glEnable(GL10.GL_BLEND);
		// Specifies pixel arithmetic
		// gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
	}
	// ----------------------------------------------------------------------------------------
	protected void autoScaleCheck() {
		if (!isAutoScaled() && mBufferToDraws != null) {
			if(mBufferToDraws.length >0){
				autoSetFrame(mBufferToDraws);
			}
		}
	}
	// ----------------------------------------------------------------------------------------
	protected void autoSetFrame(short[] arrayToScaleTo) {
	//	//Log.d(TAG, "autoSetFrame");
		int theMax = 0;
		int theMin = 0;

		for (int i = 0; i < arrayToScaleTo.length; i++) {
			if (theMax < arrayToScaleTo[i]) theMax = arrayToScaleTo[i];
			if (theMin > arrayToScaleTo[i]) theMin = arrayToScaleTo[i];
		}

		int newyMax;
		if (theMax != 0 && theMin != 0) {

			if (Math.abs(theMax) >= Math.abs(theMin)) {
				newyMax = Math.abs(theMax) * 2;
			} else {
				newyMax = Math.abs(theMin) * 2;
			}
			if (-newyMax > getMinimumDetectedPCMValue()) {
			//	//Log.d(TAG, "Scaling window to " + -newyMax + " < y < " + newyMax);
				setGlWindowVerticalSize(newyMax * 2);
			}

		}
		setAutoScaled(true);
	}
	// ----------------------------------------------------------------------------------------
	protected void setGlWindow(GL10 gl, final int samplesToShow, final int lengthOfSampleSet) {
		if(context != null) {
			if (((BackyardBrainsApplication) context).getmAudioService() != null) {
				final int micBufferSize = ((BackyardBrainsApplication) context).getmAudioService().getMicListenerBufferSizeInSamples();
				final long lastTimestamp = ((BackyardBrainsApplication) context).getmAudioService().getLastSamplesReceivedTimestamp();
				final long timeSince = System.currentTimeMillis() - lastTimestamp;

				long xEnd = Math.min(lengthOfSampleSet, lengthOfSampleSet - micBufferSize + msToSamples(timeSince));
				long xBegin = Math.min(lengthOfSampleSet - glWindowHorizontalSize, xEnd - glWindowHorizontalSize);
				xBegin = Math.max(0, xBegin);
				initGL(gl, xBegin, xEnd, -getGlWindowVerticalSize() / 2, getGlWindowVerticalSize() / 2);
			}
		}
	}
	// ----------------------------------------------------------------------------------------
	public int glHeightToPixelHeight(float glHeight) {
		if (height <= 0) {
			 //Log.d(TAG, "Checked height and size was less than or equal to zero");
		}
		int ret = BYBUtils.map(glHeight, -getGlWindowVerticalSize() / 2, getGlWindowVerticalSize() / 2, height, 0);

		return ret;
	}

	// ----------------------------------------------------------------------------------------
	public float pixelHeightToGlHeight(float pxHeight) {
		return BYBUtils.map(pxHeight, height, 0, -getGlWindowVerticalSize() / 2, getGlWindowVerticalSize() / 2);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// ----------------------------------------- SETTINGS
	////////////////////////////////////////////////////////////////////////////////////////////////
	public void readSettings(SharedPreferences settings, String TAG) {
		if (settings != null) {
			setGlWindowHorizontalSize(settings.getInt(TAG + "_glWindowHorizontalSize",glWindowHorizontalSize));
			setGlWindowVerticalSize(settings.getInt(TAG + "_glWindowVerticalSize",glWindowVerticalSize));
			glOffsetX = settings.getInt(TAG + "_glOffsetX",glOffsetX);
			glOffsetY = settings.getInt(TAG + "_glOffsetY",glOffsetY);

			height = settings.getInt(TAG + "_height", height);
			width = settings.getInt(TAG + "_width", width);
			setAutoScaled(settings.getBoolean(TAG + "_autoScaled", autoScaled));
			minimumDetectedPCMValue = settings.getFloat(TAG + "_minimumDetectedPCMValue", minimumDetectedPCMValue);
		}
	}
	// ----------------------------------------------------------------------------------------
	public void saveSettings(SharedPreferences settings, String TAG) {
		if (settings != null) {
			final SharedPreferences.Editor editor = settings.edit();
			editor.putInt(TAG + "_glWindowHorizontalSize",glWindowHorizontalSize);
			editor.putInt(TAG + "_glWindowVerticalSize",glWindowVerticalSize);
			editor.putInt(TAG + "_glOffsetX",glOffsetX);
			editor.putInt(TAG + "_glOffsetY",glOffsetY);

			editor.putInt(TAG + "_height", height);
			editor.putInt(TAG + "_width", width);
			editor.putBoolean(TAG + "_autoScaled", autoScaled);
			editor.putFloat(TAG + "_minimumDetectedPCMValue", minimumDetectedPCMValue);
			editor.commit();

		}
	}
}
