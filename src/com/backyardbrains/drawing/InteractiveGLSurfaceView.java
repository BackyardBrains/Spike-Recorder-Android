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


import com.backyardbrains.BackyardBrainsBaseScopeFragment;
import com.backyardbrains.BackyardBrainsMain;
import com.backyardbrains.R;
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

public class InteractiveGLSurfaceView extends GLSurfaceView  {

	protected TwoDimensionScaleGestureDetector mScaleDetector;
	protected ScaleListener mScaleListener;
	protected SingleFingerGestureDetector singleFingerGestureDetector = null;


	@SuppressWarnings("unused")
	private static final String TAG =  InteractiveGLSurfaceView.class.getCanonicalName();

	BYBBaseRenderer renderer;

	private boolean bNonTouchEnabled = false;
	public static final int MODE_ZOOM_IN_H = 0;
	public static final int MODE_ZOOM_OUT_H = 1;
	public static final int MODE_MOVE = 2;
	public static final int MODE_ZOOM_IN_V = 3;
	public static final int MODE_ZOOM_OUT_V = 4;

	public static final String setNonTouchBroadcastAction = "setNonTouchMode";
	protected int nonTouchMode = -1;
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
		if(bNonTouchEnabled){
			bNonTouchEnabled = false;
			enableNonTouchListeners(false);
		}
		super.surfaceDestroyed(holder);
	}
	@Override
	public boolean onTouchEvent(MotionEvent event) {
//		Log.d(TAG, "onTouchEvent " + event.toString());
		if(renderer != null && singleFingerGestureDetector != null &&  (!bNonTouchEnabled || (bNonTouchEnabled && nonTouchMode == MODE_MOVE))){
			singleFingerGestureDetector.onTouchEvent(event);
			if(singleFingerGestureDetector.hasChanged()){
				renderer.addToGlOffset(singleFingerGestureDetector.getDX(), singleFingerGestureDetector.getDY());
			}
		}
		if(!bNonTouchEnabled) {
			mScaleDetector.onTouchEvent(event);
		}else{
			nonTouchScaling(event);
		}
		return true;
	}
	private void scaleRenderer(){
		if(renderer != null) {
			scaleRenderer(renderer.getSurfaceWidth() * 0.5f);
		}
	}
	private void scaleRenderer(float focusX){
		if(renderer != null && isScalingMode()){
			float scaling = 1;
			if(isZoomIn()){
				scaling = scalingFactorIn;
			}else if(isZoomOut()) {
				scaling = scalingFactorOut;
			}
			if(isScalingHorizontally()){
				renderer.setGlWindowHorizontalSize((int)(renderer.getGlWindowHorizontalSize()*scaling));
				renderer.setScaleFocusX(focusX);
			}else {
				renderer.setGlWindowVerticalSize((int)(renderer.getGlWindowVerticalSize()*scaling));
			}
		}
	}
	private boolean isZoomIn(){
		return nonTouchMode == MODE_ZOOM_IN_H || nonTouchMode == MODE_ZOOM_IN_V;
	}
	private boolean isZoomOut(){
		return nonTouchMode == MODE_ZOOM_OUT_H || nonTouchMode == MODE_ZOOM_OUT_V;
	}
	private boolean isScalingMode(){
		return isZoomIn() || isZoomOut();
	}
	private boolean isScalingHorizontally(){
		return (nonTouchMode == MODE_ZOOM_IN_H || nonTouchMode == MODE_ZOOM_OUT_H);
	}
	private void nonTouchScaling(MotionEvent event) {
		if(bNonTouchEnabled && renderer != null && isScalingMode()){
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
	public void enableNonTouchMode(){
		bNonTouchEnabled = true;
		enableNonTouchListeners(true);
	}
	protected void showScalingInstructions() {
		if (mContext != null && isScalingMode()) {
			Intent i = new Intent();
			i.setAction("showScalingInstructions");
			mContext.sendBroadcast(i);
		}
	}
	private class NonTouchButtonsListener extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.hasExtra("nonTouchMode")){
				int newMode = intent.getIntExtra("nonTouchMode", MODE_MOVE);

				if(newMode == nonTouchMode){
					scaleRenderer();
				}
				nonTouchMode = newMode;
				showScalingInstructions();
			}
		}
	}
	private NonTouchButtonsListener nonTouchButtonsListener;
	private void enableNonTouchListeners(boolean reg) {
		if(mContext != null) {
			if (reg) {
				IntentFilter intentFilter = new IntentFilter(setNonTouchBroadcastAction);
				nonTouchButtonsListener = new NonTouchButtonsListener();
				mContext.registerReceiver(nonTouchButtonsListener, intentFilter);
			} else {
				mContext.unregisterReceiver(nonTouchButtonsListener);
			}
		}
	}
}
