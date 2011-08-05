package com.backyardbrains;

import java.nio.ByteBuffer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.widget.FrameLayout;

import com.backyardbrains.AudioService.AudioServiceBinder;

public class BackyardAndroidActivity extends Activity {

	private AudioService mAudioService;
	private boolean mAudioServiceIsBound;
	private OscilliscopeGLSurfaceView mAndroidSurface;
	private BackyardBrainsApplication application;

	/** Defines callbacks for service binding, passed to bindService() */
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// We've bound to LocalService, cast the IBinder and get
			// LocalService instance
			AudioServiceBinder binder = (AudioServiceBinder) service;
			mAudioService = binder.getService();
			mAudioServiceIsBound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mAudioServiceIsBound = false;
		}
	};

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAndroidSurface = new OscilliscopeGLSurfaceView(this);
		setContentView(R.layout.backyard_main);

		// get application
		application = (BackyardBrainsApplication) getApplication();
		application.setRunningActivity(this);
		application.startAudioService();

		// Create custom surface
		mAndroidSurface = new OscilliscopeGLSurfaceView(this);
		FrameLayout mainscreenGLLayout = (FrameLayout) findViewById(R.id.glContainer);
		mainscreenGLLayout.addView(mAndroidSurface);
	}

	public ByteBuffer getAudioFromService() {
		return mAudioService.getAudioFromMicListener();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.option_menu, menu);
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onStart() {
		super.onStart();
		// Bind to LocalService
		Intent intent = new Intent(this, AudioService.class);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onStop() {
		super.onStop();
		// Unbind from the service
		if (mAudioServiceIsBound) {
			unbindService(mConnection);
			mAudioServiceIsBound = false;
		}
	}

	public void setCurrentAudio(ByteBuffer audioData) {
		OscilliscopeGLThread l_thread = mAndroidSurface.getGLThread();
		if (l_thread != null)
			l_thread.setAudioBuffer(audioData);

	}
}