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
import android.widget.ImageView;

public class BYBThresholdHandle {

	private static final String TAG = "BYBThresholdHandle";
	
	protected ImageView					button;
	public Context							context;
	protected String						name;
	private UpdateThresholdHandleListener	updateThresholdHandleListener;
	protected View layoutView;
	// -----------------------------------------------------------------------------------------------------------------------------
	public BYBThresholdHandle(Context context, ImageView button, View view, String name) {
		this.context = context;
		this.button = button;// ((ImageButton)
								// view.findViewById(R.id.thresholdHandle));
		this.layoutView = view;
		this.name = name;
		setButton();
		//setDefaultYPosition();
	}
	// -----------------------------------------------------------------------------------------------------------------------------
	public ImageView getHandlerView() {
		return button;
	}
	// -----------------------------------------------------------------------------------------------------------------------------
	public void setButtonColor(int color){
		this.button.setColorFilter(color);
	}
	// -----------------------------------------------------------------------------------------------------------------------------	
	private void setButton() {
		setButtonColor(BYBColors.asARGB(BYBColors.getColorAsHexById(BYBColors.red)));
		
		////Log.d(TAG, "RED: " + BYBColors.getColorAsHexById(BYBColors.red) + "  " + 0xff0000ff);
		
		OnTouchListener threshTouch = new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (v.getVisibility() == View.VISIBLE) {
					////Log.d("threshold Handle", "y: " + event.getY() + "  view.y: " + v.getY());
					if (event.getActionIndex() == 0) {
						Intent i = new Intent();
						i.setAction("BYBThresholdHandlePos");
						i.putExtra("name", name);
						float pos = (float)setYPosition((int)event.getY());
						i.putExtra("y", pos);
						switch (event.getActionMasked()) {
						case MotionEvent.ACTION_DOWN:
							i.putExtra("action", "down");
							break;
						case MotionEvent.ACTION_MOVE:
							//setYPosition((int)event.getY());
//							v.setY(event.getRawY() - v.getHeight() / 2 );
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
		this.layoutView.setOnTouchListener(threshTouch);
	}
	// -----------------------------------------------------------------------------------------------------------------------------
	public void setDefaultYPosition(){
		float y = layoutView.getHeight()/4;
		Intent i = new Intent();
		i.setAction("BYBThresholdHandlePos");
		i.putExtra("name", name);
		i.putExtra("y", y);
		i.putExtra("action", "up");
		context.sendBroadcast(i);
		setYPosition((int)y);
		}
	public int setYPosition(int pos){
		//Log.d(TAG, "setYPosition " + pos);
		int buttonHalf = (button.getHeight() / 2);
		int p = pos;
//		if(p < buttonHalf){
//			p = buttonHalf;
//		}
//		int hiPos = layoutView.getHeight() - buttonHalf;
//		if(p > hiPos){
//			p = hiPos;
//		}

		button.setY(p - buttonHalf);
		return p;
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
		////Log.d(TAG, "registerListener " + reg);
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
			////Log.d(TAG, "updateTheshold onReceive");
			if (intent.hasExtra("name")) {
				////Log.d(TAG, "---------------------- intent name: " + intent.getStringExtra("name") + "  object name: " + name);
				if (name.equals(intent.getStringExtra("name"))) {
					if (intent.hasExtra("pos")) {
						int pos = intent.getIntExtra("pos", 0);
				//		//Log.d(TAG, "---------------------- pos: " + pos);
						// ImageButton b = thresholdHandle.getImageButton();//
						// (ImageButton)getView().findViewById(R.id.thresholdHandle);
						setYPosition(pos);
						// 	button.setY(pos - (button.getHeight() / 2));
					}
				}
			}
		}
	}

}
