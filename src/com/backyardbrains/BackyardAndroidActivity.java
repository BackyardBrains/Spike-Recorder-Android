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

import com.backyardbrains.audio.AudioService;
import com.backyardbrains.audio.MicListener;
import com.backyardbrains.audio.AudioService.AudioServiceBinder;
import com.backyardbrains.drawing.OscilliscopeGLSurfaceView;
import com.backyardbrains.drawing.OscilliscopeGLThread;

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
	 * Reference to the {@link AudioService} which polls the default audio
	 * device
	 */
	private AudioService mAudioService;
	/**
	 * Is the {@link AudioService} currently bound to this activity?
	 */
	private boolean mAudioServiceIsBound;
	/**
	 * Reference to the {@link OscilliscopeGLSurfaceView} to draw in this
	 * activity
	 */
	private OscilliscopeGLSurfaceView mAndroidSurface;
	/**
	 * Reference to the {@link BackyardBrainsApplication} for message passing
	 */
	private BackyardBrainsApplication application;

	/** Defines callbacks for service binding, passed to bindService() */
	private ServiceConnection mConnection = new ServiceConnection() {

		/**
		 * Sets a reference in this activity to the {@link AudioService}, which
		 * allows for {@link ByteBuffer}s full of audio information to be passed
		 * from the {@link AudioService} down into the local
		 * {@link OscilliscopeGLSurfaceView}
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
		}

		/**
		 * Clean up bindings
		 * 
		 * @TODO null out mAudioService
		 * 
		 * @see android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
		 */
		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mAudioServiceIsBound = false;
		}
	};

	/**
	 * Create the surface we'll use to draw on, grab an instance of the
	 * {@link BackyardBrainsApplication} and use it to spin up the
	 * {@link MicListener} thread (via {@link AudioService}).
	 * 
	 * @TODO remove double-instantiation of mAndroidSurface :O
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

	/**
	 * @return {@link ByteBuffer} of current received data from
	 *         {@link AudioService}e
	 * @deprecated
	 */
	public ByteBuffer getAudioFromService() {
		return mAudioService.getAudioFromMicListener();
	}

	/**
	 * inflate menu to switch between continuous and threshold modes
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.option_menu, menu);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
	}

	/**
	 * Attach to {@link AudioService} when we start
	 * 
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart() {
		super.onStart();
		// Bind to LocalService
		Intent intent = new Intent(this, AudioService.class);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	}

	/**
	 * Un-bind from {@link AudioService} when we stop
	 * 
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop() {
		super.onStop();
		// Unbind from the service
		if (mAudioServiceIsBound) {
			unbindService(mConnection);
			mAudioServiceIsBound = false;
		}
	}

	/**
	 * Called by {@link AudioService#receiveAudio(ByteBuffer)} to push current
	 * sample buffer into our {@link OscilliscopeGLThread} for drawing
	 * 
	 * @param audioData
	 */
	public void setCurrentAudio(ByteBuffer audioData) {
		OscilliscopeGLThread l_thread = mAndroidSurface.getGLThread();
		if (l_thread != null)
			l_thread.setAudioBuffer(audioData);

	}
}