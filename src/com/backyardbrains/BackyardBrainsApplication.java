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
