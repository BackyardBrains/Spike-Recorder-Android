package com.backyardbrains;

import com.backyardbrains.audio.AudioService.OnTabSelectedListener;
import com.backyardbrains.drawing.BYBBaseRenderer;
import com.backyardbrains.drawing.ContinuousGLSurfaceView;
import com.backyardbrains.drawing.ThresholdRenderer;
import com.backyardbrains.drawing.WaveformRenderer;
import com.backyardbrains.view.ScaleListener;
import com.backyardbrains.view.TwoDimensionScaleGestureDetector;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout.LayoutParams;

public class BackyardBrainsOscilloscopeFragment extends Fragment {

// public static final String ARG_SECTION_NUMBER = "section_number";
// *
	private static final String					TAG					= BackyardBrainsOscilloscopeFragment.class.getCanonicalName();

	public static final int						WAVE				= 0;
	public static final int						THRESH				= 1;
	public static final int						LIVE_MODE			= 0;
	public static final int						PLAYBACK_MODE		= 1;

	private int									mode				= 0;
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

	private TextView							msView;
	private TextView							mVView;
	private UpdateMillisecondsReciever			upmillirec;
	private SetMillivoltViewSizeReceiver		milliVoltSize;
	private UpdateMillivoltReciever				upmillivolt;
	private ShowRecordingButtonsReceiver		showRecordingButtonsReceiver;
	private ShowCloseButtonReceiver				showCloseButtonReceiver;
	private OnTabSelectedListener 				tabSelectedListener;

// ----------------------------------------------------------------------------------------
	public BackyardBrainsOscilloscopeFragment(Context context) {
		super();
		this.context = context.getApplicationContext();
		mode = LIVE_MODE;
		Log.d("BackyardBrainsOscilloscopeFragment", "Constructor");
	}
// -----------------------------------------------------------------------------------------------------------------------------
// ----------------------------------------- FRAGMENT LIFECYCLE
// -----------------------------------------------------------------------------------------------------------------------------
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (context != null) {
			mScaleListener = new ScaleListener();
			mScaleDetector = new TwoDimensionScaleGestureDetector(context, mScaleListener);
			createRenderers();
			// reassignSurfaceView();

		} else {
			Log.d(TAG, "onCreate failed, context == null");
		}
	}

// ----------------------------------------------------------------------------------------
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.backyard_main, container, false);
		getSettings();
		mainscreenGLLayout = (FrameLayout) rootView.findViewById(R.id.glContainer);
		Log.d(TAG, "onCreateView");
		setupLabels(rootView);
		setupMsLineView(rootView);
		setupRecordingButtons(rootView);
		return rootView;

	}

// ----------------------------------------------------------------------------------------
	@Override
	public void onStart() {
		Log.d(TAG, "onStart");
		readSettings();
		reassignSurfaceView();
		enableUiForActivity();
		super.onStart();
	}
// ----------------------------------------------------------------------------------------
	@Override
	public void onResume() {
		Log.d(TAG, "onResume");
		registerReceivers();
		mAndroidSurface.onResume();

		readSettings();
		super.onResume();
	}
// ----------------------------------------------------------------------------------------
	@Override
	public void onPause() {
		Log.d(TAG, "onPause");
		mAndroidSurface.onPause();
		unregisterReceivers();
		hideRecordingButtons();

		saveSettings();
		super.onPause();
	}
// ----------------------------------------------------------------------------------------
	@Override
	public void onStop() {
		Log.d(TAG, "onStop");
		saveSettings();
		super.onStop();
		mAndroidSurface = null;
	}
// -----------------------------------------------------------------------------------------------------------------------------
// ----------------------------------------- GL RENDERING
// -----------------------------------------------------------------------------------------------------------------------------
	private void createRenderers() {
		if (waveRenderer != null) {
			waveRenderer = null;
		}
		if (threshRenderer != null) {
			threshRenderer = null;
		}
		waveRenderer = new WaveformRenderer(context);
		threshRenderer = new ThresholdRenderer(context);
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

			Log.d(getClass().getCanonicalName(), "Reassigned OscilloscopeGLSurfaceView");
		}
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

