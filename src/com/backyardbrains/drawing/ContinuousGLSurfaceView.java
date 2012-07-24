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

import android.app.Activity;
import android.content.SharedPreferences;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import com.backyardbrains.BackyardAndroidActivity;
import com.backyardbrains.view.ScaleListener;
import com.backyardbrains.view.TwoDimensionScaleGestureDetector;

public class ContinuousGLSurfaceView extends GLSurfaceView {

	@SuppressWarnings("unused")
	private static final String TAG = ContinuousGLSurfaceView.class
			.getCanonicalName();
	protected TwoDimensionScaleGestureDetector mScaleDetector;

	protected OscilloscopeRenderer renderer;
	protected SharedPreferences settings;

	public ContinuousGLSurfaceView(Activity context) {
		super(context);
		settings = ((BackyardAndroidActivity) context)
				.getPreferences(BackyardAndroidActivity.MODE_PRIVATE);

		assignRenderer(context);
	}

	protected void assignRenderer(Activity context) {
		renderer = new OscilloscopeRenderer(context);
		setRenderer(renderer);
		mScaleDetector = new TwoDimensionScaleGestureDetector(context,
				new ScaleListener(renderer));
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		super.surfaceCreated(holder);
		setKeepScreenOn(true);
		readSettings();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		saveSettings();
		setKeepScreenOn(false);
		super.surfaceDestroyed(holder);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		mScaleDetector.onTouchEvent(event);
		return super.onTouchEvent(event);
	}

	protected void readSettings() {
		renderer.setAutoScaled(settings.getBoolean("continuousAutoscaled",
				renderer.isAutoScaled()));
		renderer.setGlWindowHorizontalSize(settings.getInt(
				"continuousGlWindowHorizontalSize",
				renderer.getGlWindowHorizontalSize()));
		renderer.setGlWindowVerticalSize(settings.getInt(
				"continuousGlWindowVerticalSize",
				renderer.getGlWindowVerticalSize()));
	}

	protected void saveSettings() {
		final SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("continuousAutoscaled", renderer.isAutoScaled());
		editor.putInt("continuousGlWindowHorizontalSize",
				renderer.getGlWindowHorizontalSize());
		editor.putInt("continuousGlWindowVerticalSize",
				renderer.getGlWindowVerticalSize());
		editor.commit();

	}

}
