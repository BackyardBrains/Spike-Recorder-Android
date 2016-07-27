package com.backyardbrains.view;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.MotionEvent;

public class SingleFingerGestureDetector {
	private float prevSingleFingerX,prevSingleFingerY, dx, dy;
	private boolean bSingleFingerGesture = false;

	private boolean bChanged = false;
	
	public SingleFingerGestureDetector (){}
	public boolean hasChanged(){
		boolean temp = bChanged;
		bChanged = false;
		return temp;
	}
	public float getDX(){
		if(bSingleFingerGesture){
			return dx;
		}else{
			return 0;
		}
	}
	public float getDY(){
		if(bSingleFingerGesture){
		return dy;
		}else{
			return 0;
		}
	}
	public void onTouchEvent(MotionEvent event) {
		final int action = event.getAction();
			switch (action & MotionEvent.ACTION_MASK) {
						case MotionEvent.ACTION_MOVE:
				if(event.getPointerCount() == 1 && bSingleFingerGesture){
					float rx = event.getRawX();
					float ry = event.getRawY();
					dx = rx - prevSingleFingerX;
					dy = ry - prevSingleFingerY;
					prevSingleFingerX = rx;
					prevSingleFingerY = ry;
					bChanged = true;
//					if(context!=null){
//					Intent i = new Intent();
//					i.setAction("BYBSingleFingerMove");
//					i.putExtra("x", dx);
//					i.putExtra("y", dy);
//					context.sendBroadcast(i);
//					}
					////Log.d("ScaleGestureDet", "dx: " + dx + "  dy: "+ dy );
				}
				
				break;
			case MotionEvent.ACTION_DOWN:
					bSingleFingerGesture = true;
					prevSingleFingerX = event.getRawX();
					prevSingleFingerY = event.getRawY();
					dx = 0;
					dy = 0;
				break;
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_POINTER_UP:
			case MotionEvent.ACTION_POINTER_DOWN:
				bSingleFingerGesture = false;
				break;
			}
		}
	
}
