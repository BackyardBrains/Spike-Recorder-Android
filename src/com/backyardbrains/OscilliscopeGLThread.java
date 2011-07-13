package com.backyardbrains;

import android.util.Log;
import android.view.SurfaceView;

public class OscilliscopeGLThread extends Thread {
	
	SurfaceView parent;
	private boolean mDone = false; // signal whether thread is finished	
	
	OscilliscopeGLThread(SurfaceView view) {
		parent = view;
	}
	
	public void run() {
		initEGL();
		initGL();
		while(!mDone) {
			// draw things
		}
	}
	
	public void requestStop() {
		mDone = true;
		try {
			join();
		} catch (InterruptedException e) {
			Log.e("BYB", "GL Thread couldn't rejoin!", e);
		}
		cleanupGL();
		
	}

	private void cleanupGL() {
		// TODO Auto-generated method stub
		
	}

	private void initGL() {
		// TODO Auto-generated method stub
		
	}

	private void initEGL() {
		// TODO Auto-generated method stub
		
	}
}
