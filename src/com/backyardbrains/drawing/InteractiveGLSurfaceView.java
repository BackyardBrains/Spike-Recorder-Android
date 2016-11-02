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

package com.backyardbrains.drawing;


import com.backyardbrains.BackyardBrainsMain;
import com.backyardbrains.view.ScaleListener;
import com.backyardbrains.view.SingleFingerGestureDetector;
import com.backyardbrains.view.TwoDimensionScaleGestureDetector;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

public class InteractiveGLSurfaceView extends GLSurfaceView  {

	protected TwoDimensionScaleGestureDetector mScaleDetector;
	protected ScaleListener mScaleListener;
	protected SingleFingerGestureDetector singleFingerGestureDetector = null;


	@SuppressWarnings("unused")
	private static final String TAG =  InteractiveGLSurfaceView.class.getCanonicalName();

	BYBBaseRenderer renderer;

	public InteractiveGLSurfaceView(Context context, BYBBaseRenderer renderer) {
		super(context);
		this.renderer = renderer;
		setRenderer(renderer);
		mScaleListener = new ScaleListener();
		mScaleDetector = new TwoDimensionScaleGestureDetector(context, mScaleListener);
		mScaleListener.setRenderer(renderer);
		singleFingerGestureDetector = new SingleFingerGestureDetector(context);
	}
//*
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		super.surfaceCreated(holder);
		setKeepScreenOn(true);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		setKeepScreenOn(false);
		super.surfaceDestroyed(holder);
	}
	@Override
	public boolean onTouchEvent(MotionEvent event) {
//		Log.d(TAG, "onTouchEvent " + event.toString());
		if(renderer != null && singleFingerGestureDetector != null){
			singleFingerGestureDetector.onTouchEvent(event);
			if(singleFingerGestureDetector.hasChanged()){
				renderer.addToGlOffset(singleFingerGestureDetector.getDX(), singleFingerGestureDetector.getDY());
			}
		}
		mScaleDetector.onTouchEvent(event);

		return true;
	}
}
