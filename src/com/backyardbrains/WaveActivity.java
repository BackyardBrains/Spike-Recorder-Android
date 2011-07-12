package com.backyardbrains;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.hardware.SensorManager;
import android.hardware.SensorListener;

public class WaveActivity extends Activity {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		GLSurfaceView view = new GLSurfaceView(this);
		view.setRenderer(new OpenGLRenderer());
		setContentView(view);

		// Locate the SensorManager using Activity.getSystemService
		SensorManager sm;
		sm = (SensorManager) getSystemService(SENSOR_SERVICE);

		// Register your SensorListener
		sm.registerListener(sl, SensorManager.SENSOR_ORIENTATION,
				SensorManager.SENSOR_DELAY_NORMAL);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.option_menu, menu);
		return true;
	}

	private final SensorListener sl = new SensorListener() {
		public void onSensorChanged(int sensor, float[] values) {
			float pitch = values[2];
			if (pitch <= 45 && pitch >= -45) {
				// mostly vertical
			} else if (pitch < -45) {
				// mostly right side up
			} else if (pitch > 45) {
				// mostly left side up
			}
		}

		public void onAccuracyChanged(int sensor, int accuracy) {
		}
	};

}