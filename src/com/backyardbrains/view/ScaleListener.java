package com.backyardbrains.view;

import android.util.Log;
import android.util.Pair;

import com.backyardbrains.drawing.OscilloscopeRenderer;
import com.backyardbrains.view.TwoDimensionScaleGestureDetector.Simple2DOnScaleGestureListener;

public class ScaleListener extends Simple2DOnScaleGestureListener {

	private static final String TAG = ScaleListener.class.getCanonicalName();

	int xSizeAtBeginning = -1;
	int ySizeAtBeginning = -1;
	private OscilloscopeRenderer renderer;

	public ScaleListener(OscilloscopeRenderer r) {
		super();
		this.renderer = r;
	}

	@Override
	public boolean onScaleBegin(TwoDimensionScaleGestureDetector detector) {
		xSizeAtBeginning = renderer.getGlWindowHorizontalSize();
		ySizeAtBeginning = renderer.getGlWindowVerticalSize();

		return super.onScaleBegin(detector);
	}

	@Override
	public boolean onScale(TwoDimensionScaleGestureDetector detector) {

		try {
			final Pair<Float, Float> scaleModifier = detector.getScaleFactor();
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