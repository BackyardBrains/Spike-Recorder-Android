package com.backyardbrains.drawing;

import android.app.Activity;
import android.content.SharedPreferences;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import com.backyardbrains.BackyardAndroidActivity;
import com.backyardbrains.view.TwoDimensionScaleGestureDetector;
import com.backyardbrains.view.TwoDimensionScaleGestureDetector.Simple2DOnScaleGestureListener;

public class ContinuousGLSurfaceView extends GLSurfaceView {

	private static final String TAG = OscilloscopeGLThread.class
			.getCanonicalName();
	protected TwoDimensionScaleGestureDetector mScaleDetector;

	protected OscilloscopeRenderer renderer;
	protected SharedPreferences settings;

	public ContinuousGLSurfaceView(Activity context) {
		super(context);
		settings = ((BackyardAndroidActivity) context)
				.getPreferences(BackyardAndroidActivity.MODE_PRIVATE);

		assignRenderer(context);
		mScaleDetector = new TwoDimensionScaleGestureDetector(context,
				new ScaleListener());
	}

	protected void assignRenderer(Activity context) {
		renderer = new OscilloscopeRenderer(context);
		setRenderer(renderer);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		super.surfaceCreated(holder);
		setKeepScreenOn(true);
		readSettings();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		renderer.onSurfaceDestroyed();
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

	protected class ScaleListener extends Simple2DOnScaleGestureListener {

		int xSizeAtBeginning = -1;
		int ySizeAtBeginning = -1;

		@Override
		public boolean onScaleBegin(TwoDimensionScaleGestureDetector detector) {
			xSizeAtBeginning = renderer.getGlWindowHorizontalSize();
			ySizeAtBeginning = renderer.getGlWindowVerticalSize();

			return super.onScaleBegin(detector);
		}

		@Override
		public boolean onScale(TwoDimensionScaleGestureDetector detector) {

			try {
				final Pair<Float, Float> scaleModifier = detector
						.getScaleFactor();
				int newXsize = (int) (xSizeAtBeginning / scaleModifier.first);
				renderer.setGlWindowHorizontalSize(newXsize);

				int newYsize = (int) (ySizeAtBeginning * scaleModifier.second);

				renderer.setGlWindowVerticalSize(newYsize);
			} catch (IllegalStateException e) {
				Log.e(TAG, "Got invalid values back from Scale listener!");
			} catch (NullPointerException e) {
				Log.e(TAG, "NPE while monitoring scale.");
			}
			return super.onScale(detector);
		}

		@Override
		public void onScaleEnd(TwoDimensionScaleGestureDetector detector) {
			super.onScaleEnd(detector);
		}
	}
}
