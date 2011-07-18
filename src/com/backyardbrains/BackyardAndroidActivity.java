package com.backyardbrains;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.widget.FrameLayout;

public class BackyardAndroidActivity extends Activity {

	private OscilliscopeGLSurfaceView mAndroidSurface;

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAndroidSurface = new OscilliscopeGLSurfaceView(this);
		setContentView(R.layout.backyard_main);

		// Create custom surface
		mAndroidSurface = new OscilliscopeGLSurfaceView(this);
		FrameLayout mainscreenGLLayout = (FrameLayout) findViewById(R.id.glContainer);
		mainscreenGLLayout.addView(mAndroidSurface);
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.option_menu, menu);
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}
}