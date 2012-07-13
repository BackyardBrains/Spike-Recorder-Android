package com.backyardbrains;

import android.content.Intent;

import com.backyardbrains.drawing.ThresholdGlSurfaceView;
import com.backyardbrains.view.UIFactory;

public class TriggerActivity extends BackyardAndroidActivity {

	@Override
	protected void setGlSurface() {
		mAndroidSurface = new ThresholdGlSurfaceView(this);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		broadcastToggleTrigger(true);
	}
	
	@Override
	protected void onPause() {
		broadcastToggleTrigger(false);
		super.onPause();
	}

	private void broadcastToggleTrigger(boolean b) {
		Intent i = new Intent("BYBToggleTrigger").putExtra("triggerMode", b);
		sendBroadcast(i);
	}
	
	@Override
	protected void enableUiForActivity() {
		UIFactory.hideRecordingButtons(this);
		UIFactory.showSampleSliderBox(this);
	}
}
