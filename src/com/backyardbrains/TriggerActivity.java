/*
 * Backyard Brains Android App
 * Copyright (C) 2011 Backyard Brains
 * by Nathan Dotz <nate (at) backyardbrains.com>
 * Edited by Ekavali Mishra <ekavali (at) gmail.com>
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

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

import com.backyardbrains.audio.AudioService;
import com.backyardbrains.drawing.ThresholdGlSurfaceView;
import com.backyardbrains.view.UIFactory;

public class TriggerActivity extends BackyardAndroidActivity {

	@Override
	protected void setGlSurface() {
		mAndroidSurface = new ThresholdGlSurfaceView(this);
	}
	
	
	@Override
	protected void onResume() {
		super.onResume();
		broadcastToggleTrigger(true);
	}
	
	@Override
	protected void onPause() {
		broadcastToggleTrigger(false);
		super.onPause();
	}
	

	private void broadcastToggleTrigger(boolean b) {
		Intent i = new Intent("BYBToggleTrigger").putExtra("triggerMode", b);
		sendBroadcast(i);
	}
	
	@Override
	protected void enableUiForActivity() {
		UIFactory.hideRecordingButtons(this);
		UIFactory.showSampleSliderBox(this);
	}
}
