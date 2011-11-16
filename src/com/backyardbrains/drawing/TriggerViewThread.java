package com.backyardbrains.drawing;

import android.util.Log;

public class TriggerViewThread extends OscilloscopeGLThread {

	private static final String TAG = Class.class.getCanonicalName();

	TriggerViewThread(OscilloscopeGLSurfaceView view) {
		super(view);
		Log.d(TAG , "Creating TriggerViewThread");
	}

	public final boolean drawThresholdLine = true;
}
