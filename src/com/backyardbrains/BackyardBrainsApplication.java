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
	 * A reference to the running {@link BackyardAndroidActivity}
	 */
	private BackyardAndroidActivity runningActivity;

	/**
	 * Used by {@link AudioService} to reference the foreground activity
	 * 
	 * @return the runningActivity
	 */
	public BackyardAndroidActivity getRunningActivity() {
		return runningActivity;
	}

	/**
	 * Used by {@link BackyardAndroidActivity} to tell the application it's in
	 * the foreground so that
	 * {@link AudioService#receiveAudio(java.nio.ByteBuffer)} can retrieve it
	 * via {@link BackyardBrainsApplication#getRunningActivity()}
	 * 
	 * @param runningActivity
	 *            the runningActivity to set
	 */
	public void setRunningActivity(BackyardAndroidActivity runningActivity) {
		this.runningActivity = runningActivity;
	}

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
	 * When we start, spin up the {@link AudioService}
	 * 
	 * @see android.app.Application#onCreate()
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		// new AudioService();
		startAudioService();
	}

	/**
	 * Make sure we stop the {@link AudioService} when we exit
	 * 
	 * @see android.app.Application#onTerminate()
	 */
	@Override
	public void onTerminate() {
		super.onTerminate();
		stopAudioService();
	}

}
