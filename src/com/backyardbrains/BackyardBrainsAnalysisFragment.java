package com.backyardbrains;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import com.backyardbrains.analysis.BYBAnalysisType;
import com.backyardbrains.analysis.BYBSignalAnalysis;
import com.backyardbrains.audio.RecordingReader;
import com.backyardbrains.drawing.AutoCorrelationRenderer;
import com.backyardbrains.drawing.AverageSpikeRenderer;
import com.backyardbrains.drawing.BYBAnalysisBaseRenderer;
import com.backyardbrains.drawing.BYBBaseRenderer;
import com.backyardbrains.drawing.ContinuousGLSurfaceView;
import com.backyardbrains.drawing.CrossCorrelationRenderer;
import com.backyardbrains.drawing.FindSpikesRenderer;
import com.backyardbrains.drawing.ISIRenderer;

import com.backyardbrains.drawing.WaitRenderer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class BackyardBrainsAnalysisFragment extends Fragment {

	private static final String			TAG							= BackyardBrainsAnalysisFragment.class.getCanonicalName();

	protected GLSurfaceView	mAndroidSurface				= null;
	private FrameLayout					mainscreenGLLayout;
	private SharedPreferences			settings					= null;
	private Context						context						= null;
	private BYBSignalAnalysis 			signalAnalysis;
	protected int						currentAnalyzer				= BYBAnalysisType.BYB_ANALYSIS_NONE;

	private RecordingReader				reader						= null;
	private AudioFileReadListener		audioFileReadListener;
	private SignalAnalysisDoneListener 	signalAnalysisDoneListener;
	private boolean						bFileLoaded					= false;
	private boolean 					bAnalysisDone				= false;




	// ----------------------------------------------------------------------------------------
	public BackyardBrainsAnalysisFragment(Context context) {
		super();
		this.context = context.getApplicationContext();
		Log.d("BackyardBrainsAnalysisFragment", "Constructor");
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- FRAGMENT LIFECYCLE
	// -----------------------------------------------------------------------------------------------------------------------------
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (context != null) {

		} else {
			Log.d(TAG, "onCreate failed, context == null");
		}
	}

	// ----------------------------------------------------------------------------------------
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.analysis_layout, container, false);
		getSettings();
		mainscreenGLLayout = (FrameLayout) rootView.findViewById(R.id.analysisGlContainer);
		Log.d(TAG, "onCreateView");

		return rootView;
	}

	@Override
	public void onStart() {
		super.onStart();
		reassignSurfaceView(currentAnalyzer);
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onStop() {
		super.onStop();
		destroySurfaceView();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- GL RENDERING
	// -----------------------------------------------------------------------------------------------------------------------------

	// ----------------------------------------------------------------------------------------
	
	public void setRenderer(int i) {
		if (i >= 0 && i <= 4) {
			currentAnalyzer = i;
			reassignSurfaceView(currentAnalyzer);
		}
	}

	// ----------------------------------------------------------------------------------------
	protected void destroySurfaceView() {
		mAndroidSurface = null;
		if (mainscreenGLLayout != null) {
			mainscreenGLLayout.removeAllViews();
			mainscreenGLLayout = null;
		}
	}
	// ----------------------------------------------------------------------------------------
	protected void reassignSurfaceView(int renderer) {
		if (context != null ) {
			
			mAndroidSurface = null;
			mainscreenGLLayout.removeAllViews();
			if(bFileLoaded && bAnalysisDone){
			switch (renderer) {
			case BYBAnalysisType.BYB_ANALYSIS_FIND_SPIKES :
				setGlSurface(new FindSpikesRenderer(context), true);
				break;
			case BYBAnalysisType.BYB_ANALYSIS_AUTOCORRELATION:
				setGlSurface(new AutoCorrelationRenderer(context), true);
				break;
			case BYBAnalysisType.BYB_ANALYSIS_CROSS_CORRELATION:
				setGlSurface(new CrossCorrelationRenderer(context), true);
				break;
			case BYBAnalysisType.BYB_ANALYSIS_ISI:
				setGlSurface(new ISIRenderer(context), true);
				break;
			case BYBAnalysisType.BYB_ANALYSIS_AVERAGE_SPIKE:
				setGlSurface(new AverageSpikeRenderer(context), true);
				break;
			case BYBAnalysisType.BYB_ANALYSIS_NONE:
				setGlSurface(new WaitRenderer(context), false);
				break;
			}
			}else{
				setGlSurface(new WaitRenderer(context), false);
			}
		}
		mainscreenGLLayout.addView(mAndroidSurface);

		// enableUiForActivity();
		Log.d(getClass().getCanonicalName(), "Reassigned AnalysisGLSurfaceView");
	}

	// ----------------------------------------------------------------------------------------
	protected void setGlSurface(BYBAnalysisBaseRenderer renderer, boolean bSetOnDemand) {
		if (context != null && renderer != null) {
			if (mAndroidSurface != null) {
				mAndroidSurface = null;
			}
			mAndroidSurface = new GLSurfaceView(context);
			mAndroidSurface.setRenderer(renderer);
			if(bSetOnDemand){
				mAndroidSurface.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
			}
			Log.d(TAG, "setGLSurface oK.");
		} else {
			Log.d(TAG, "setGLSurface failed. Context == null.");
		}
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- TOUCH
	// -----------------------------------------------------------------------------------------------------------------------------
	public boolean onTouchEvent(MotionEvent event) {

		return mAndroidSurface.onTouchEvent(event);
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- SETTINGS
	// -----------------------------------------------------------------------------------------------------------------------------
	private void getSettings() {
		if (settings == null) {
			// settings = (
			// context).getPreferences(BackyardBrainsMain.MODE_PRIVATE);
		}
	}

	public void load(String filePath, int analysisType) {
		load(new File(filePath), analysisType);
	}

	public void load(File f, int analysisType) {
		if (context != null) {
			if (f.exists()) {
				reader = null;
				registerAudioFileReadReceiver(true);
				reader = new RecordingReader(f, context);
				bFileLoaded = false;
				bAnalysisDone = false;
				if(signalAnalysis != null){
					signalAnalysis.resetAnalyzer();
					signalAnalysis = null;
				}
				currentAnalyzer = analysisType;
				reassignSurfaceView(BYBAnalysisType.BYB_ANALYSIS_NONE);
			} else {
				Log.d("AudioFilePlayer", "Cant load file: it doent exist!!");
			}
		}
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- BROADCAST RECEIVERS CLASS
	// -----------------------------------------------------------------------------------------------------------------------------
	
	private class AudioFileReadListener extends BroadcastReceiver {
		@Override
		public void onReceive(android.content.Context context, android.content.Intent intent) {
			Log.d("AudioFileReadListener", "onReceive");
			
			bFileLoaded = true;

			registerSignalAnalysisDoneReceiver(true);
			signalAnalysis = new BYBSignalAnalysis(context, reader.getDataShorts(), currentAnalyzer);
			
			registerAudioFileReadReceiver(false);
		};
	}
	private class SignalAnalysisDoneListener extends BroadcastReceiver {
		@Override
		public void onReceive(android.content.Context context, android.content.Intent intent) {
			registerSignalAnalysisDoneReceiver(false);
			Log.d("SignalAnalysisDoneListener", "onReceive");
			if(intent.getIntExtra("analysisType", BYBAnalysisType.BYB_ANALYSIS_NONE ) == currentAnalyzer){
				bAnalysisDone = true;
				reassignSurfaceView(currentAnalyzer);
			}else{
				Log.d("SignalAnalysisDoneListener", " recieved analysis type differs from current analysis type	");
			}

		};
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- BROADCAST RECEIVERS TOGGLES
	// -----------------------------------------------------------------------------------------------------------------------------

	private void registerAudioFileReadReceiver(boolean reg) {
		if (reg) {
			IntentFilter intentFilter = new IntentFilter("BYBAudioFileRead");
			audioFileReadListener = new AudioFileReadListener();
			context.registerReceiver(audioFileReadListener, intentFilter);
		} else {
			context.unregisterReceiver(audioFileReadListener);
			audioFileReadListener = null;
		}
	}
	private void registerSignalAnalysisDoneReceiver(boolean reg) {
		if (reg) {
			IntentFilter intentFilter = new IntentFilter("BYBSignalAnalysisDone");
			signalAnalysisDoneListener = new SignalAnalysisDoneListener();
			context.registerReceiver(signalAnalysisDoneListener, intentFilter);
		} else {
			context.unregisterReceiver(signalAnalysisDoneListener);
			signalAnalysisDoneListener = null;
		}
	}
}
