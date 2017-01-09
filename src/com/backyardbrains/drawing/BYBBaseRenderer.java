package com.backyardbrains.drawing;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.text.DecimalFormat;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.backyardbrains.BYBConstants;
import com.backyardbrains.BYBGlUtils;
import com.backyardbrains.BackyardBrainsApplication;
import com.backyardbrains.BackyardBrainsMain;
import com.backyardbrains.audio.AudioService;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.opengl.GLSurfaceView;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;

import com.backyardbrains.BYBUtils;
import com.backyardbrains.view.ScaleListener;
import com.backyardbrains.view.SingleFingerGestureDetector;
import com.backyardbrains.view.TwoDimensionScaleGestureDetector;

public class BYBBaseRenderer implements GLSurfaceView.Renderer {
	private static final String	TAG						= "BYBBaseRenderer";
	protected int				glWindowHorizontalSize	= 4000;
	protected int				glWindowVerticalSize	= 10000;
	protected int				prevGlWindowHorizontalSize=4000;
	protected int				prevGlWindowVerticalSize = 10000;


	protected float 			focusX					= 0;
	protected float				scaledFocusX 			= 0;
	protected float				normalizedFocusX 		= 0;
	protected int 				focusedSample			= 0;

	protected boolean bZooming = false;
	protected boolean bPannig = false;
	protected float panningDx =0;

	protected short[]			mBufferToDraws;

	protected int				height;
	protected int				width;
	protected boolean			autoScaled				= false;
	public static final int		PCM_MAXIMUM_VALUE		= (Short.MAX_VALUE * 3);
	public static final int     MIN_GL_HORIZONTAL_SIZE  = 16;
	public static final int     MIN_GL_VERTICAL_SIZE    = 400;
	public static final int		MAX_NUM_SAMPLES			= 4410000; //100 seconds
	protected float				minimumDetectedPCMValue	= -5000000f;

	protected int startIndex =0;
	protected int endIndex =0;
	protected boolean bShowScalingAreaX = false;
	protected int scalingAreaStartX;
	protected int scalingAreaEndX;
	protected boolean bShowScalingAreaY = false;
	protected int scalingAreaStartY;
	protected int scalingAreaEndY;

	protected boolean bShowScalingInstructions = true;
	protected Context context;


