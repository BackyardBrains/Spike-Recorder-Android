package com.backyardbrains;

import com.backyardbrains.audio.AudioService;
import com.backyardbrains.drawing.BYBBaseRenderer;
import com.backyardbrains.drawing.ContinuousGLSurfaceView;
import com.backyardbrains.drawing.ThresholdRenderer;
import com.backyardbrains.drawing.WaveformRenderer;
import com.backyardbrains.view.ScaleListener;
import com.backyardbrains.view.TwoDimensionScaleGestureDetector;
import com.backyardbrains.view.UIFactory;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class BackyardBrainsOscilloscopeFragment extends Fragment {

	// public static final String ARG_SECTION_NUMBER = "section_number";
	// *
	private static final String					TAG					= BackyardBrainsOscilloscopeFragment.class.getCanonicalName();

	public static final int						WAVE				= 0;
	public static final int						THRESH				= 1;
	protected ContinuousGLSurfaceView			mAndroidSurface		= null;
	private boolean								isRecording			= false;
	private FrameLayout							mainscreenGLLayout;
	private SharedPreferences					settings			= null;
	private Context								context				= null;
	private WaveformRenderer					waveRenderer		= null;
	private ThresholdRenderer					threshRenderer		= null;
	protected TwoDimensionScaleGestureDetector	mScaleDetector;
	private ScaleListener						mScaleListener;
	private boolean								bRenderersCreated	= false;
	protected int								currentRenderer		= -1;
//	protected AudioService audioService = null;
	// ----------------------------------------------------------------------------------------
	public BackyardBrainsOscilloscopeFragment(Context context){//, AudioService audioService) {
		super();
		setContext(context);
		//this.audioService = audioService;
		Log.d("BackyardBrainsOscilloscopeFragment", "Constructor");
//		if(this.audioService == null){
//		Log.d("BackyardBrainsOscilloscopeFragment","null audioservice");
//		}
	}

	// ----------------------------------------------------------------------------------------
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (context != null) {
			mScaleListener = new ScaleListener();
			mScaleDetector = new TwoDimensionScaleGestureDetector(context, mScaleListener);
			createRenderers();
			// reassignSurfaceView();
			enableUiForActivity();
		} else {
			Log.d(TAG, "onCreate failed, context == null");
		}
	}

	// ----------------------------------------------------------------------------------------
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.backyard_main, container, false);
		// Log.d("BackyardBrainsOscilloscopeFragment", "onCreateView");
		getSettings();
		mainscreenGLLayout = (FrameLayout) rootView.findViewById(R.id.glContainer);

		UIFactory.getUi().setupLabels(rootView);
		UIFactory.setupMsLineView(this, rootView);
		UIFactory.setupRecordingButtons(this, rootView);
		// UIFactory.setupSampleSlider(this);
		return rootView;

	}

	// ----------------------------------------------------------------------------------------
	public void setContext(Context context) {
		this.context = context.getApplicationContext();
	}

	// ----------------------------------------------------------------------------------------
	private void createRenderers() {
		if (waveRenderer != null) {
			waveRenderer = null;
		}
		if (threshRenderer != null) {
			threshRenderer = null;
		}
		waveRenderer = new WaveformRenderer(context);//, audioService);
		threshRenderer = new ThresholdRenderer(context);//, audioService);
		bRenderersCreated = true;
	}

	// ----------------------------------------------------------------------------------------
	public void setRenderer(int i) {
		if (i == WAVE || i == THRESH) {
			currentRenderer = i;
			reassignSurfaceView();
		}
	}

	// ----------------------------------------------------------------------------------------
	protected void reassignSurfaceView() {
		if (context != null && bRenderersCreated) {
			mAndroidSurface = null;
			mainscreenGLLayout.removeAllViews();
			if (currentRenderer < 1) {
				setGlSurface(waveRenderer);
				mScaleListener.setRenderer(waveRenderer);
				currentRenderer = WAVE;
			} else {
				setGlSurface(threshRenderer);
				mScaleListener.setRenderer(threshRenderer);
				currentRenderer = THRESH;
			}
			mainscreenGLLayout.addView(mAndroidSurface);

			readSettings();

			// enableUiForActivity();
			Log.d(getClass().getCanonicalName(), "Reassigned OscilloscopeGLSurfaceView");
		}
	}

	// ----------------------------------------------------------------------------------------
	protected void enableUiForActivity() {
		UIFactory.showRecordingButtons();
		// UIFactory.hideSampleSliderBox(this);
	}

	// ----------------------------------------------------------------------------------------
	protected void setGlSurface(BYBBaseRenderer renderer) {
		if (context != null) {
			if (mAndroidSurface != null) {
				mAndroidSurface = null;
			}
			mAndroidSurface = new ContinuousGLSurfaceView(context, renderer);
			Log.d(TAG, "setGLSurface oK.");
		} else {
			Log.d(TAG, "setGLSurface failed. Context == null.");
		}
	}

	// ----------------------------------------------------------------------------------------
	// *
	@Override
	public void onStart() {
		readSettings();
		reassignSurfaceView();
		super.onStart();
	}

	// ----------------------------------------------------------------------------------------
	@Override
	public void onResume() {
		UIFactory.getUi().registerReceivers(this);
		mAndroidSurface.onResume();
		// reassignSurfaceView();
		// bindAudioService(true);
		enableUiForActivity();
		readSettings();
		super.onResume();
	}

	// ----------------------------------------------------------------------------------------
	@Override
	public void onPause() {
		// mAndroidSurface = null;
		mAndroidSurface.onPause();
		UIFactory.getUi().unregisterReceivers(this);
		UIFactory.hideRecordingButtons();
		saveSettings();
		super.onPause();
	}

	// ----------------------------------------------------------------------------------------
	@Override
	public void onStop() {
		saveSettings();
		super.onStop();
		mAndroidSurface = null;
		// finish();
	}

	// ----------------------------------------------------------------------------------------
	// *
	public boolean onTouchEvent(MotionEvent event) {
		mScaleDetector.onTouchEvent(event);
		return mAndroidSurface.onTouchEvent(event);
	}

	// ----------------------------------------------------------------------------------------
	// // */
	public void toggleRecording() {
		UIFactory.getUi().toggleRecording(this, isRecording);
		isRecording = !isRecording;
	}

	// ----------------------------------------------------------------------------------------
	public void setDisplayedMilliseconds(Float ms) {
		UIFactory.getUi().setDisplayedMilliseconds(ms);
	}

	// ----------------------------------------------------------------------------------------
	private void getSettings() {
		if (settings == null) {
		//	settings =  ( context).getPreferences(BackyardBrainsMain.MODE_PRIVATE);
		}
	}

	// ----------------------------------------------------------------------------------------
	// */
	// *
	protected void readSettings() {
		if (settings != null) {
			waveRenderer.setAutoScaled(settings.getBoolean("waveRendererAutoscaled", waveRenderer.isAutoScaled()));
			waveRenderer.setGlWindowHorizontalSize(settings.getInt("waveRendererGlWindowHorizontalSize", waveRenderer.getGlWindowHorizontalSize()));
			waveRenderer.setGlWindowVerticalSize(settings.getInt("waveRendererGlWindowVerticalSize", waveRenderer.getGlWindowVerticalSize()));

			threshRenderer.setAutoScaled(settings.getBoolean("threshRendererAutoscaled", threshRenderer.isAutoScaled()));
			threshRenderer.setGlWindowHorizontalSize(settings.getInt("threshRendererGlWindowHorizontalSize", threshRenderer.getGlWindowHorizontalSize()));
			threshRenderer.setGlWindowVerticalSize(settings.getInt("threshRendererGlWindowVerticalSize", threshRenderer.getGlWindowVerticalSize()));
		}
	}

	// ----------------------------------------------------------------------------------------
	protected void saveSettings() {
		if (settings != null) {
			final SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("waveRendererAutoscaled", waveRenderer.isAutoScaled());
			editor.putInt("waveRendererGlWindowHorizontalSize", waveRenderer.getGlWindowHorizontalSize());
			editor.putInt("waveRendererGlWindowVerticalSize", waveRenderer.getGlWindowVerticalSize());

			editor.putBoolean("threshRendererAutoscaled", threshRenderer.isAutoScaled());
			editor.putInt("threshRendererGlWindowHorizontalSize", threshRenderer.getGlWindowHorizontalSize());
			editor.putInt("threshRendererGlWindowVerticalSize", threshRenderer.getGlWindowVerticalSize());
			editor.commit();
		}
	}
}
