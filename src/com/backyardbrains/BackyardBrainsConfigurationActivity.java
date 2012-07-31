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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

public class BackyardBrainsConfigurationActivity extends Activity {

	private SharedPreferences prefs;
	private String speedPrefsKey;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.configuration_screen);
		
		final String prefsname = getResources().getString(R.string.global_prefs);
		speedPrefsKey = getResources().getString(R.string.microphone_read_speed);
		
		prefs = getSharedPreferences(prefsname, MODE_PRIVATE);
		CharSequence preferencesSpeed = prefs.getString(speedPrefsKey, "1");

		Log.d("Config Screen", "read MicrophoneReadSpeed of " + preferencesSpeed);
		Spinner spinner = (Spinner) findViewById(R.id.read_speed_spinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.speeds_array,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setSelection(adapter.getPosition(preferencesSpeed), true);
		spinner.setOnItemSelectedListener(new SpeedSelectedListener());

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.option_menu, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.configuration).setVisible(false);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.waveview:
			Intent ca = new Intent(this, BackyardAndroidActivity.class);
			startActivity(ca);
			return true;
		case R.id.threshold:
			Intent ta = new Intent(this, TriggerActivity.class);
			startActivity(ta);
			return true;
		case R.id.configuration:
			Intent config = new Intent(this,
					BackyardBrainsConfigurationActivity.class);
			startActivity(config);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public class SpeedSelectedListener implements OnItemSelectedListener {

	    public void onItemSelected(AdapterView<?> parent,
	        View view, int pos, long id) {
	    	SharedPreferences.Editor ed = prefs.edit();
	    	ed.putString(speedPrefsKey, parent.getItemAtPosition(pos).toString());
	    	ed.commit();
	      Toast.makeText(parent.getContext(), "Set audio read speed to " +
	          parent.getItemAtPosition(pos).toString(), Toast.LENGTH_LONG).show();
	    }

	    public void onNothingSelected(AdapterView<?> parent) {
	      // Do nothing.
	    }
	}	

}
