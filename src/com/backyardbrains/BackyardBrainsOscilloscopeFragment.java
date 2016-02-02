package com.backyardbrains;

import com.backyardbrains.drawing.ContinuousGLSurfaceView;
import com.backyardbrains.drawing.ThresholdRenderer;
import com.backyardbrains.drawing.WaveformRenderer;
import com.backyardbrains.view.TwoDimensionScaleGestureDetector;
import com.backyardbrains.view.UIFactory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

public class BackyardBrainsOscilloscopeFragment extends Fragment implements CustomFragment {

    //public static final String ARG_SECTION_NUMBER = "section_number";
//*
	 protected ContinuousGLSurfaceView mAndroidSurface;
		private boolean isRecording = false;
		private FrameLayout mainscreenGLLayout;
		private SharedPreferences settings;
		private BackyardBrainsMain context;
		private WaveformRenderer waveRenderer;
		private ThresholdRenderer threshRenderer;
		protected TwoDimensionScaleGestureDetector mScaleDetector;
	public BackyardBrainsOscilloscopeFragment (BackyardBrainsMain context) {
			super();
			setContext(context);
			Log.d("BackyardBrainsOscilloscopeFragment", "Constructor");
		}
		
		
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.backyard_main, container, false);
        //((TextView) rootView.findViewById(android.R.id.text1)).setText("Oscilloscope");
        Log.d("BackyardBrainsOscilloscopeFragment", "onCreateView");
		getSettings();
		mainscreenGLLayout = (FrameLayout) rootView.findViewById(R.id.glContainer);
		reassignSurfaceView();
        return rootView;
    }
    public void setContext(BackyardBrainsMain context){
    	this.context = context;
    }
    //*
   
	/*
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		

		UIFactory.getUi().setupLabels(this);
		UIFactory.setupMsLineView(this);
		UIFactory.setupRecordingButtons(this);
		UIFactory.setupSampleSlider(this);
		
	    }
//*/
	protected void reassignSurfaceView() {
		mAndroidSurface = null;
		mainscreenGLLayout.removeAllViews();
		setGlSurface();
		mainscreenGLLayout.addView(mAndroidSurface);
		enableUiForActivity();
		Log.d(getClass().getCanonicalName(), "Reassigned OscilloscopeGLSurfaceView");
	}

	protected void enableUiForActivity() {
		//UIFactory.showRecordingButtons(this);
		//UIFactory.hideSampleSliderBox(this);
	}

	protected void setGlSurface() {
		if(context != null){
		mAndroidSurface = new ContinuousGLSurfaceView(context);
		}
	}
/*
	@Override
	protected void onResume() {
		//UIFactory.getUi().registerReceivers(this);
		reassignSurfaceView();
		//bindAudioService(true);
		super.onResume();
	}

	@Override
	protected void onPause() {
		mAndroidSurface = null;
		UIFactory.getUi().unregisterReceivers(this);
		super.onPause();
	}


	
	@Override
	protected void onStop() {
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("triggerAutoscaled", false);
		editor.putBoolean("continuousAutoscaled", false);
		editor.commit();
		super.onStop();
		//finish();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
//*/
	//*
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return mAndroidSurface.onTouchEvent(event);
		
	}
//*/
	public void toggleRecording() {
		//UIFactory.getUi().toggleRecording(this, isRecording);
		isRecording = !isRecording;
	}

	public void setDisplayedMilliseconds(Float ms) {
		//UIFactory.getUi().setDisplayedMilliseconds(ms);
	}

	private void getSettings() {
		if (settings == null) {
			settings = ((BackyardBrainsMain) context).getPreferences(BackyardBrainsMain.MODE_PRIVATE);
		}
	}
    //*/
    
}
