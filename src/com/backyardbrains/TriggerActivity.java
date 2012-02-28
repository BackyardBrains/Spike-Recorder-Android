package com.backyardbrains;

import com.backyardbrains.view.UIFactory;

public class TriggerActivity extends BackyardAndroidActivity {

	@Override
	protected void enableUiForActivity() {
		UIFactory.hideRecordingButtons(this);
		UIFactory.showSampleSliderBox(this);
	}
}