	////////////////////////////////////////////////////////////////////////////////////////////////
	// ----------------------------------------- CONSTRUCTOR & SETUP
	////////////////////////////////////////////////////////////////////////////////////////////////
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
	////////////////////////////////////////////////////////////////////////////////////////////////
	// ----------------------------------------- SETTERS/GETTERS
	////////////////////////////////////////////////////////////////////////////////////////////////
	public void setGlWindowUnscaledHorizontalSize(float newX){
		setGlWindowHorizontalSize((int)(glWindowHorizontalSize * newX/(float)width));
	}
	public void setGlWindowUnscaledVerticalSize(float newY){
		setGlWindowVerticalSize((int)(glWindowVerticalSize * newY/(float)height));
	}
	// ----------------------------------------------------------------------------------------
	public void setGlWindowHorizontalSize(int newX) {
		if(newX < 0){return;}
		prevGlWindowHorizontalSize = glWindowHorizontalSize;
		int maxlength = 0;
		if (mBufferToDraws != null) {
			maxlength = Math.min(mBufferToDraws.length, MAX_NUM_SAMPLES);
			if (newX < MIN_GL_HORIZONTAL_SIZE){
				newX = MIN_GL_HORIZONTAL_SIZE;
			}
			if(maxlength > 0 && newX > maxlength){
				newX = maxlength;
			}
			this.glWindowHorizontalSize = newX;
		}
		Log.d(TAG, "SetGLHorizontalSize "+glWindowHorizontalSize);
	}
	// ----------------------------------------------------------------------------------------
	public void setGlWindowVerticalSize(int newY) {
		//Log.d(TAG, "SetGLVerticalSize newY: "+newY);
		if(newY < 0){
			return;
		}
		prevGlWindowVerticalSize = glWindowVerticalSize;
		String text = "hsize: " + newY;
		if (newY < MIN_GL_VERTICAL_SIZE){
			newY = MIN_GL_VERTICAL_SIZE;
		}
		if(newY > PCM_MAXIMUM_VALUE) {
			newY = PCM_MAXIMUM_VALUE;
		}
		glWindowVerticalSize = newY;
//		Log.d(TAG, "SetGLVerticalSize "+glWindowVerticalSize);
	}
	// ----------------------------------------------------------------------------------------
	public int getGlWindowVerticalSize() {
		return glWindowVerticalSize;
	}
	// ----------------------------------------------------------------------------------------
	public int getGlWindowHorizontalSize() {
		return glWindowHorizontalSize;
	}
	void broadcastDebugText(String text){
		if(context != null){
			Intent i = new Intent();
			i.setAction("updateDebugView");
			i.putExtra("text", text);
			context.sendBroadcast(i);
		}
	}
	// ----------------------------------------------------------------------------------------
	public int getSurfaceWidth(){
		return width;
	}
	// ----------------------------------------------------------------------------------------
	public int getSurfaceHeight(){
		return height;
	}
	// ----------------------------------------------------------------------------------------
	public void addToGlOffset(float dx, float dy ){
		if(context != null) {
			if (getIsPlaybackMode() && !getIsPlaying()) {
				bPannig = true;
				panningDx = dx;
				bZooming = false;
//				setStartIndex(startIndex + screenToSamplePos(dx));
			}
		}
	}
	private void setStartIndex(int si){
		startIndex = si;
		endIndex = startIndex + glWindowHorizontalSize;
	}
/*	private void constrainOffset(int samplesLenght) {
		if (getIsPlaybackMode() && !getIsPlaying()) {
			long playhead = getAudioService().getPlaybackProgress();
			float scaleX =  (getGlWindowHorizontalSize() / width);

			boolean bSetOffsetX = false;
			if (scaledOffsetX > playhead){
				scaledOffsetX = playhead;
				bSetOffsetX = true;
			}
			if(scaledOffsetX < -(samplesLenght - playhead)) {
				scaledOffsetX = -(samplesLenght - playhead);
				bSetOffsetX = true;
			}
			if(bSetOffsetX){
				glOffsetX = scaledOffsetX/scaleX;
			}
		}
	}
	public void resetGlOffset(){
		glOffsetX = 0;
		glOffsetY = 0;
		scaledOffsetX = 0;
		scaledOffsetY = 0;
	}//*/
	public void setScaleFocusX(float fx){
		focusX = fx;
		bZooming = true;
		bPannig = false;
//			normalizedFocusX = focusX/(float)width;
//		scaledFocusX = (float)prevGlWindowHorizontalSize*normalizedFocusX;//(float)width)*focusX;
//		focusedSample = startIndex + (int)Math.floor(scaledFocusX);
//		setStartIndex(startIndex + (int)((prevGlWindowHorizontalSize - glWindowHorizontalSize)*normalizedFocusX));
//		endIndex = startIndex + glWindowHorizontalSize;
//		bUseFocusForOffset = true;
//		resetGlOffset();
		//printDebugText();
	}
	public void showScalingAreaX(float start, float end){
		bShowScalingAreaX = true;
		scalingAreaStartX = screenToSampleScale(start);
		scalingAreaEndX = screenToSampleScale(end);
	}
	public void showScalingAreaY(float start, float end){
		bShowScalingAreaY = true;
		scalingAreaStartY = (int)pixelHeightToGlHeight(start);
		scalingAreaEndY = (int)pixelHeightToGlHeight(end);
	}
	public void hideScalingArea(){
		bShowScalingAreaX = false;
		bShowScalingAreaY = false;
	}
	private void setStartEndIndex(int arrayLength){
//		startIndex = 0;
		boolean bTempZooming = false;
		boolean bTempPanning = false;
		if(getAudioService().isPlaybackMode()){
			if(getAudioService().isAudioPlayerPlaying()) {
			long playbackProgress = getAudioService().getPlaybackProgress();
//			if(bUseFocusForOffset){
//				bUseFocusForOffset = false;
//				startIndex = (int)(focusedSample - (normalizedFocusX*getGlWindowHorizontalSize()));
//			}else {
			setStartIndex((int) playbackProgress - glWindowHorizontalSize);
//			}
//			if(!getAudioService().isAudioPlayerPlaying()) {
//				startIndex -= (int)scaledOffsetX;
			}else{
				if(bZooming){
					bZooming = false;
					bTempZooming = true;
					normalizedFocusX = focusX/(float)width;
					scaledFocusX = (float)prevGlWindowHorizontalSize*normalizedFocusX;//(float)width)*focusX;
					focusedSample = startIndex + (int)Math.floor(scaledFocusX);
					setStartIndex(startIndex + (int)((prevGlWindowHorizontalSize - glWindowHorizontalSize)*normalizedFocusX));
				}else
				if(bPannig){
					bTempPanning = true;
					bPannig = false;
					setStartIndex(startIndex - (int)Math.floor(((panningDx * glWindowHorizontalSize) / (float)width)));
				}
			}
			//printDebugText(arrayLength,bTempZooming, bTempPanning);
		}else{
			setStartIndex(arrayLength - glWindowHorizontalSize);
		}
		if(startIndex< -glWindowHorizontalSize ){
			setStartIndex( -glWindowHorizontalSize);
		}
		if(startIndex + getGlWindowHorizontalSize() > arrayLength){
			setStartIndex(arrayLength - getGlWindowHorizontalSize());
		}

//		endIndex = startIndex + glWindowHorizontalSize;
	}
	/*
	protected void printDebugText(int arrayLength,boolean bZoom , boolean bPan){//int arrayLength){
		String msg = "startIndex:   " +startIndex+"\n";
//		msg += "glOffsetX:          " +glOffsetX+"\n";
//		msg += "scaledOffsetX:      " +scaledOffsetX+"\n";
		msg += "arrayToDraw length: " + arrayLength + "\n";
		msg += "window H size:      " + glWindowHorizontalSize + "\n";
		msg += "scaleFactorX        " + scaleFactorX + "\n";
		msg += "scaleFactorY        " + scaleFactorY + "\n";
//		msg += "arrayLength + winH  " + (arrayLength + glWindowHorizontalSize) + "\n";
		msg += "playhead pos        " + getAudioService().getPlaybackProgress() + "\n";
//		msg += "normalized focusX   " + normalizedFocusX + "\n";
//		msg += "scaled focusX       " + scaledFocusX + "\n";
//		msg += "before: " + startIndex + "  " + getGlWindowHorizontalSize() + "  " + width + "\n";
		msg += "raw focusX (screen) " + focusX+"\n";

		msg += "focus samplePos     " + screenToSamplePos(focusX) +"\n";

		msg += "calc focus onscreen " + samplePosToScreen(screenToSamplePos(focusX)) + "\n";
//		msg += "after:m " + startIndex + "  " + getGlWindowHorizontalSize() + "  " + width + "\n";

		msg += "Zooming:			 " + (bZoom ?"TRUE":"FALSE")+"\n";
		msg += "Panning:			 " + (bPan ?"TRUE":"FALSE")+"\n";
		msg += "Playing:			 " + (getAudioService().isAudioPlayerPlaying() ?"TRUE":"FALSE");
		broadcastDebugText(msg);
	}
	//*/
	// ----------------------------------------------------------------------------------------
	protected FloatBuffer getWaveformBuffer(short[] shortArrayToDraw) {
		if(context != null) {
			float[] arr;
			if (getAudioService() != null) {

//				if (glWindowHorizontalSize > shortArrayToDraw.length) {
//					setGlWindowHorizontalSize(shortArrayToDraw.length);
//				}

//				constrainOffset(shortArrayToDraw.length);
				setStartEndIndex(shortArrayToDraw.length);
//				if(getAudioService().isPlaybackMode()) {
//					long playbackProgress = getAudioService().getPlaybackProgress();
//					startIndex = (int)playbackProgress - glWindowHorizontalSize;
//					if(!getAudioService().isAudioPlayerPlaying()) {
//						startIndex -= (int)scaledOffsetX;
//					}
//				}else{
//					startIndex = shortArrayToDraw.length - glWindowHorizontalSize;
//				}
				//printDebugText(shortArrayToDraw.length);
//				if(startIndex < 0){
//					startIndex = 0;
//				}
//				if(startIndex > shortArrayToDraw.length - glWindowHorizontalSize){
//					startIndex = shortArrayToDraw.length - glWindowHorizontalSize;
//				}

//				if(startIndex< -glWindowHorizontalSize ){
//					startIndex = -glWindowHorizontalSize;
//				}
//				if(startIndex + getGlWindowHorizontalSize() > shortArrayToDraw.length){
//					startIndex = shortArrayToDraw.length - getGlWindowHorizontalSize();
//				}
//
//				int endIndex = startIndex + glWindowHorizontalSize;

				arr = new float[(glWindowHorizontalSize) * 2];//+ micSize) * 2];

				int j = 0; // index of arr
				try {
					for (int i = startIndex; i < shortArrayToDraw.length && i < endIndex; i++) {
						arr[j++] = i-startIndex;
						if(i < 0){
							arr[j++] = 0;
						}else {
							arr[j++] = shortArrayToDraw[i];
						}
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
	protected boolean getCurrentAudio() {
		if (getAudioService() != null) {
			mBufferToDraws = getAudioService().getAudioBuffer();
			return true;
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
	// ----------------------------------------- LABELS
	protected void setLabels(int samplesToShow) {
		setmVText();
		final float millisecondsInThisWindow = samplesToShow / 44100.0f * 1000 / 2;
		setMsText(millisecondsInThisWindow);
	}
	// ----------------------------------------------------------------------------------------
	protected void setmVText() {
		float yPerDiv = (float) getGlWindowVerticalSize() / 4.0f / 24.5f / 1000 * BYBConstants.millivoltScale;
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
	////////////////////////////////////////////////////////////////////////////////////////////////
	// ----------------------------------------- DRAWING
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void onDrawFrame(GL10 gl) {


		if (!getCurrentAudio()) {
			Log.d(TAG, "cant get current audio buffer!");
			return;
		}
		if (!BYBUtils.isValidAudioBuffer(mBufferToDraws )) {
			Log.d(TAG, "Invalid audio buffer!");
			return;
		}
		preDrawingHandler();
		BYBGlUtils.glClear(gl);
		drawingHandler(gl);
		postDrawingHandler(gl);

	}
	// ----------------------------------------------------------------------------------------
	protected void preDrawingHandler() {
		setLabels(glWindowHorizontalSize);
	}
	// ----------------------------------------------------------------------------------------
	protected void postDrawingHandler(GL10 gl) {
		if(getIsPlaybackMode() && !getIsPlaying()) {
			float playheadDraw = getAudioService().getPlaybackProgress()-startIndex;

			BYBGlUtils.drawGlLine(gl, playheadDraw, -getGlWindowVerticalSize(),playheadDraw, getGlWindowVerticalSize(),0x00FFFFFF);
//			BYBGlUtils.drawGlLine(gl, scaledFocusX, -getGlWindowVerticalSize(), scaledFocusX, getGlWindowVerticalSize(), 0xFFFF00FF);

		}
		if(bShowScalingAreaX || bShowScalingAreaY){
			gl.glEnable(GL10.GL_BLEND);
			// Specifies pixel arithmetic
			gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
			if(bShowScalingAreaX) {
				BYBGlUtils.drawRectangle(gl, scalingAreaStartX, -getGlWindowVerticalSize(), scalingAreaEndX - scalingAreaStartX, getGlWindowVerticalSize() * 2, 0xFFFFFF33);
			}else{
				BYBGlUtils.drawRectangle(gl, 0, scalingAreaStartY, getGlWindowHorizontalSize(), scalingAreaEndY, 0xFFFFFF33);
			}
//			BYBGlUtils.drawGlLine(gl, scalingAreaStart, -getGlWindowVerticalSize(),scalingAreaStart, getGlWindowVerticalSize(),0xFF8F06FF);
//			BYBGlUtils.drawGlLine(gl, scalingAreaEnd, -getGlWindowVerticalSize(),scalingAreaEnd, getGlWindowVerticalSize(),0xFF8F06FF);
			gl.glDisable(GL10.GL_BLEND);
		}
//		BYBGlUtils.drawGlLine(gl, screenToSampleScale(width/4),pixelHeightToGlHeight( height - 20), screenToSampleScale(3*width/4),pixelHeightToGlHeight( height - 20),0xFFFFFFFF);
	}
	// ----------------------------------------------------------------------------------------
	protected void drawingHandler(GL10 gl) {}
	// ----------------------------------------------------------------------------------------
	////////////////////////////////////////////////////////////////////////////////////////////////
	// ----------------------------------------- SURFACE LISTENERS
	////////////////////////////////////////////////////////////////////////////////////////////////
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
	////////////////////////////////////////////////////////////////////////////////////////////////
	// ----------------------------------------- GL
	////////////////////////////////////////////////////////////////////////////////////////////////
	protected void initGL(GL10 gl, float xBegin, float xEnd, float scaledYBegin, float scaledYEnd) {

//		updateScaledOffset(xBegin,xEnd, scaledYBegin, scaledYEnd);

		gl.glViewport(0, 0, width, height);

		BYBGlUtils.glClear(gl);
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrthof(xBegin, xEnd, scaledYBegin, scaledYEnd, -1f, 1f);
		gl.glRotatef(0f, 0f, 0f, 1f);

		gl.glClearColor(0f, 0f, 0f, 1.0f);
		gl.glClearDepthf(1.0f);
		gl.glEnable(GL10.GL_DEPTH_TEST);
		gl.glDepthFunc(GL10.GL_LEQUAL);
		gl.glEnable(GL10.GL_LINE_SMOOTH);
		gl.glHint(GL10.GL_LINE_SMOOTH_HINT, GL10.GL_NICEST);
		// Enable Blending
//		gl.glEnable(GL10.GL_BLEND);
//		// Specifies pixel arithmetic
//		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
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
		initGL(gl, 0, samplesToShow, -getGlWindowVerticalSize() / 2, getGlWindowVerticalSize() / 2);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// ----------------------------------------- UTILS
	////////////////////////////////////////////////////////////////////////////////////////////////
//	private void updateScaledOffset(float xBegin, float xEnd,float yBegin, float yEnd){
//		scaledOffsetX = 0;
//		scaledOffsetY = 0;
//		if(context != null) {
//			if (getAudioService() != null) {
//				if(getIsPlaybackMode() && !getIsPlaying()) {
//					float scaleX = ((float)(xEnd - xBegin)) / (float)width;
//					scaledOffsetX = glOffsetX * scaleX;
//					float scaleY = ((float)(yEnd - yBegin)) / (float)height;
//					scaledOffsetY = glOffsetY * scaleY;
//				}else{
//					resetGlOffset();
//				}
//			}
//		}
//	}
	private void broadcastTextUpdate(String action, String name, String data) {
		if(context != null) {
			Intent i = new Intent();
			i.setAction(action);
			i.putExtra(name, data);
			context.sendBroadcast(i);
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
	public int screenToSampleScale(float screenPos){
		float normalizedScreenPos = screenPos/(float)width;
		return (int)(normalizedScreenPos*getGlWindowHorizontalSize());
	}
	public int screenToSamplePos(float screenPos){
//		float normalizedScreenPos = screenPos/(float)width;
//	return startIndex + (int)(normalizedScreenPos*getGlWindowHorizontalSize());
	return startIndex + screenToSampleScale(screenPos);
	}
	public float samplePosToScreen(int samplePos){
		float normalizedScreenPos = (float)(samplePos - startIndex)/(float)getGlWindowHorizontalSize();
		return normalizedScreenPos*width;
	}
	// ----------------------------------------------------------------------------------------
	protected long msToSamples(long timeSince) {
		return Math.round(44.1 * timeSince);
	}

	public AudioService getAudioService(){
		if(context!=null) {
			BackyardBrainsApplication app = ((BackyardBrainsApplication) context.getApplicationContext());
			if (app != null) {
				return app.getmAudioService();
			}
		}
		return null;
	}
	public boolean getIsRecording(){
		if(getAudioService() != null){
			return getAudioService().isRecording();
		}
		return false;
	}
	public boolean getIsPlaybackMode(){
		if(getAudioService() != null){
			return getAudioService().isPlaybackMode();
		}
		return false;
	}
	public boolean getIsPlaying(){
		if(getAudioService() != null){
			return getAudioService().isAudioPlayerPlaying();
		}
		return false;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// ----------------------------------------- SETTINGS
	////////////////////////////////////////////////////////////////////////////////////////////////
	public void readSettings(SharedPreferences settings, String TAG) {
		if (settings != null) {
			setGlWindowHorizontalSize(settings.getInt(TAG + "_glWindowHorizontalSize",glWindowHorizontalSize));
			setGlWindowVerticalSize(settings.getInt(TAG + "_glWindowVerticalSize",glWindowVerticalSize));
//			glOffsetX = settings.getFloat(TAG + "_glOffsetX",glOffsetX);
//			glOffsetY = settings.getFloat(TAG + "_glOffsetY",glOffsetY);


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
//			editor.putFloat(TAG + "_glOffsetX",glOffsetX);
//			editor.putFloat(TAG + "_glOffsetY",glOffsetY);

			editor.putInt(TAG + "_height", height);
			editor.putInt(TAG + "_width", width);
			editor.putBoolean(TAG + "_autoScaled", autoScaled);
			editor.putFloat(TAG + "_minimumDetectedPCMValue", minimumDetectedPCMValue);
			editor.commit();

		}
	}
}
