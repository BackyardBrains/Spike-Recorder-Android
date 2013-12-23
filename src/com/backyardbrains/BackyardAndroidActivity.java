/*
 * Backyard Brains Android App
 * Copyright (C) 2011 Backyard Brains
 * by Nathan Dotz <nate (at) backyardbrains.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.backyardbrains;

import java.nio.ByteBuffer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.backyardbrains.audio.AudioService;
import com.backyardbrains.audio.AudioService.AudioServiceBinder;
import com.backyardbrains.drawing.ContinuousGLSurfaceView;
import com.backyardbrains.drawing.ThresholdRenderer;
import com.backyardbrains.view.UIFactory;


/**
 * Primary activity of the Backyard Brains app. By default shows the continuous
 * oscilloscope view for use with the spikerbox
 * 
 * @author Nathan Dotz <nate@backyardbrains.com>
 * @version 1.5
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
	private int mBindingsCount;


	/**
	 * @return the mAudioService
	 */
	public AudioService getmAudioService() {
		return mAudioService;
	}

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
		

		BackyardBrainsApplication application = (BackyardBrainsApplication) getApplication();
		application.startAudioService();
		bindAudioService(true);
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
	public boolean onPrepareOptionsMenu(Menu menu) {
		return super.onPrepareOptionsMenu(menu);
	}
	


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.waveview:
			startActivity(new Intent(this, BackyardAndroidActivity.class));
			return true;
		case R.id.threshold:
			startActivity(new Intent(this, TriggerActivity.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}


	@Override
	protected void onResume() {
		UIFactory.getUi().registerReceivers(this);
		reassignSurfaceView();
		//bindAudioService(true);
		super.onResume();
	}

	@Override
	protected void onPause() {
		mAndroidSurface = null;
		UIFactory.getUi().unregisterReceivers(this);
		super.onPause();
		//bindAudioService(false);
	}


	@Override
	protected void onStart() {
		super.onStart();
		//BackyardBrainsApplication application = (BackyardBrainsApplication) getApplication();
		//application.startAudioService();
		//bindAudioService(true);
	}
	
	@Override
	protected void onStop() {
		//bindAudioService(false);
		//BackyardBrainsApplication application = (BackyardBrainsApplication) getApplication();
		//application.stopAudioService();
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("triggerAutoscaled", false);
		editor.putBoolean("continuousAutoscaled", false);
		editor.commit();
		super.onStop();
		//finish();
	}

	@Override
	protected void onDestroy() {
		bindAudioService(false);
		BackyardBrainsApplication application = (BackyardBrainsApplication) getApplication();
		application.stopAudioService();
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

	protected void bindAudioService(boolean on) {
		if (on) {
			//Log.d(getClass().getCanonicalName(), "Binding audio service to main activity.");
			Intent intent = new Intent(this, AudioService.class);
			bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
			mBindingsCount++;
			Log.d(getClass().getCanonicalName(), "Binder called" + mBindingsCount + "bindings");
		} else {
			//Log.d(getClass().getCanonicalName(), "unBinding audio service from main activity.");
			unbindService(mConnection);
			mBindingsCount--;
			Log.d(getClass().getCanonicalName(), "Unbinder called" + mBindingsCount + "bindings");
		}
	}

	protected ServiceConnection mConnection = new ServiceConnection() {

		private boolean mAudioServiceIsBound;

		/**
		 * Sets a reference in this activity to the {@link AudioService}, which
		 * allows for {@link ByteBuffer}s full of audio information to be passed
		 * from the {@link AudioService} down into the local
		 * {@link OscilloscopeGLSurfaceView}
		 * 
		 * @see android.content.ServiceConnection#onServiceConnected(android.content.ComponentName,
		 *      android.os.IBinder)
		 */
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// We've bound to LocalService, cast the IBinder and get
			// LocalService instance
			AudioServiceBinder binder = (AudioServiceBinder) service;
			mAudioService = binder.getService();
			mAudioServiceIsBound = true;
			Log.d(getClass().getCanonicalName(), "Service connected and bound");
		}

		/**
		 * Clean up bindings
		 * 
		 * @see android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
		 */
		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mAudioService = null;
			mAudioServiceIsBound = false;
			Log.d(getClass().getCanonicalName(), "Service disconnected.");
		}
	};
	
	 

}
