package com.backyardbrains.drawing;

import android.app.Activity;
import android.content.SharedPreferences;
import android.view.MotionEvent;

import com.backyardbrains.view.ScaleListener;
import com.backyardbrains.view.TwoDimensionScaleGestureDetector;

public class ThresholdGlSurfaceView extends ContinuousGLSurfaceView {

	@SuppressWarnings("unused")
	private static final String TAG = ContinuousGLSurfaceView.class
			.getCanonicalName();
	protected ThresholdRenderer renderer;
	private float initialThresholdTouch = -1;

	public ThresholdGlSurfaceView(Activity context) {
		super(context);
	}

	@Override
	protected void assignRenderer(Activity context) {
		renderer = new ThresholdRenderer(context);
		setRenderer(renderer);
		mScaleDetector = new TwoDimensionScaleGestureDetector(context,
				new ScaleListener(renderer));
	}

	protected void readSettings() {
		renderer.setAutoScaled(settings.getBoolean("thresholdAutoscaled",
				renderer.isAutoScaled()));
		renderer.setGlWindowHorizontalSize(settings.getInt(
				"thresholdGlWindowHorizontalSize",
				renderer.getGlWindowHorizontalSize()));
		renderer.setGlWindowVerticalSize(settings.getInt(
				"thresholdGlWindowVerticalSize",
				renderer.getGlWindowVerticalSize()));
	}

	protected void saveSettings() {
		final SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("thresholdAutoscaled", renderer.isAutoScaled());
		editor.putInt("thresholdGlWindowHorizontalSize",
				renderer.getGlWindowHorizontalSize());
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
