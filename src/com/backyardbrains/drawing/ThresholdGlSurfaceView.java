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

import android.content.SharedPreferences;
import android.view.MotionEvent;

import com.backyardbrains.BackyardBrainsMain;
import com.backyardbrains.view.ScaleListener;
import com.backyardbrains.view.TwoDimensionScaleGestureDetector;

public class ThresholdGlSurfaceView extends ContinuousGLSurfaceView {

	@SuppressWarnings("unused")
	private static final String TAG = ContinuousGLSurfaceView.class
			.getCanonicalName();
	protected ThresholdRenderer renderer;
	private float initialThresholdTouch = -1;

	public ThresholdGlSurfaceView(BackyardBrainsMain context) {
		super(context);
	}

	@Override
	protected void assignRenderer(BackyardBrainsMain context) {
		renderer = new ThresholdRenderer(context);
		setRenderer(renderer);
		mScaleDetector = new TwoDimensionScaleGestureDetector(context,
				new ScaleListener(renderer));
	}

	protected void readSettings() {
		//renderer.setAutoScaled(settings.getBoolean("thresholdAutoscaled",
				//renderer.isAutoScaled()));
		//correct for normalized height
		//renderer.adjustThresholdValue(settings.getFloat("savedThreshold", 0) * renderer.height);
		renderer.setGlWindowHorizontalSize(settings.getInt(
				"thresholdGlWindowHorizontalSize",
				renderer.getGlWindowHorizontalSize()));
		renderer.setGlWindowVerticalSize(settings.getInt(
				"thresholdGlWindowVerticalSize",
				renderer.getGlWindowVerticalSize()));
	}

	protected void saveSettings() {
		final SharedPreferences.Editor editor = settings.edit();
		editor.clear();
		//editor.putBoolean("thresholdAutoscaled", renderer.isAutoScaled());
		editor.putInt("thresholdGlWindowHorizontalSize",
				renderer.getGlWindowHorizontalSize());
		//normalize by height
		//editor.putFloat("savedThreshold", (renderer.getThresholdYValue()/renderer.height));
		editor.putInt("thresholdGlWindowVerticalSize",
				renderer.getGlWindowVerticalSize());
		editor.commit();
		

	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		boolean result = super.onTouchEvent(event);
		if (result) return result;
		final int action = event.getAction();
		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN: {
			if (Math.abs(event.getY() - renderer.getThresholdYValue()) < 30) {
				initialThresholdTouch = event.getY();
			}
			break;
		}
		case MotionEvent.ACTION_MOVE: {
			if (!mScaleDetector.isInProgress() && initialThresholdTouch != -1) {

				final float y = event.getY();
				renderer.adjustThresholdValue(y);
			}
			break;
		}
		case MotionEvent.ACTION_POINTER_UP:
		case MotionEvent.ACTION_UP: {
			initialThresholdTouch = -1;
			break;
		}
		}
		return result;
	}


}
