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

package com.backyardbrains.view;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.backyardbrains.BackyardBrainsOscilloscopeFragment;
//import com.backyardbrains.BackyardAndroidActivity;
import com.backyardbrains.FileListActivity;
import com.backyardbrains.R;

public class UIFactory {
	//*
//	private TextView msView;
//	private TextView mVView;
//	private UpdateMillisecondsReciever upmillirec;
//	private SetMillivoltViewSizeReceiver milliVoltSize;
//	private UpdateMillivoltReciever upmillivolt;
//	private ShowRecordingButtonsReceiver showRecordingButtonsReceiver;
	//private static BackyardBrainsOscilloscopeFragment oscilloscopeContext = null;
//	private static UIFactory instance = null;
//	private static Context appContext; 
//	protected UIFactory() {}; // singleton like whoa.
//	 
//	
//	public static UIFactory getUi() {
//		if (instance == null) {
//			instance = new UIFactory();
//		}
//		return instance;
//	}
//	// -----------------------------------------------------------------------------------------------------------------------------
//	// ----------------------------------------- REGISTER RECEIVERS
//	// -----------------------------------------------------------------------------------------------------------------------------
//
//	public void registerReceivers(Context context) {
//		appContext = context.getApplicationContext();
//		
//		IntentFilter intentFilter = new IntentFilter("BYBUpdateMillisecondsReciever");
//		upmillirec = new UpdateMillisecondsReciever();
//		appContext.registerReceiver(upmillirec, intentFilter);
//
//		IntentFilter intentFilterVolts = new IntentFilter("BYBUpdateMillivoltReciever");
//		upmillivolt = new UpdateMillivoltReciever();
//		appContext.registerReceiver(upmillivolt, intentFilterVolts);
//
//		IntentFilter intentFilterVoltSize = new IntentFilter("BYBMillivoltsViewSize");
//		milliVoltSize = new SetMillivoltViewSizeReceiver();
//		appContext.registerReceiver(milliVoltSize, intentFilterVoltSize);
//		
//		IntentFilter intentFilterRecordingButtons = new IntentFilter("BYBShowRecordingButtons");
//		showRecordingButtonsReceiver = new ShowRecordingButtonsReceiver();
//		appContext.registerReceiver(showRecordingButtonsReceiver, intentFilterRecordingButtons);
//		
//
//
//	}
//	// -----------------------------------------------------------------------------------------------------------------------------
//	// ----------------------------------------- UNREGISTER RECEIVERS
//	// -----------------------------------------------------------------------------------------------------------------------------
//	public void unregisterReceivers(Context context) {
//		
//		appContext.unregisterReceiver(upmillirec);
//		appContext.unregisterReceiver(upmillivolt);
//		appContext.unregisterReceiver(milliVoltSize);
//		appContext.unregisterReceiver(showRecordingButtonsReceiver);
//		
//	}
//
//	//-----------------------------------------------------------------------------------------------------------------------------
//// ----------------------------------------- SETUPS
//// -----------------------------------------------------------------------------------------------------------------------------
//	public void setupLabels(View v) {
//	msView = (TextView) v.findViewById(R.id.millisecondsView);
//	mVView = (TextView) v.findViewById(R.id.mVLabelView);
//}
//
//public void setDisplayedMilliseconds(Float ms) {
//	msView.setText(ms.toString());
//}
//
//	public static void setupMsLineView(BackyardBrainsOscilloscopeFragment context, View v) {
//		ImageView msLineView = new ImageView(context.getActivity());
//		Bitmap bmp = BitmapFactory.decodeResource(context.getResources(),R.drawable.msline);
//		int width = appContext.getWindowManager().getDefaultDisplay().getWidth() / 3;
//		int height = 2;
//		Bitmap resizedbitmap = Bitmap.createScaledBitmap(bmp, width, height,
//				false);
//		msLineView.setImageBitmap(resizedbitmap);
//		msLineView.setBackgroundColor(Color.BLACK);
//		msLineView.setScaleType(ScaleType.CENTER);
//		
//		LayoutParams rl = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
//		rl.setMargins(0, 0, 0, 20);
//		rl.addRule(RelativeLayout.ABOVE, R.id.millisecondsView);
//		rl.addRule(RelativeLayout.CENTER_HORIZONTAL);
//		
//		RelativeLayout parentLayout = (RelativeLayout) v.findViewById(R.id.parentLayout);
//		parentLayout.addView(msLineView, rl);
//	}
//	
//	
//	public static void setupRecordingButtons(final BackyardBrainsOscilloscopeFragment context, View v) {
//		OnClickListener recordingToggle = new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				context.toggleRecording();
//			}
//		};
//		ImageButton mRecordButton = (ImageButton) v.findViewById(R.id.recordButton);
//		mRecordButton.setOnClickListener(recordingToggle);
//		
//		OnClickListener closeButtonToggle = new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				((ImageButton) v.findViewById(R.id.closeButton)).setVisibility(View.GONE);
//				Log.d("UIFactory", "Close Button Pressed!");
//			}
//		};
//		ImageButton mCloseButton = (ImageButton) v.findViewById(R.id.closeButton);
//		mCloseButton.setOnClickListener(closeButtonToggle);
//		mCloseButton.setVisibility(View.GONE);
//
//		View tapToStopRecView = v.findViewById(R.id.TapToStopRecordingTextView);
//		tapToStopRecView.setOnClickListener(recordingToggle);
//	}
// -----------------------------------------------------------------------------------------------------------------------------
// ----------------------------------------- BROADCAST RECEIVERS CLASS
// -----------------------------------------------------------------------------------------------------------------------------
//
//	public static void hideRecordingButtons(){//BackyardBrainsOscilloscopeFragment context) {
//	//	if(oscilloscopeContext  != null){
//		ImageButton mRecordButton = (ImageButton) oscilloscopeContext.getView().findViewById(R.id.recordButton);
//		
//		if(mRecordButton != null) {
//			mRecordButton.setVisibility(View.GONE);
//		}
//		//}
//	}
//	
//	public static void showRecordingButtons(){//BackyardBrainsOscilloscopeFragment context) {
//		//if(oscilloscopeContext  != null){
//		ImageButton mRecordButton = (ImageButton) oscilloscopeContext.getView().findViewById(R.id.recordButton);
//		
//		if(mRecordButton != null) {
//			mRecordButton.setVisibility(View.VISIBLE);
//		}
//		
//		//}
//	}
//	
//	public void toggleRecording(BackyardBrainsOscilloscopeFragment context, boolean isRecording) {
//		ShowRecordingAnimation anim = new ShowRecordingAnimation(context.getActivity(), isRecording);
//		try {
//			View tapToStopRecView = context.getView().findViewById(R.id.TapToStopRecordingTextView);
//			anim.run();
//			Intent i = new Intent();
//			i.setAction("BYBToggleRecording");
//			context.getActivity().getBaseContext().sendBroadcast(i);
//			if (isRecording == false) {
//				tapToStopRecView.setVisibility(View.VISIBLE);
//			} else {
//				tapToStopRecView.setVisibility(View.GONE);
//			}
//		} catch (RuntimeException e) {
//			Toast.makeText(context.getActivity().getApplicationContext(),
//					"No SD Card is available. Recording is disabled",
//					Toast.LENGTH_LONG).show();
//
//		}
//	}
//
//	public void showCloseButton() {
//		if(appContext  != null){
//			ImageButton mCloseButton = (ImageButton) appContext.getView().findViewById(R.id.closeButton);		
//			if(mCloseButton != null) {
//				
//					mCloseButton.setVisibility(View.VISIBLE);
//					hideRecordingButtons();
//								
//			}
//		}
//	}
//	public void hideCloseButton() {
//		if(appContext  != null){
//			ImageButton mCloseButton = (ImageButton) appContext.getView().findViewById(R.id.closeButton);		
//		if(mCloseButton != null) {
//			if (mCloseButton.getVisibility() == View.VISIBLE) {
//				mCloseButton.setVisibility(View.GONE);
//				showRecordingButtons();
//			}
//		}
//	}
//	}
//
//	public static void toggleCloseButton() {
//		if(appContext  != null){
//			ImageButton mCloseButton = (ImageButton) appContext.getView().findViewById(R.id.closeButton);		
//			if(mCloseButton != null) {
//				if (mCloseButton.getVisibility() == View.VISIBLE) {
//					mCloseButton.setVisibility(View.GONE);
//					showRecordingButtons();
//				} else {
//					mCloseButton.setVisibility(View.VISIBLE);
//					hideRecordingButtons();
//				}				
//			}
//		}
//	}
//	
	public static void toggleSeekbar(final BackyardBrainsOscilloscopeFragment context) {
		
		View samplesSeekBar = context.getView().findViewById(R.id.samplesSeekBar);
		if (samplesSeekBar.getVisibility() == View.VISIBLE) {
			samplesSeekBar.setVisibility(View.INVISIBLE);
		} else {
			samplesSeekBar.setVisibility(View.VISIBLE);
		}
	}
	
