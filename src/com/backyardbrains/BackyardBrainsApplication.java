package com.backyardbrains;

import android.app.Application;

public class BackyardBrainsApplication extends Application {
	private final static
	String TAG = "BYBAPP";
	
	private boolean audioRunning;

	private AudioService audio;

	/**
	 * @return the serviceRunning
	 */
	public boolean isServiceRunning() {
		return audioRunning;
	}

	/**
	 * @param serviceRunning the serviceRunning to set
	 */
	public void setServiceRunning(boolean serviceRunning) {
		this.audioRunning = serviceRunning;
	}
	
	public void startAudioService() {
		// TODO Auto-generated method stub

	}
	public void stopAudioService() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see android.app.Application#onCreate()
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		this.audio = new AudioService();
	}

	/* (non-Javadoc)
	 * @see android.app.Application#onTerminate()
	 */
	@Override
	public void onTerminate() {
		super.onTerminate();
		this.audio = null;
	}
	
	
}
