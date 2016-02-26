package com.backyardbrains;

import com.backyardbrains.drawing.ContinuousGLSurfaceView;
import com.backyardbrains.drawing.FindSpikesRenderer;
import com.backyardbrains.view.ScaleListener;
import com.backyardbrains.view.TwoDimensionScaleGestureDetector;

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
import android.widget.SeekBar;
import android.widget.TextView;

public class BackyardBrainsSpikesFragment extends Fragment {

	private static final String					TAG				= "BackyardBrainsSpikesFragment";

	private SharedPreferences					settings		= null;
	private Context								context			= null;

	protected TwoDimensionScaleGestureDetector	mScaleDetector;
	private ScaleListener						mScaleListener;

// protected ContinuousGLSurfaceView mAndroidSurface = null;
	protected GLSurfaceView						mAndroidSurface	= null;
	protected FindSpikesRenderer				renderer		= null;
	private FrameLayout							mainscreenGLLayout;

	// ----------------------------------------------------------------------------------------
	public BackyardBrainsSpikesFragment(Context context) {
		super();
		this.context = context.getApplicationContext();
	}
	// ----------------------------------------------------------------------------------------
// public BackyardBrainsSpikesFragment(Context context) {
// super();
// this.context = context.getApplicationContext();
// }
// -----------------------------------------------------------------------------------------------------------------------------
// ----------------------------------------- AUDIO FILE
// -----------------------------------------------------------------------------------------------------------------------------

	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- FRAGMENT LIFECYCLE
	// -----------------------------------------------------------------------------------------------------------------------------
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (context != null) {
			mScaleListener = new ScaleListener();
			mScaleDetector = new TwoDimensionScaleGestureDetector(context, mScaleListener);
		}
	}

	// ----------------------------------------------------------------------------------------
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.backyard_spikes, container, false);
		getSettings();
		mainscreenGLLayout = (FrameLayout) rootView.findViewById(R.id.glContainer2);
		setupButtons(rootView);
		return rootView;
	}

	// ----------------------------------------------------------------------------------------
	@Override
	public void onStart() {
		readSettings();
		reassignSurfaceView();

		super.onStart();
	}

	// ----------------------------------------------------------------------------------------
	@Override
	public void onResume() {
		registerReceivers();
		readSettings();
		if (mAndroidSurface != null) {
			mAndroidSurface.onResume();
		}
		super.onResume();
	}

	// ----------------------------------------------------------------------------------------
	@Override
	public void onPause() {
		if (mAndroidSurface != null) {
			mAndroidSurface.onPause();
		}
		unregisterReceivers();
		saveSettings();
		super.onPause();
	}

	// ----------------------------------------------------------------------------------------
	@Override
	public void onStop() {
		saveSettings();
		super.onStop();
		mAndroidSurface = null;
	}

	// ----------------------------------------------------------------------------------------
	@Override
	public void onDestroy() {
		destroyRenderers();
		super.onDestroy();
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- GL RENDERING
	// ----------------------------------------------------------------------------------------
	private void destroyRenderers() {
		if (renderer != null) {
			renderer.close();
			renderer = null;
		}
	}
	protected void reassignSurfaceView() {
		if (context != null) {
			mAndroidSurface = null;
			mainscreenGLLayout.removeAllViews();
			if (renderer != null) {
				saveSettings();
				renderer = null;
			}
			if (renderer == null) {
				renderer = new FindSpikesRenderer(context);

			}

// mAndroidSurface = new ContinuousGLSurfaceView(context, renderer);
			mAndroidSurface = new GLSurfaceView(context);
			mAndroidSurface.setRenderer(renderer);

			mScaleListener.setRenderer(renderer);

			mainscreenGLLayout.addView(mAndroidSurface);

			readSettings();

			Log.d(getClass().getCanonicalName(), "Reassigned FindSpikesGLSurfaceView");
		} else {
			Log.d(TAG, "context == null");
		}

	}

	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- TOUCH
	// -----------------------------------------------------------------------------------------------------------------------------
	public boolean onTouchEvent(MotionEvent event) {
		mScaleDetector.onTouchEvent(event);
		if (mAndroidSurface != null) {
			return mAndroidSurface.onTouchEvent(event);
		}
		return false;
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- SETTINGS
	// -----------------------------------------------------------------------------------------------------------------------------
	private void getSettings() {
		if (settings == null) {
			settings = getActivity().getPreferences(BackyardBrainsMain.MODE_PRIVATE);
		}
	}

	// ----------------------------------------------------------------------------------------
	protected void readSettings() {
		if (settings != null) {

			if (renderer != null) {
				renderer.setAutoScaled(settings.getBoolean("spikesRendererAutoscaled", renderer.isAutoScaled()));
				renderer.setGlWindowHorizontalSize(settings.getInt("spikesRendererGlWindowHorizontalSize", renderer.getGlWindowHorizontalSize()));
				renderer.setGlWindowVerticalSize(settings.getInt("spikesRendererGlWindowVerticalSize", renderer.getGlWindowVerticalSize()));
				Log.d(TAG, "renderer readsettings"); 
				Log.d(TAG,"isAutoScaled: "+settings.getBoolean("spikesRendererAutoscaled",false));
				Log.d(TAG, "GlHorizontalSize: " + settings.getInt("spikesRendererGlWindowHorizontalSize", 0)); //
				Log.d(TAG, "GlVerticalSize: " + settings.getInt("spikesRendererGlWindowVerticalSize", 0));
			} //

		} else {
			Log.d(TAG, "Cant Read settings. settings == null");
		}
	}

	// ----------------------------------------------------------------------------------------
	protected void saveSettings() {
		if (settings != null) {
			final SharedPreferences.Editor editor = settings.edit();

			if (renderer != null) {
				editor.putBoolean("spikesRendererAutoscaled", renderer.isAutoScaled());
				editor.putInt("spikesRendererGlWindowHorizontalSize", renderer.getGlWindowHorizontalSize());
				editor.putInt("spikesRendererGlWindowVerticalSize", renderer.getGlWindowVerticalSize());
				editor.commit();
				Log.d(TAG, "renderer saved settings");
				Log.d(TAG, "rendererAutoscaled " + renderer.isAutoScaled());
				Log.d(TAG, "rendererGlWindowHorizontalSize " + renderer.getGlWindowHorizontalSize());
				Log.d(TAG, "rendererGlWindowVerticalSize " + renderer.getGlWindowVerticalSize());
			}

		} else {
			Log.d(TAG, "Cant Save settings. settings == null");
		}
	}
	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- UI
	// -----------------------------------------------------------------------------------------------------------------------------

	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- UI SETUPS
	// -----------------------------------------------------------------------------------------------------------------------------

	// ----------------------------------------------------------------------------------------

	// ----------------------------------------------------------------------------------------
	public void setupButtons(View view) {
		/*
		 * OnClickListener recordingToggle = new OnClickListener() {
		 * @Override public void onClick(View v) { toggleRecording(); } };
		 * ImageButton mRecordButton = (ImageButton)
		 * view.findViewById(R.id.recordButton);
		 * mRecordButton.setOnClickListener(recordingToggle);
		 * //------------------------------------ View tapToStopRecView =
		 * view.findViewById(R.id.TapToStopRecordingTextView);
		 * tapToStopRecView.setOnClickListener(recordingToggle);
		 * //------------------------------------ OnClickListener
		 * closeButtonListener = new OnClickListener() {
		 * @Override public void onClick(View v) { ((ImageButton)
		 * v.findViewById(R.id.closeButton)).setVisibility(View.GONE);
		 * showRecordingButtons(); Intent i = new Intent();
		 * i.setAction("BYBCloseButton"); context.sendBroadcast(i);
		 * Log.d("UIFactory", "Close Button Pressed!"); } }; ImageButton
		 * mCloseButton = (ImageButton) view.findViewById(R.id.closeButton);
		 * mCloseButton.setOnClickListener(closeButtonListener);
		 * mCloseButton.setVisibility(View.GONE);
		 * //------------------------------------ OnTouchListener threshTouch =
		 * new OnTouchListener() {
		 * @Override public boolean onTouch(View v, MotionEvent event) { if
		 * (v.getVisibility() == View.VISIBLE) { Log.d("threshold Handle", "y: "
		 * + event.getY() + "  view.y: " + v.getY()); if (event.getActionIndex()
		 * == 0) { int yOffset = 0; if
		 * (getActivity().getActionBar().isShowing()) { yOffset =
		 * getActivity().getActionBar().getHeight(); } switch
		 * (event.getActionMasked()) { case MotionEvent.ACTION_DOWN: // break;
		 * case MotionEvent.ACTION_MOVE: v.setY(event.getRawY() - v.getHeight()
		 * / 2 - yOffset); Intent i = new Intent();
		 * i.setAction("BYBThresholdHandlePos"); i.putExtra("y", event.getRawY()
		 * - yOffset); context.sendBroadcast(i); break; case
		 * MotionEvent.ACTION_CANCEL: case MotionEvent.ACTION_UP: Intent ii =
		 * new Intent(); ii.setAction("BYBThresholdHandlePos");
		 * ii.putExtra("update", true); ii.putExtra("y", event.getRawY() -
		 * yOffset); context.sendBroadcast(ii); } } return true; } return false;
		 * } }; ((ImageButton)
		 * view.findViewById(R.id.thresholdHandle)).setOnTouchListener(
		 * threshTouch); //------------------------------------ final SeekBar
		 * sk=(SeekBar) view.findViewById(R.id.samplesSeekBar);
		 * sk.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
		 * @Override public void onStopTrackingTouch(SeekBar seekBar) { // TODO
		 * Auto-generated method stub }
		 * @Override public void onStartTrackingTouch(SeekBar seekBar) { // TODO
		 * Auto-generated method stub }
		 * @Override public void onProgressChanged(SeekBar seekBar, int
		 * progress,boolean fromUser) { if(getView()!=null){ TextView tx =
		 * ((TextView) getView().findViewById(R.id.numberOfSamplesAveraged));
		 * if(tx != null){ tx.setText(progress + "x"); } } if(fromUser){ Intent
		 * i = new Intent(); i.setAction("BYBThresholdNumAverages");
		 * i.putExtra("num",progress); context.sendBroadcast(i); } } });
		 * ((TextView) view.findViewById(R.id.numberOfSamplesAveraged)).setText(
		 * TriggerAverager.defaultSize + "x");
		 * sk.setProgress(TriggerAverager.defaultSize); //
		 */
		final SeekBar sk = (SeekBar) view.findViewById(R.id.playheadBar);

		sk.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					if (renderer != null) {
						renderer.setStartSample((float) sk.getProgress() / (float) sk.getMax());
					}
				}
			}
		});
		sk.setProgress(0);
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	// --------------------------------------------------- LISTENERS
	// -----------------------------------------------------------------------------------------------------------------------------

	// ----------------------------------------- BROADCAST RECEIVERS OBJECTS
	private DebugShitListener debugShitListener;

	// ----------------------------------------- BROADCAST RECEIVERS CLASS
	private class DebugShitListener extends BroadcastReceiver {
		@Override
		public void onReceive(android.content.Context context, android.content.Intent intent) {
			if (intent.hasExtra("d")) {
				String t = intent.getStringExtra("d");
				((TextView) getView().findViewById(R.id.debugShit)).setText(t);
			}
		};
	}

	// ----------------------------------------- BROADCAST RECEIVERS TOGGLES
	private void registerDebugShitListener(boolean reg) {
		if (reg) {
			IntentFilter intentFilter = new IntentFilter("DebugShit");
			debugShitListener = new DebugShitListener();
			context.registerReceiver(debugShitListener, intentFilter);
		} else {
			context.unregisterReceiver(debugShitListener);
			debugShitListener = null;
		}
	}

	// ----------------------------------------- REGISTER RECEIVERS
	public void registerReceivers() {
		registerDebugShitListener(true);
	}

	// ----------------------------------------- UNREGISTER RECEIVERS
	public void unregisterReceivers() {
		registerDebugShitListener(false);
	}
}