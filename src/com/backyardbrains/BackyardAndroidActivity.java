package com.backyardbrains;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;

public class BackyardAndroidActivity extends Activity {
	private GLSurfaceView glSurface;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		this.glSurface = (GLSurfaceView) findViewById(R.id.glSurface);
		if (this.glSurface != null) {
			this.glSurface.setRenderer(new OpenGLRenderer());
			this.glSurface.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
		}
	}

}