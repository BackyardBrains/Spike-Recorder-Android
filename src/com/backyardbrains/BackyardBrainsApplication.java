package com.backyardbrains;

import android.app.Application;
import android.content.Intent;

public class BackyardBrainsApplication extends Application {
	//private final static String TAG = "BYBAPP";

	private boolean audioRunning;
	private AudioService audio;

	/**
	 * @return the serviceRunning
	 */
	public boolean isServiceRunning() {
		return audioRunning;
	}

	/**
	 * @param serviceRunning
	 *            the serviceRunning to set
	 */
	public void setServiceRunning(boolean serviceRunning) {
		this.audioRunning = serviceRunning;
	}

	public void startAudioService() {
		// spin up service
		if (!this.audioRunning && !this.audio.running)
			startService(new Intent(this, AudioService.class));
	}

	public void stopAudioService() {
		stopService(new Intent(this, AudioService.class));
		this.audio.stopSelf();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Application#onCreate()
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		this.audio = new AudioService();
		startAudioService();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Application#onTerminate()
	 */
	@Override
	public void onTerminate() {
		super.onTerminate();
		stopAudioService();
		this.audio = null;
	}

}