	public static void showSampleSliderBox(BackyardBrainsOscilloscopeFragment context) {
		View triggerViewSampleChanger = context.getView().findViewById(R.id.triggerViewSampleChangerLayout);
		if (triggerViewSampleChanger != null) {
			triggerViewSampleChanger.setVisibility(View.VISIBLE);
		}
		View numberOfSamplesLabel = context.getView().findViewById(R.id.numberOfSamplesAveraged);
		if (numberOfSamplesLabel != null) {
			numberOfSamplesLabel.setVisibility(View.VISIBLE);
		}
	}
	
	public static void hideSampleSliderBox(BackyardBrainsOscilloscopeFragment context) {
		View triggerViewSampleChanger = context.getView().findViewById(R.id.triggerViewSampleChangerLayout);
		if (triggerViewSampleChanger != null) {
			triggerViewSampleChanger.setVisibility(View.GONE);
		}
		View numberOfSamplesLabel = context.getView().findViewById(R.id.numberOfSamplesAveraged);
		if (numberOfSamplesLabel != null) {
			numberOfSamplesLabel.setVisibility(View.GONE);
		}
	}
//// -----------------------------------------------------------------------------------------------------------------------------
//// ----------------------------------------- BROADCAST RECEIVERS CLASS
//// -----------------------------------------------------------------------------------------------------------------------------
//
//	private class UpdateMillisecondsReciever extends BroadcastReceiver {
//		@Override
//		public void onReceive(android.content.Context context,
//				android.content.Intent intent) {
//			msView.setText(intent.getStringExtra("millisecondsDisplayedString"));
//		};
//	}
//
//	private class UpdateMillivoltReciever extends BroadcastReceiver {
//		@Override
//		public void onReceive(android.content.Context context,
//				android.content.Intent intent) {
//			mVView.setText(intent.getStringExtra("millivoltsDisplayedString"));
//		};
//	}
//
//	private class SetMillivoltViewSizeReceiver extends BroadcastReceiver {
//		@Override
//		public void onReceive(android.content.Context context,
//				android.content.Intent intent) {
//			mVView.setHeight(intent.getIntExtra("millivoltsViewNewSize", mVView.getHeight()));
//		};
//	}
//	
//	private class ShowRecordingButtonsReceiver extends BroadcastReceiver {
//		@Override
//		public void onReceive(android.content.Context context, android.content.Intent intent) {
//			boolean yesno = intent.getBooleanExtra("showRecordingButton", true);
//			if (yesno) {
//				showRecordingButtons();
//			} else {
//				hideRecordingButtons();
//			}
//		}	
//	}
// -----------------------------------------------------------------------------------------------------------------------------
// ----------------------------------------- ANIMATIONS
// -----------------------------------------------------------------------------------------------------------------------------
//
//	private class ShowRecordingAnimation implements Runnable {
//
//		private Activity activity;
//		private boolean recording;
//
//		public ShowRecordingAnimation(Activity a, Boolean b) {
//			this.activity = a;
//			this.recording = b;
//		}
//		
//		@Override
//		public void run() {
//			Animation a = null;
//			if (this.recording == false) {
//				a = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0,
//						Animation.RELATIVE_TO_SELF, 0,
//						Animation.RELATIVE_TO_SELF, -1,
//						Animation.RELATIVE_TO_SELF, 0);
//			} else {
//				a = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0,
//						Animation.RELATIVE_TO_SELF, 0,
//						Animation.RELATIVE_TO_SELF, 0,
//						Animation.RELATIVE_TO_SELF, -1);
//			}
//			a.setDuration(250);
//			a.setInterpolator(AnimationUtils.loadInterpolator(this.activity,
//					android.R.anim.anticipate_overshoot_interpolator));
//			View stopRecView = activity.findViewById(R.id.TapToStopRecordingTextView);
//			stopRecView.startAnimation(a);
//		}
//
//	}
//*/
}
