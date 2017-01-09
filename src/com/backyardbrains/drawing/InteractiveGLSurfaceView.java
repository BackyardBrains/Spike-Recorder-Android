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


import com.backyardbrains.BackyardBrainsApplication;
import com.backyardbrains.BackyardBrainsBaseScopeFragment;
import com.backyardbrains.BackyardBrainsMain;
import com.backyardbrains.R;
import com.backyardbrains.view.BYBZoomButton;
import com.backyardbrains.view.ScaleListener;
import com.backyardbrains.view.SingleFingerGestureDetector;
import com.backyardbrains.view.TwoDimensionScaleGestureDetector;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;

import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.widget.EditText;

import java.io.File;

import static android.view.MotionEvent.TOOL_TYPE_FINGER;
import static android.view.MotionEvent.TOOL_TYPE_MOUSE;
import static android.view.MotionEvent.TOOL_TYPE_STYLUS;
import static android.view.MotionEvent.TOOL_TYPE_UNKNOWN;

public class InteractiveGLSurfaceView extends GLSurfaceView  {

	protected TwoDimensionScaleGestureDetector mScaleDetector;
	protected ScaleListener mScaleListener;
	protected SingleFingerGestureDetector singleFingerGestureDetector = null;


	@SuppressWarnings("unused")
	private static final String TAG =  InteractiveGLSurfaceView.class.getCanonicalName();

	BYBBaseRenderer renderer;

	private boolean bZoomButtonsEnabled = false;
	public static final int MODE_ZOOM_IN_H = 0;
	public static final int MODE_ZOOM_OUT_H = 1;
	public static final int MODE_MOVE = 2;
	public static final int MODE_ZOOM_IN_V = 3;
	public static final int MODE_ZOOM_OUT_V = 4;


//	protected int nonTouchMode = -1;
	float startTouchX;
	float startTouchY;
	float minScalingDistance = 5;
	float scalingFactor = 0.5f;
	float scalingFactorOut;
	float scalingFactorIn;
	boolean bIsNonTouchScaling = false;
	Context mContext;

	public InteractiveGLSurfaceView(Context context, BYBBaseRenderer renderer) {
		super(context);
		setEGLConfigChooser(8, 8, 8, 8, 16, 0);
		scalingFactorIn = 1 - scalingFactor;
		scalingFactorOut = 1 + scalingFactor;

		mContext = context;
		this.renderer = renderer;
		setRenderer(renderer);
		mScaleListener = new ScaleListener();
		mScaleDetector = new TwoDimensionScaleGestureDetector(context, mScaleListener);
		mScaleListener.setRenderer(renderer);
		singleFingerGestureDetector = new SingleFingerGestureDetector(context);
		getHolder().setFormat( PixelFormat.RGBA_8888 );

		if(mContext != null){
			boolean bHasTouch = ((BackyardBrainsApplication)mContext.getApplicationContext()).isTouchSupported();
			enableZoomButtons(!bHasTouch);
		}
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
		if(bZoomButtonsEnabled){
			bZoomButtonsEnabled = false;
			enableZoomButtonListeners(false);
		}
		super.surfaceDestroyed(holder);
	}
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(event.getPointerCount()>0) {
			switch (event.getToolType(0)) {
				case TOOL_TYPE_UNKNOWN:
				case TOOL_TYPE_STYLUS:
				case TOOL_TYPE_MOUSE:
					enableZoomButtons(true);
					break;
				case TOOL_TYPE_FINGER:
					enableZoomButtons(false);
					break;
			}
		}
		Log.d(TAG, "onTouchEvent " + event.toString());
		if (renderer != null) {
			if (singleFingerGestureDetector != null) {
				singleFingerGestureDetector.onTouchEvent(event);
				if (singleFingerGestureDetector.hasChanged()) {
					renderer.addToGlOffset(singleFingerGestureDetector.getDX(), singleFingerGestureDetector.getDY());
				}
			}
			if (mScaleDetector != null) {
				mScaleDetector.onTouchEvent(event);
			}
		}
		return true;
	}
	private void scaleRenderer(int zoomMode){
		if(renderer != null) {
			scaleRenderer(renderer.getSurfaceWidth() * 0.5f, zoomMode);
		}
	}
	private void scaleRenderer(float focusX, int zoomMode){
		if(renderer != null && isScalingMode(zoomMode)){
			float scaling = 1;
			if(isZoomIn(zoomMode)){
				scaling = scalingFactorIn;
			}else if(isZoomOut(zoomMode)) {
				scaling = scalingFactorOut;
			}
			if(isScalingHorizontally(zoomMode)){
				renderer.setGlWindowHorizontalSize((int)(renderer.getGlWindowHorizontalSize()*scaling));
				renderer.setScaleFocusX(focusX);
			}else {
				renderer.setGlWindowVerticalSize((int)(renderer.getGlWindowVerticalSize()*scaling));
			}
		}
	}
	private boolean isZoomIn(int mode){
		return mode == MODE_ZOOM_IN_H || mode == MODE_ZOOM_IN_V;
	}
	private boolean isZoomOut(int mode){
		return mode == MODE_ZOOM_OUT_H || mode == MODE_ZOOM_OUT_V;
	}
	private boolean isScalingMode(int mode){
		return isZoomIn(mode) || isZoomOut(mode);
	}
	private boolean isScalingHorizontally(int mode){
		return (mode == MODE_ZOOM_IN_H || mode == MODE_ZOOM_OUT_H);
	}
