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
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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

import com.backyardbrains.BackyardAndroidActivity;
import com.backyardbrains.FileListActivity;
import com.backyardbrains.R;

public class UIFactory {
	
	private TextView msView;
	private TextView mVView;
	private UpdateMillisecondsReciever upmillirec;
	private SetMillivoltViewSizeReceiver milliVoltSize;
	private UpdateMillivoltReciever upmillivolt;
	private ShowRecordingButtonsReceiver showRecordingButtonsReceiver;
	
	private static UIFactory instance = null;
	protected UIFactory() {}; // singleton like whoa.
	
	
	public static UIFactory getUi() {
		if (instance == null) {
			instance = new UIFactory();
		}
		return instance;
	}
	
	public void registerReceivers(Activity context) {
		
		IntentFilter intentFilter = new IntentFilter(
				"BYBUpdateMillisecondsReciever");
		upmillirec = new UpdateMillisecondsReciever();
		context.registerReceiver(upmillirec, intentFilter);

		IntentFilter intentFilterVolts = new IntentFilter(
				"BYBUpdateMillivoltReciever");
		upmillivolt = new UpdateMillivoltReciever();
		context.registerReceiver(upmillivolt, intentFilterVolts);

		IntentFilter intentFilterVoltSize = new IntentFilter(
				"BYBMillivoltsViewSize");
		milliVoltSize = new SetMillivoltViewSizeReceiver();
		context.registerReceiver(milliVoltSize, intentFilterVoltSize);
		
		IntentFilter intentFilterRecordingButtons = new IntentFilter(
				"BYBShowRecordingButtons");
		showRecordingButtonsReceiver = new ShowRecordingButtonsReceiver();
		context.registerReceiver(showRecordingButtonsReceiver, intentFilterRecordingButtons);

	}


	public void unregisterReceivers(Activity context) {
		context.unregisterReceiver(upmillirec);
		context.unregisterReceiver(upmillivolt);
		context.unregisterReceiver(milliVoltSize);
		context.unregisterReceiver(showRecordingButtonsReceiver);
	}
	
	public void setupLabels(BackyardAndroidActivity context) {
		msView = (TextView) context.findViewById(R.id.millisecondsView);
		mVView = (TextView) context.findViewById(R.id.mVLabelView);
	}
	
	public void setDisplayedMilliseconds(Float ms) {
		msView.setText(ms.toString());
	}
	
	public static void setupMsLineView(BackyardAndroidActivity context) {
		ImageView msLineView = new ImageView(context);
		Bitmap bmp = BitmapFactory.decodeResource(context.getResources(),
				R.drawable.msline);
		int width = context.getWindowManager().getDefaultDisplay().getWidth() / 3;
		int height = 2;
		Bitmap resizedbitmap = Bitmap.createScaledBitmap(bmp, width, height,
				false);
		msLineView.setImageBitmap(resizedbitmap);
		msLineView.setBackgroundColor(Color.BLACK);
		msLineView.setScaleType(ScaleType.CENTER);
		
		LayoutParams rl = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		rl.setMargins(0, 0, 0, 20);
		rl.addRule(RelativeLayout.ABOVE, R.id.millisecondsView);
		rl.addRule(RelativeLayout.CENTER_HORIZONTAL);
		
		RelativeLayout parentLayout = (RelativeLayout) context.findViewById(R.id.parentLayout);
		parentLayout.addView(msLineView, rl);
	}

	public static void setupRecordingButtons(final BackyardAndroidActivity context) {
		OnClickListener recordingToggle = new OnClickListener() {
			@Override
			public void onClick(View v) {
				context.toggleRecording();
			}
		};
		ImageButton mRecordButton = (ImageButton) context.findViewById(R.id.recordButton);
		mRecordButton.setOnClickListener(recordingToggle);
		
		OnClickListener fileViewclick = new OnClickListener() {
			public void onClick(View view) {
				Intent i = new Intent(view.getContext(), FileListActivity.class);
				context.startActivityForResult(i, 0);
			}

		};
		ImageButton mFileButton = (ImageButton) context.findViewById(R.id.fileButton);
		mFileButton.setOnClickListener(fileViewclick);
		
		View tapToStopRecView = context.findViewById(R.id.TapToStopRecordingTextView);
		tapToStopRecView.setOnClickListener(recordingToggle);
	}
	
	public static void hideRecordingButtons(BackyardAndroidActivity context) {
		ImageButton mRecordButton = (ImageButton) context.findViewById(R.id.recordButton);
		ImageButton mFileButton = (ImageButton) context.findViewById(R.id.fileButton);
		ImageView recordingBackground = (ImageView) context.findViewById(R.id.recordButtonBackground);
		
		if(mFileButton != null) {
			mFileButton.setVisibility(View.GONE);
		}
		if(mRecordButton != null) {
			mRecordButton.setVisibility(View.GONE);
		}
		if(recordingBackground != null) {
			recordingBackground.setVisibility(View.GONE);
		}
	}
	
	public static void showRecordingButtons(BackyardAndroidActivity context) {
		ImageButton mRecordButton = (ImageButton) context.findViewById(R.id.recordButton);
		ImageButton mFileButton = (ImageButton) context.findViewById(R.id.fileButton);
		ImageView recordingBackground = (ImageView) context.findViewById(R.id.recordButtonBackground);

		if(mFileButton != null) {
			mFileButton.setVisibility(View.VISIBLE);
		}
		if(mRecordButton != null) {
			mRecordButton.setVisibility(View.VISIBLE);
		}
		if(recordingBackground != null) {
			recordingBackground.setVisibility(View.VISIBLE);
		}
		
	}
	
