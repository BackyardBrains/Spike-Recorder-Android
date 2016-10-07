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

import com.backyardbrains.audio.AudioService;
import com.backyardbrains.audio.AudioService.AudioServiceBinder;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import com.backyardbrains.analysis.*;
import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;
/**
 * Main application class for the Backyard Brains app.
 * 
 * @author Nathan Dotz <nate@backyardbrains.com>
 * @author Ekavali Mishra <ekavali@gmail.com>
 *
 * @version 1.5
 */
public class BackyardBrainsApplication extends Application {
	private boolean			serviceRunning;
	protected AudioService	mAudioService;
	private int				mBindingsCount;
	protected BYBAnalysisManager analysisManager;
	public boolean isServiceRunning() {
		return serviceRunning;
	}

	public void setServiceRunning(boolean serviceRunning) {
		this.serviceRunning = serviceRunning;
	}

	public void startAudioService() {
		// spin up service
		// if (!this.serviceRunning) {
		startService(new Intent(this, AudioService.class));
		serviceRunning = true;
		// }
	}

	public void stopAudioService() {
		stopService(new Intent(this, AudioService.class));
		serviceRunning = false;
	}
	@Override
	public void onCreate() {
		try {
			Fabric.with(this, new Crashlytics());
		} catch (Exception e) {
			e.printStackTrace();
		}
		analysisManager = new BYBAnalysisManager(getApplicationContext());
		startAudioService();
     	bindAudioService(true);
	}
	/**
	 * Make sure we stop the {@link AudioService} when we exit
	 */
	@Override
	public void onTerminate() {
		super.onTerminate();
		analysisManager.close();
		bindAudioService(false);
		if (this.serviceRunning) stopAudioService();
	}

	// ----------------------------------------------------------------------------------------
	protected void bindAudioService(boolean on) {
		if (on) {
			// //Log.d(getClass().getCanonicalName(), "Binding audio service to
			// main activity.");
			Intent intent = new Intent(this, AudioService.class);
			bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
			mBindingsCount++;
			//Log.d(getClass().getCanonicalName(), "Binder called" + mBindingsCount + "bindings");
		} else {
			// //Log.d(getClass().getCanonicalName(), "unBinding audio service
			// from main activity.");
			unbindService(mConnection);
			mBindingsCount--;
			//Log.d(getClass().getCanonicalName(), "Unbinder called" + mBindingsCount + "bindings");
		}
	}

	// ----------------------------------------------------------------------------------------
	protected ServiceConnection mConnection = new ServiceConnection() {

		private boolean mAudioServiceIsBound;

		// Sets a reference in this activity to the {@link AudioService}, which
		// allows for {@link ByteBuffer}s full of audio information to be passed
		// from the {@link AudioService} down into the local
		// {@link OscilloscopeGLSurfaceView}
		//
		// @see
		// android.content.ServiceConnection#onServiceConnected(android.content.ComponentName,
		// android.os.IBinder)
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// We've bound to LocalService, cast the IBinder and get
			// LocalService instance
			AudioServiceBinder binder = (AudioServiceBinder) service;
			mAudioService = binder.getService();
			mAudioServiceIsBound = true;
			//Log.d(getClass().getCanonicalName(), "Service connected and bound");
			/*
			Intent i = new Intent();
			i.setAction("BYBAudioServiceBind");
			i.putExtra("isBind", true);
			sendBroadcast(i);
			//*/
		}

		// Clean up bindings
		//
		// @see
		// android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mAudioService = null;
			mAudioServiceIsBound = false;
//			Intent i = new Intent();
//			i.setAction("BYBAudioServiceBind");
//			i.putExtra("isBind", false);
//			getApplicationContext().sendBroadcast(i);
			//Log.d(getClass().getCanonicalName(), "Service disconnected.");
		}
	};

	// ----------------------------------------------------------------------------------------
	public AudioService getmAudioService() {
		return mAudioService;
	}
	public BYBAnalysisManager getAnalysisManager(){
		return analysisManager;
	}
	
}
