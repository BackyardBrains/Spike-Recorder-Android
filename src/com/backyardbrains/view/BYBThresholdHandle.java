package com.backyardbrains.view;

import com.backyardbrains.R;
import com.backyardbrains.drawing.BYBColors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageButton;

public class BYBThresholdHandle {

	private static final String TAG = "BYBThresholdHandle";
	
	protected ImageButton					button;
	public Context							context;
	protected String						name;
	private UpdateThresholdHandleListener	updateThresholdHandleListener;
	// -----------------------------------------------------------------------------------------------------------------------------
	public BYBThresholdHandle(Context context, ImageButton button, String name) {
		this.context = context;
		this.button = button;// ((ImageButton)
								// view.findViewById(R.id.thresholdHandle));
		this.name = name;
		setButton();
	}
	// -----------------------------------------------------------------------------------------------------------------------------
	public ImageButton getImageButton() {
		return button;
	}
	// -----------------------------------------------------------------------------------------------------------------------------
	public void setButtonColor(int color){
		this.button.setColorFilter(color);
	}
	// -----------------------------------------------------------------------------------------------------------------------------	
	private void setButton() {
		setButtonColor(BYBColors.asARGB(BYBColors.getColorAsHexById(BYBColors.red)));
		
		//Log.d(TAG, "RED: " + BYBColors.getColorAsHexById(BYBColors.red) + "  " + 0xff0000ff);
		
		OnTouchListener threshTouch = new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (v.getVisibility() == View.VISIBLE) {
					//Log.d("threshold Handle", "y: " + event.getY() + "  view.y: " + v.getY());
					if (event.getActionIndex() == 0) {
						Intent i = new Intent();
						i.setAction("BYBThresholdHandlePos");
						i.putExtra("name", name);
						i.putExtra("y", event.getRawY());
						switch (event.getActionMasked()) {
						case MotionEvent.ACTION_DOWN:
							i.putExtra("action", "down");
							break;
						case MotionEvent.ACTION_MOVE:
							v.setY(event.getRawY() - v.getHeight() / 2 );
							i.putExtra("action", "move");
							break;
						case MotionEvent.ACTION_CANCEL:
							i.putExtra("action", "cancel");
							break;
						case MotionEvent.ACTION_UP:
							i.putExtra("action", "up");
							break;
						}
						context.sendBroadcast(i);
					}
					return true;
				}
				return false;
			}
		};
		this.button.setOnTouchListener(threshTouch);
	}
	// -----------------------------------------------------------------------------------------------------------------------------	
	public void setYPosition(int pos){
		//Log.d(TAG, "setYPosition " + pos);
		
		button.setY(pos - (button.getHeight() / 2));
	}
	public void hide(){
		button.setVisibility(View.GONE);
	}
	public void show(){
		button.setVisibility(View.VISIBLE);
	}
	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- BROADCAST RECEIVERS
	// -----------------------------------------------------------------------------------------------------------------------------	
	public void registerUpdateThresholdHandleListener(boolean reg) {
		//Log.d(TAG, "registerListener " + reg);
		if (reg) {
			IntentFilter intentFilter = new IntentFilter("BYBUpdateThresholdHandle");
			updateThresholdHandleListener = new UpdateThresholdHandleListener();
			context.registerReceiver(updateThresholdHandleListener, intentFilter);
		} else {
			context.unregisterReceiver(updateThresholdHandleListener);
			updateThresholdHandleListener = null;
		}
	}
	// -----------------------------------------------------------------------------------------------------------------------------
	private class UpdateThresholdHandleListener extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			//Log.d(TAG, "updateTheshold onReceive");
			if (intent.hasExtra("name")) {
				//Log.d(TAG, "---------------------- intent name: " + intent.getStringExtra("name") + "  object name: " + name);
				if (name.equals(intent.getStringExtra("name"))) {
					if (intent.hasExtra("pos")) {
						int pos = intent.getIntExtra("pos", 0);
				//		Log.d(TAG, "---------------------- pos: " + pos);
						// ImageButton b = thresholdHandle.getImageButton();//
						// (ImageButton)getView().findViewById(R.id.thresholdHandle);
						button.setY(pos - (button.getHeight() / 2));
					}
				}
			}
		}
	}

}
