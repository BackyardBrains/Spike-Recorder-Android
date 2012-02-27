package com.backyardbrains;

import com.backyardbrains.drawing.ThresholdGlSurfaceView;
import com.backyardbrains.view.UIFactory;

public class TriggerActivity extends BackyardAndroidActivity {

	@Override
	protected void setGlSurface() {
		// TODO Auto-generated method stub
		mAndroidSurface = new ThresholdGlSurfaceView(this);
	}
	
	@Override
	protected void enableUiForActivity() {
		UIFactory.hideRecordingButtons(this);
		UIFactory.showSampleSliderBox(this);
	}
}