/*
	private void nonTouchScaling(MotionEvent event) {
		if(bZoomButtonsEnabled && renderer != null && isScalingMode()){
			final int action = event.getAction();
			float distanceX=0;
			float distanceY=0;
			if(bIsNonTouchScaling) {
				distanceX = Math.abs(event.getX() - startTouchX);
				distanceY = Math.abs(event.getY() - startTouchY);
			}
			switch (action & MotionEvent.ACTION_MASK) {
				case MotionEvent.ACTION_DOWN:
					bIsNonTouchScaling = true;
					startTouchX = event.getX();
					startTouchY = event.getY();
					break;
				case MotionEvent.ACTION_UP:
					if(bIsNonTouchScaling) {
						if (isScalingHorizontally() && distanceX > minScalingDistance) {
								renderer.setGlWindowUnscaledHorizontalSize(distanceX);
								renderer.setScaleFocusX(distanceX / 2 + Math.min(event.getX(), startTouchX));
						}else if (!isScalingHorizontally() && distanceY > minScalingDistance) {
								renderer.setGlWindowUnscaledVerticalSize(distanceY);
						}else{
//							renderer.setGlWindowHorizontalSize((int)(renderer.getGlWindowHorizontalSize()*zoomFactor));
//							renderer.setScaleFocusX(startTouchX);
							scaleRenderer(startTouchX);
						}
					}
					renderer.hideScalingArea();
					bIsNonTouchScaling = false;
					break;
				case MotionEvent.ACTION_MOVE:
					if(bIsNonTouchScaling) {
						if (isScalingHorizontally() && distanceX > minScalingDistance) {
							renderer.showScalingAreaX(startTouchX, event.getX());
						}else if (!isScalingHorizontally() && distanceY > minScalingDistance) {
							renderer.showScalingAreaY(startTouchY, event.getY());
						}
					}
					break;
				case MotionEvent.ACTION_CANCEL:
					renderer.hideScalingArea();
					bIsNonTouchScaling = false;
					break;
			}
		}
	}
	//*/
	public void enableZoomButtons(boolean bEnable) {
		boolean bBroadcast = false;
		if (bEnable && !bZoomButtonsEnabled) {
			bZoomButtonsEnabled = true;
			enableZoomButtonListeners(true);
			bBroadcast = true;
		} else if (!bEnable && bZoomButtonsEnabled) {
			bZoomButtonsEnabled = false;
			enableZoomButtonListeners(false);
			bBroadcast = true;
		}
		if(mContext != null && bBroadcast){
			Intent i = new Intent();
			i.setAction("BYBShowZoomUI");
			i.putExtra("showUI",bZoomButtonsEnabled);
			mContext.sendBroadcast(i);
		}
	}
//	protected void showScalingInstructions() {
//		if (mContext != null && isScalingMode()) {
//			Intent i = new Intent();
//			i.setAction("showScalingInstructions");
//			mContext.sendBroadcast(i);
//		}
//	}
	private class ZoomButtonsListener extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.hasExtra("zoomMode")){
				int zoomMode = intent.getIntExtra("zoomMode", MODE_ZOOM_IN_H);
				scaleRenderer(zoomMode);
			}
		}
	}
	private ZoomButtonsListener zoomButtonsListener;
	private void enableZoomButtonListeners(boolean reg) {
		if(mContext != null) {
			if (reg) {
				IntentFilter intentFilter = new IntentFilter(BYBZoomButton.broadcastAction);
				zoomButtonsListener = new ZoomButtonsListener();
				mContext.registerReceiver(zoomButtonsListener, intentFilter);
			} else {
				mContext.unregisterReceiver(zoomButtonsListener);
			}
		}
	}
}
