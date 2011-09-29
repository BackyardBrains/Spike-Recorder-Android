package com.backyardbrains;

import com.backyardbrains.audio.AudioService;

import android.app.Application;
import android.content.Intent;

/**
 * Main application class for the Backyard Brains app.
 * 
 * @author Nathan Dotz <nate@backyardbrains.com>
 * @version 1
 */
public class BackyardBrainsApplication extends Application {
	// private final static String TAG = "BYBAPP";

	/**
	 * Is the {@link AudioService} running?
	 */
	private boolean serviceRunning;

	/**
	 * @return the serviceRunning
	 */
	public boolean isServiceRunning() {
		return serviceRunning;
	}

	/**
	 * have the service set whether or not it's polling mic data
	 * 
	 * @param serviceRunning
	 */
	public void setServiceRunning(boolean serviceRunning) {
		this.serviceRunning = serviceRunning;
	}

	/**
	 * If {@link AudioService} has not told us it's running, tell it to start
	 */
	public void startAudioService() {
		// spin up service
		if (!this.serviceRunning)
			startService(new Intent(this, AudioService.class));
	}

	/**
	 * signal {@link AudioService} to stop
	 */
	public void stopAudioService() {
		stopService(new Intent(this, AudioService.class));
	}

	/**
	 * Make sure we stop the {@link AudioService} when we exit
	 * 
	 * @see android.app.Application#onTerminate()
	 */
	@Override
	public void onTerminate() {
		super.onTerminate();
		if(this.serviceRunning) stopAudioService();
	}

}
