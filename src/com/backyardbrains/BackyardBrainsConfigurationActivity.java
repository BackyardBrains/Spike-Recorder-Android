package com.backyardbrains;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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
		
		prefs = getSharedPreferences(prefsname, MODE_WORLD_READABLE);
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