	public void toggleRecording(BackyardAndroidActivity context, boolean isRecording) {
		ShowRecordingAnimation anim = new ShowRecordingAnimation(context, isRecording);
		try {
			View tapToStopRecView = context.findViewById(R.id.TapToStopRecordingTextView);
			anim.run();
			Intent i = new Intent();
			i.setAction("BYBToggleRecording");
			context.getBaseContext().sendBroadcast(i);
			if (isRecording == false) {
				tapToStopRecView.setVisibility(View.VISIBLE);
			} else {
				tapToStopRecView.setVisibility(View.GONE);
			}
		} catch (RuntimeException e) {
			Toast.makeText(context.getApplicationContext(),
					"No SD Card is available. Recording is disabled",
					Toast.LENGTH_LONG).show();

		}
	}
	
	public static void setupSampleSlider(final BackyardAndroidActivity context) {
		//LinearLayout triggerViewSampleChanger = (LinearLayout) context.findViewById(R.id.triggerViewSampleChangerLayout);
	
		final TextView numberOfSamplesLabel = (TextView) context.findViewById(R.id.numberOfSamplesAveraged);
		OnClickListener toggleSeekbarListener = new OnClickListener() {
			@Override public void onClick(View v) {
				toggleSeekbar(context);
			}
		};
		numberOfSamplesLabel.setOnClickListener(toggleSeekbarListener);
		
		SeekBar samplesSeekBar = (SeekBar) context.findViewById(R.id.samplesSeekBar);
		samplesSeekBar.setMax(49);
		samplesSeekBar.setProgress(9);
		samplesSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			
			@Override public void onStopTrackingTouch(SeekBar seekBar) { 
			}
			
			@Override public void onStartTrackingTouch(SeekBar seekBar) {
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if (!fromUser) return;
				Intent i = new Intent("setSampleSize").putExtra("newSampleSize", progress+1);
				context.sendBroadcast(i);
				numberOfSamplesLabel.setText((progress+1)+" x");
			}
		});
	}
	
	public static void toggleSeekbar(final BackyardAndroidActivity context) {
		View samplesSeekBar = context.findViewById(R.id.samplesSeekBar);
		if (samplesSeekBar.getVisibility() == View.VISIBLE) {
			samplesSeekBar.setVisibility(View.INVISIBLE);
		} else {
			samplesSeekBar.setVisibility(View.VISIBLE);
		}
	}
	
	public static void showSampleSliderBox(BackyardAndroidActivity context) {
		View triggerViewSampleChanger = context.findViewById(R.id.triggerViewSampleChangerLayout);
		if (triggerViewSampleChanger != null) {
			triggerViewSampleChanger.setVisibility(View.VISIBLE);
		}
		View numberOfSamplesLabel = context.findViewById(R.id.numberOfSamplesAveraged);
		if (numberOfSamplesLabel != null) {
			numberOfSamplesLabel.setVisibility(View.VISIBLE);
		}
	}
	
	public static void hideSampleSliderBox(BackyardAndroidActivity context) {
		View triggerViewSampleChanger = context.findViewById(R.id.triggerViewSampleChangerLayout);
		if (triggerViewSampleChanger != null) {
			triggerViewSampleChanger.setVisibility(View.GONE);
		}
		View numberOfSamplesLabel = context.findViewById(R.id.numberOfSamplesAveraged);
		if (numberOfSamplesLabel != null) {
			numberOfSamplesLabel.setVisibility(View.GONE);
		}
	}

	private class UpdateMillisecondsReciever extends BroadcastReceiver {
		@Override
		public void onReceive(android.content.Context context,
				android.content.Intent intent) {
			msView.setText(intent.getStringExtra("millisecondsDisplayedString"));
		};
	}

	private class UpdateMillivoltReciever extends BroadcastReceiver {
		@Override
		public void onReceive(android.content.Context context,
				android.content.Intent intent) {
			mVView.setText(intent.getStringExtra("millivoltsDisplayedString"));
		};
	}

	private class SetMillivoltViewSizeReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(android.content.Context context,
				android.content.Intent intent) {
			mVView.setHeight(intent.getIntExtra("millivoltsViewNewSize", mVView.getHeight()));
		};
	}
	
	private class ShowRecordingButtonsReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(android.content.Context context,
				android.content.Intent intent) {
			boolean yesno = intent.getBooleanExtra("showRecordingButton", true);
			if (yesno) {
				showRecordingButtons((BackyardAndroidActivity) context);
			} else {
				hideRecordingButtons((BackyardAndroidActivity) context);
			}
		}	
		}
	
	private class ShowRecordingAnimation implements Runnable {

		private Activity activity;
		private boolean recording;

		public ShowRecordingAnimation(Activity a, Boolean b) {
			this.activity = a;
			this.recording = b;
		}
		
		@Override
		public void run() {
			Animation a = null;
			if (this.recording == false) {
				a = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0,
						Animation.RELATIVE_TO_SELF, 0,
						Animation.RELATIVE_TO_SELF, -1,
						Animation.RELATIVE_TO_SELF, 0);
			} else {
				a = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0,
						Animation.RELATIVE_TO_SELF, 0,
						Animation.RELATIVE_TO_SELF, 0,
						Animation.RELATIVE_TO_SELF, -1);
			}
			a.setDuration(250);
			a.setInterpolator(AnimationUtils.loadInterpolator(this.activity,
					android.R.anim.anticipate_overshoot_interpolator));
			View stopRecView = activity.findViewById(R.id.TapToStopRecordingTextView);
			stopRecView.startAnimation(a);
		}

	}

}
