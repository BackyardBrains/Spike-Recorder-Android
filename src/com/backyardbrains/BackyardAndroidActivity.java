package com.backyardbrains;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

public class BackyardAndroidActivity extends Activity {

	private OscilliscopeGLSurfaceView mAndroidSurface;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAndroidSurface = new OscilliscopeGLSurfaceView(this);
		setContentView(R.layout.backyard_main);

		FrameLayout mainscreenGLLayout = (FrameLayout) findViewById(R.id.glContainer);
		mainscreenGLLayout.addView(mAndroidSurface);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.option_menu, menu);
		return true;
	}

	private class OscilliscopeGLSurfaceView extends SurfaceView implements
			SurfaceHolder.Callback {

		SurfaceHolder mAndroidHolder;
		private OscilliscopeGLThread mGLThread;

		OscilliscopeGLSurfaceView(Context context) {
			super(context);
			mAndroidHolder = getHolder();
			mAndroidHolder.addCallback(this);
			mAndroidHolder.setType(SurfaceHolder.SURFACE_TYPE_GPU);
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			// TODO Auto-generated method stub

		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			mGLThread = new OscilliscopeGLThread(this);
			mGLThread.start();
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			// TODO Auto-generated method stub

		}
	}
}