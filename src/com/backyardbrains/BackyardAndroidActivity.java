package com.backyardbrains;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.FrameLayout;

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
	private boolean isRecording = false;
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
		
		UIFactory.getUi().setupLabels(this);
		UIFactory.setupMsLineView(this);
		UIFactory.setupRecordingButtons(this);
		UIFactory.setupSampleSlider(this);

		reassignSurfaceView(triggerMode);
		
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
		registerReceivers();

		application.startAudioService();
		
	}
	
	private void registerReceivers() {
		UIFactory.getUi().registerReceivers(this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		// Unbind from the service has been moved to OpenGLThread
		application.stopAudioService();
		unregisterReceivers();
		Editor editor = settings.edit();
		editor.putBoolean("triggerMode", false);
		editor.commit();
	}
	
	private void unregisterReceivers() {
		UIFactory.getUi().unregisterReceivers(this);
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
	
	void reassignSurfaceView(boolean isTriggerView) {
		mAndroidSurface = null;
		mainscreenGLLayout.removeAllViews();
		mAndroidSurface = new OscilloscopeGLSurfaceView(this, isTriggerView);
		mainscreenGLLayout.addView(mAndroidSurface);
		if (isTriggerView) {
			UIFactory.hideRecordingButtons(this);
			UIFactory.showSampleSliderBox(this);
		} else {
			UIFactory.showRecordingButtons(this);
			UIFactory.hideSampleSliderBox(this);
		}
		Log.d(getClass().getCanonicalName(), "Reassigned OscilloscopeGLSurfaceView");
	}

	public void toggleRecording() {
		UIFactory.getUi().toggleRecording(this, isRecording);
		isRecording = !isRecording;
	}

	public void setDisplayedMilliseconds(Float ms) {
		UIFactory.getUi().setDisplayedMilliseconds(ms);
	}

}