package com.backyardbrains.drawing;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.MotionEvent;

public class ThresholdGlSurfaceView extends OscilloscopeGLSurfaceView {

	private static final String TAG = ThresholdGlSurfaceView.class.getCanonicalName();
	private float initialThresholdTouch = -1;

	public ThresholdGlSurfaceView(Context context) {
		super(context);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		boolean result = super.onTouchEvent(event);
		final int action = event.getAction();
			switch (action & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN: {
				if (Math.abs(event.getY() - mGLThread.getThresholdYValue()) < 30) {
					initialThresholdTouch = event.getY();
				}
				break;
			}
			case MotionEvent.ACTION_MOVE: {
				if (!mScaleDetector.isInProgress()
						&& initialThresholdTouch != -1) {

					final float y = event.getY();
					getContext().sendBroadcast(
							new Intent("BYBThresholdChange").putExtra(
									"deltathreshold", y));

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
	
	@Override
	protected void assignThread() {
		Log.d(TAG, "Creating Trigger View Thread");
		mGLThread = new TriggerViewThread(this);
	}
}