// -----------------------------------------------------------------------------------------------------------------------------
// ----------------------------------------- TOUCH
// -----------------------------------------------------------------------------------------------------------------------------
	public boolean onTouchEvent(MotionEvent event) {
		mScaleDetector.onTouchEvent(event);
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

// ----------------------------------------------------------------------------------------
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

// -----------------------------------------------------------------------------------------------------------------------------
// ----------------------------------------- UI
// -----------------------------------------------------------------------------------------------------------------------------

	public void showCloseButton() {
		ImageButton mCloseButton = (ImageButton) getView().findViewById(R.id.closeButton);
		if (mCloseButton != null) {
			mCloseButton.setVisibility(View.VISIBLE);
			hideRecordingButtons();
			mode = PLAYBACK_MODE;
		}
	}

// ----------------------------------------------------------------------------------------
	public void hideCloseButton() {
		ImageButton mCloseButton = (ImageButton) getView().findViewById(R.id.closeButton);
		if (mCloseButton != null) {
			if (mCloseButton.getVisibility() == View.VISIBLE) {
				mCloseButton.setVisibility(View.GONE);
				showRecordingButtons();
			}
		}
	}

// ----------------------------------------------------------------------------------------
	public void toggleCloseButton() {
		ImageButton mCloseButton = (ImageButton) getView().findViewById(R.id.closeButton);
		if (mCloseButton != null) {
			if (mCloseButton.getVisibility() == View.VISIBLE) {
				mCloseButton.setVisibility(View.GONE);
				showRecordingButtons();
			} else {
				mCloseButton.setVisibility(View.VISIBLE);
				mode = PLAYBACK_MODE;
				hideRecordingButtons();
			}
		}
		Log.d(TAG, "toggleCloseButton");
	}

// ----------------------------------------------------------------------------------------
	public void hideRecordingButtons() {// BackyardBrainsOscilloscopeFragment
		// context) {
// if(oscilloscopeContext != null){
		ImageButton mRecordButton = (ImageButton) getView().findViewById(R.id.recordButton);

		if (mRecordButton != null) {
			mRecordButton.setVisibility(View.GONE);
		}
// }
	}

// ----------------------------------------------------------------------------------------
	public void showRecordingButtons() {// BackyardBrainsOscilloscopeFragment
		// context) {
// if(oscilloscopeContext != null){
		ImageButton mRecordButton = (ImageButton) getView().findViewById(R.id.recordButton);

		if (mRecordButton != null) {
			mRecordButton.setVisibility(View.VISIBLE);
			mode = LIVE_MODE;
		}

	}// ----------------------------------------------------------------------------------------

	protected void enableUiForActivity() {
		if (mode == LIVE_MODE) {
			showRecordingButtons();
		} else {
			showCloseButton();
		}
// UIFactory.hideSampleSliderBox(this);
	}

// ----------------------------------------------------------------------------------------
	public void toggleRecording() {
		// UIFactory.getUi().toggleRecording(this, isRecording);
		ShowRecordingAnimation anim = new ShowRecordingAnimation(getActivity(), isRecording);
		try {
			View tapToStopRecView = getView().findViewById(R.id.TapToStopRecordingTextView);
			anim.run();
			Intent i = new Intent();
			i.setAction("BYBToggleRecording");
			context.sendBroadcast(i);
			if (isRecording == false) {
				tapToStopRecView.setVisibility(View.VISIBLE);
			} else {
				tapToStopRecView.setVisibility(View.GONE);
			}
		} catch (RuntimeException e) {
			Toast.makeText(context.getApplicationContext(), "No SD Card is available. Recording is disabled", Toast.LENGTH_LONG).show();
		}
		isRecording = !isRecording;
	}

// -----------------------------------------------------------------------------------------------------------------------------
// ----------------------------------------- UI SETUPS
// -----------------------------------------------------------------------------------------------------------------------------
	public void setupLabels(View v) {
		msView = (TextView) v.findViewById(R.id.millisecondsView);
		mVView = (TextView) v.findViewById(R.id.mVLabelView);
	}

// ----------------------------------------------------------------------------------------
	public void setDisplayedMilliseconds(Float ms) {
		msView.setText(ms.toString());
	}

// ----------------------------------------------------------------------------------------
	public void setupMsLineView(View v) {
		ImageView msLineView = new ImageView(getActivity());
		Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.msline);
		int width = v.getWidth() / 3;
		int height = 2;
		Bitmap resizedbitmap = Bitmap.createScaledBitmap(bmp, width, height, false);
		msLineView.setImageBitmap(resizedbitmap);
		msLineView.setBackgroundColor(Color.BLACK);
		msLineView.setScaleType(ScaleType.CENTER);

		LayoutParams rl = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		rl.setMargins(0, 0, 0, 20);
		rl.addRule(RelativeLayout.ABOVE, R.id.millisecondsView);
		rl.addRule(RelativeLayout.CENTER_HORIZONTAL);

		RelativeLayout parentLayout = (RelativeLayout) v.findViewById(R.id.parentLayout);
		parentLayout.addView(msLineView, rl);
	}

// ----------------------------------------------------------------------------------------
	public void setupRecordingButtons(View v) {
		OnClickListener recordingToggle = new OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleRecording();
			}
		};
		ImageButton mRecordButton = (ImageButton) v.findViewById(R.id.recordButton);
		mRecordButton.setOnClickListener(recordingToggle);

		OnClickListener closeButtonToggle = new OnClickListener() {
			@Override
			public void onClick(View v) {
				((ImageButton) v.findViewById(R.id.closeButton)).setVisibility(View.GONE);
				showRecordingButtons();
				Intent i = new Intent();
				i.setAction("BYBCloseButton");
				context.sendBroadcast(i);
				Log.d("UIFactory", "Close Button Pressed!");
			}
		};
		ImageButton mCloseButton = (ImageButton) v.findViewById(R.id.closeButton);
		mCloseButton.setOnClickListener(closeButtonToggle);
		mCloseButton.setVisibility(View.GONE);

		View tapToStopRecView = v.findViewById(R.id.TapToStopRecordingTextView);
		tapToStopRecView.setOnClickListener(recordingToggle);
	}

