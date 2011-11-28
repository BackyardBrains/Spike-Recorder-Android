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
	private boolean serviceRunning;

	public boolean isServiceRunning() {
		return serviceRunning;
	}

	public void setServiceRunning(boolean serviceRunning) {
		this.serviceRunning = serviceRunning;
	}

	public void startAudioService() {
		// spin up service
		if (!this.serviceRunning) {
			startService(new Intent(this, AudioService.class));
			serviceRunning = true;
		}
	}

	public void stopAudioService() {
		stopService(new Intent(this, AudioService.class));
		serviceRunning = false;
	}

	/**
	 * Make sure we stop the {@link AudioService} when we exit
	 */
	@Override
	public void onTerminate() {
		super.onTerminate();
		if(this.serviceRunning) stopAudioService();
	}

}
