package com.backyardbrains;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.backyardbrains.drawing.OscilloscopeGLSurfaceView;
import com.backyardbrains.view.UIFactory;

/**
 * Primary activity of the Backyard Brains app. By default shows the continuous
 * oscilloscope view for use with the spikerbox
 * 
 * @author Nathan Dotz <nate@backyardbrains.com>
 * @version 1
 * 
 */
public class BackyardAndroidActivity extends Activity {

	/**
	 * Reference to the {@link OscilloscopeGLSurfaceView} to draw in this
	 * activity
	 */
	private OscilloscopeGLSurfaceView mAndroidSurface;
	/**
	 * Reference to the {@link BackyardBrainsApplication} for message passing
	 */
	private BackyardBrainsApplication application;
	private TextView msView;
	//private ImageButton mRecordButton;
	private UpdateMillisecondsReciever upmillirec;
	private boolean isRecording = false;
	//private View tapToStopRecView;
	//private View mFileButton;
	private TextView mVView;
	private BroadcastReceiver upmillivolt;
	private SetMillivoltViewSizeReceiver milliVoltSize;
	//private View recordingBackground;
	private ShowRecordingButtonsReceiver showRecordingButtonsReceiver;
	private FrameLayout mainscreenGLLayout;
	private boolean triggerMode;
	private SharedPreferences settings;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.backyard_main);

		// get application
		application = (BackyardBrainsApplication) getApplication();

		mainscreenGLLayout = (FrameLayout) findViewById(R.id.glContainer);

		settings = getPreferences(MODE_PRIVATE);
		triggerMode = settings.getBoolean("triggerMode", false);
		
		setupLabels();
		setUpRecordingButtons();
		setUpSampleSlider();

		reassignSurfaceView(triggerMode);
		
	}

	private void setupLabels() {
		msView = (TextView) findViewById(R.id.millisecondsView);
		mVView = (TextView) findViewById(R.id.mVLabelView);
		setupMsLineView();
	}

	private void setupMsLineView() {
		RelativeLayout parentLayout = (RelativeLayout) findViewById(R.id.parentLayout);
		UIFactory.setupMsLineView(this, parentLayout);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.option_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		SharedPreferences.Editor editor = settings.edit();
		switch (item.getItemId()) {
		case R.id.waveview:
			triggerMode = false;
			editor.putBoolean("triggerMode", triggerMode);
			editor.commit();
			reassignSurfaceView(triggerMode);
			return true;
		case R.id.threshold:
			triggerMode = true;
			editor.putBoolean("triggerMode", triggerMode);
			editor.commit();
			reassignSurfaceView(triggerMode);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		// Bind to LocalService has been moved to OpenGLThread
		IntentFilter intentFilter = new IntentFilter(
				"BYBUpdateMillisecondsReciever");
		upmillirec = new UpdateMillisecondsReciever();
		registerReceiver(upmillirec, intentFilter);

		IntentFilter intentFilterVolts = new IntentFilter(
				"BYBUpdateMillivoltReciever");
		upmillivolt = new UpdateMillivoltReciever();
		registerReceiver(upmillivolt, intentFilterVolts);

		IntentFilter intentFilterVoltSize = new IntentFilter(
				"BYBMillivoltsViewSize");
		milliVoltSize = new SetMillivoltViewSizeReceiver();
		registerReceiver(milliVoltSize, intentFilterVoltSize);

		IntentFilter intentFilterRecordingButtons = new IntentFilter(
				"BYBShowRecordingButtons");
		showRecordingButtonsReceiver = new ShowRecordingButtonsReceiver();
		registerReceiver(showRecordingButtonsReceiver, intentFilterRecordingButtons);

		application.startAudioService();
		
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		// Unbind from the service has been moved to OpenGLThread
		application.stopAudioService();
		unregisterReceiver(upmillirec);
		unregisterReceiver(upmillivolt);
		unregisterReceiver(milliVoltSize);
		unregisterReceiver(showRecordingButtonsReceiver);
		Editor editor = settings.edit();
		editor.putBoolean("triggerMode", false);
		editor.commit();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		Editor editor = settings.edit();
		editor.putBoolean("triggerMode", false);
		editor.commit();
		super.onDestroy();
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		mAndroidSurface.onTouchEvent(event);
		return super.onTouchEvent(event);
	}
	
	private void setUpRecordingButtons() {
		UIFactory.setupRecordingButtons(this);
	}
	
	private void showRecordingButtons() {
		UIFactory.showRecordingButtons(this);
	}

	private void hideRecordingButtons() {
		UIFactory.hideRecordingButtons(this);
	}

	private void setUpSampleSlider() {
		UIFactory.setupSampleSlider(this);
	}

	public void toggleSeekbar() {
		UIFactory.toggleSeekbar(this);
	}

	private void showSampleSliderBox() {
		UIFactory.showSampleSliderBox(this);
	}

	private void hideSampleSliderBox() {
		UIFactory.hideSampleSliderBox(this);
	}

	void reassignSurfaceView(boolean isTriggerView) {
		mAndroidSurface = null;
		mainscreenGLLayout.removeAllViews();
		mAndroidSurface = new OscilloscopeGLSurfaceView(this, isTriggerView);
		mainscreenGLLayout.addView(mAndroidSurface);
		if (isTriggerView) {
			hideRecordingButtons();
			showSampleSliderBox();
		} else {
			showRecordingButtons();
			hideSampleSliderBox();
		}
		Log.d(getClass().getCanonicalName(), "Reassigned OscilloscopeGLSurfaceView");
	}

	public void toggleRecording() {
		UIFactory uiFactory = new UIFactory();
		uiFactory.toggleRecording(this, isRecording);
		isRecording = !isRecording;
	}

	public void setDisplayedMilliseconds(Float ms) {
		msView.setText(ms.toString());
	}

	private class UpdateMillisecondsReciever extends BroadcastReceiver {
		@Override
		public void onReceive(android.content.Context context,
				android.content.Intent intent) {
			msView.setText(intent.getStringExtra("millisecondsDisplayedString"));
		};
	}

	private class UpdateMillivoltReciever extends BroadcastReceiver {
		@Override
		public void onReceive(android.content.Context context,
				android.content.Intent intent) {
			mVView.setText(intent.getStringExtra("millivoltsDisplayedString"));
		};
	}

	private class SetMillivoltViewSizeReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(android.content.Context context,
				android.content.Intent intent) {
			mVView.setHeight(intent.getIntExtra("millivoltsViewNewSize", mVView.getHeight()));
		};
	}

	private class ShowRecordingButtonsReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(android.content.Context context,
				android.content.Intent intent) {
			boolean yesno = intent.getBooleanExtra("showRecordingButton", true);
			if (yesno) {
				showRecordingButtons();
			} else {
				hideRecordingButtons();
			}
		}	
		}
}