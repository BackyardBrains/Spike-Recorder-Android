package com.backyardbrains;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.backyardbrains.audio.AudioService;
import com.backyardbrains.drawing.ContinuousGLSurfaceView;
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
	protected ContinuousGLSurfaceView mAndroidSurface;
	private boolean isRecording = false;
	private FrameLayout mainscreenGLLayout;
	private SharedPreferences settings;
	protected AudioService mAudioService;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.backyard_main);

		getSettings();
		mainscreenGLLayout = (FrameLayout) findViewById(R.id.glContainer);

		UIFactory.getUi().setupLabels(this);
		UIFactory.setupMsLineView(this);
		UIFactory.setupRecordingButtons(this);
		UIFactory.setupSampleSlider(this);
	}

	protected void reassignSurfaceView() {
		mAndroidSurface = null;
		mainscreenGLLayout.removeAllViews();
		setGlSurface();
		mainscreenGLLayout.addView(mAndroidSurface);
		enableUiForActivity();
		Log.d(getClass().getCanonicalName(),
				"Reassigned OscilloscopeGLSurfaceView");
	}

	protected void enableUiForActivity() {
		UIFactory.showRecordingButtons(this);
		UIFactory.hideSampleSliderBox(this);
	}

	protected void setGlSurface() {
		mAndroidSurface = new ContinuousGLSurfaceView(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.option_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.waveview:
			Intent ca = new Intent(this, BackyardAndroidActivity.class);
			startActivity(ca);
			return true;
		case R.id.threshold:
			Intent ta = new Intent(this, TriggerActivity.class);
			startActivity(ta);
			return true;
		case R.id.configuration:
			Intent config = new Intent(this,
					BackyardBrainsConfigurationActivity.class);
			startActivity(config);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		BackyardBrainsApplication application = (BackyardBrainsApplication) getApplication();
		application.startAudioService();
	}

	@Override
	protected void onResume() {
		UIFactory.getUi().registerReceivers(this);
		reassignSurfaceView(); 
		super.onResume();
	}

	@Override
	protected void onPause() {
		mAndroidSurface = null;
		UIFactory.getUi().unregisterReceivers(this);
		super.onPause();
	}

	@Override
	protected void onStop() {
		BackyardBrainsApplication application = (BackyardBrainsApplication) getApplication();
		application.stopAudioService();
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("triggerAutoscaled", false);
		editor.putBoolean("continuousAutoscaled", false);
		editor.commit();
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		mAndroidSurface.onTouchEvent(event);
		return super.onTouchEvent(event);
	}

	public void toggleRecording() {
		UIFactory.getUi().toggleRecording(this, isRecording);
		isRecording = !isRecording;
	}

	public void setDisplayedMilliseconds(Float ms) {
		UIFactory.getUi().setDisplayedMilliseconds(ms);
	}

	private void getSettings() {
		if (settings == null) {
			settings = getPreferences(MODE_PRIVATE);
		}
	}

}