// -----------------------------------------------------------------------------------------------------------------------------
// ----------------------------------------- UI ANIMATIONS
// -----------------------------------------------------------------------------------------------------------------------------
	private class ShowRecordingAnimation implements Runnable {

		private Activity	activity;
		private boolean		recording;

		public ShowRecordingAnimation(Activity a, Boolean b) {
			this.activity = a;
			this.recording = b;
		}

		@Override
		public void run() {
			Animation a = null;
			if (this.recording == false) {
				a = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, -1, Animation.RELATIVE_TO_SELF, 0);
			} else {
				a = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, -1);
			}
			a.setDuration(250);
			a.setInterpolator(AnimationUtils.loadInterpolator(this.activity, android.R.anim.anticipate_overshoot_interpolator));
			View stopRecView = activity.findViewById(R.id.TapToStopRecordingTextView);
			stopRecView.startAnimation(a);
		}
	}
// -----------------------------------------------------------------------------------------------------------------------------
// ----------------------------------------- BROADCAST RECEIVERS CLASS
// -----------------------------------------------------------------------------------------------------------------------------

	private class UpdateMillisecondsReciever extends BroadcastReceiver {
		@Override
		public void onReceive(android.content.Context context, android.content.Intent intent) {
			msView.setText(intent.getStringExtra("millisecondsDisplayedString"));
		};
	}

	private class UpdateMillivoltReciever extends BroadcastReceiver {
		@Override
		public void onReceive(android.content.Context context, android.content.Intent intent) {
			mVView.setText(intent.getStringExtra("millivoltsDisplayedString"));
		};
	}

	private class SetMillivoltViewSizeReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(android.content.Context context, android.content.Intent intent) {
			mVView.setHeight(intent.getIntExtra("millivoltsViewNewSize", mVView.getHeight()));
		};
	}

	private class ShowRecordingButtonsReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(android.content.Context context, android.content.Intent intent) {
			boolean yesno = intent.getBooleanExtra("showRecordingButton", true);
			if (yesno) {
				showRecordingButtons();
			} else {
				hideRecordingButtons();
			}
		}
	}

	private class ShowCloseButtonReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(android.content.Context context, android.content.Intent intent) {
			toggleCloseButton();
			Log.d(TAG, "ShowCloseButtonReceiver");
		};
	}
	
	private class OnTabSelectedListener extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.hasExtra("tab")){
				setRenderer(intent.getIntExtra("tab", 0));
			}else{
				setRenderer(0);
			}
		}
	}
// -----------------------------------------------------------------------------------------------------------------------------
// ----------------------------------------- REGISTER RECEIVERS
// -----------------------------------------------------------------------------------------------------------------------------

	public void registerReceivers() {
		IntentFilter intentFilter = new IntentFilter("BYBUpdateMillisecondsReciever");
		upmillirec = new UpdateMillisecondsReciever();
		context.registerReceiver(upmillirec, intentFilter);

		IntentFilter intentFilterVolts = new IntentFilter("BYBUpdateMillivoltReciever");
		upmillivolt = new UpdateMillivoltReciever();
		context.registerReceiver(upmillivolt, intentFilterVolts);

		IntentFilter intentFilterVoltSize = new IntentFilter("BYBMillivoltsViewSize");
		milliVoltSize = new SetMillivoltViewSizeReceiver();
		context.registerReceiver(milliVoltSize, intentFilterVoltSize);

		IntentFilter intentFilterRecordingButtons = new IntentFilter("BYBShowRecordingButtons");
		showRecordingButtonsReceiver = new ShowRecordingButtonsReceiver();
		context.registerReceiver(showRecordingButtonsReceiver, intentFilterRecordingButtons);

		IntentFilter intentShowCloseButton = new IntentFilter("BYBShowCloseButton");
		showCloseButtonReceiver = new ShowCloseButtonReceiver();
		context.registerReceiver(showCloseButtonReceiver, intentShowCloseButton);
		
		IntentFilter intentTabSelectedFilter = new IntentFilter("BYBonTabSelected");
		tabSelectedListener = new OnTabSelectedListener();
		context.registerReceiver(tabSelectedListener , intentTabSelectedFilter);

	}

// -----------------------------------------------------------------------------------------------------------------------------
// ----------------------------------------- UNREGISTER RECEIVERS
// -----------------------------------------------------------------------------------------------------------------------------
	public void unregisterReceivers() {
		context.unregisterReceiver(upmillirec);
		context.unregisterReceiver(upmillivolt);
		context.unregisterReceiver(milliVoltSize);
		context.unregisterReceiver(showRecordingButtonsReceiver);
		context.unregisterReceiver(showCloseButtonReceiver);
		context.unregisterReceiver(tabSelectedListener);
	}
}
