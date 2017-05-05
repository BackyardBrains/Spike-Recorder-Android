package com.backyardbrains.view;

import android.content.Context;
import android.content.Intent;
import android.view.MotionEvent;

public class SingleFingerGestureDetector {
    private float prevSingleFingerX, prevSingleFingerY, dx, dy;
    private boolean bSingleFingerGesture = false;

    private boolean bChanged = false;
    private Context context;

    public SingleFingerGestureDetector(Context context) {
        this.context = context;
    }

    public boolean hasChanged() {
        boolean temp = bChanged;
        bChanged = false;
        return temp;
    }

    public float getDX() {
        if (bSingleFingerGesture) {
            return dx;
        } else {
            return 0;
        }
    }

    public float getDY() {
        if (bSingleFingerGesture) {
            return dy;
        } else {
            return 0;
        }
    }

    void broadcastText(String text) {
        if (context != null) {
            Intent i = new Intent();
            i.setAction("updateDebugView");
            i.putExtra("text", text);
            context.sendBroadcast(i);
        }
    }

    String getMovementEventAsString(int action) {
        String s = "";
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                s = "MotionEvent.ACTION_DOWN";
                break;
            case MotionEvent.ACTION_UP:
                s = "MotionEvent.ACTION_UP";
                break;
            case MotionEvent.ACTION_MOVE:
                s = "MotionEvent.ACTION_MOVE";
                break;
            case MotionEvent.ACTION_CANCEL:
                s = "MotionEvent.ACTION_CANCEL";
                break;
            case MotionEvent.ACTION_OUTSIDE:
                s = "MotionEvent.ACTION_OUTSIDE";
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                s = "MotionEvent.ACTION_POINTER_DOWN";
                break;
            case MotionEvent.ACTION_POINTER_UP:
                s = "MotionEvent.ACTION_POINTER_UP";
                break;
            case MotionEvent.ACTION_HOVER_MOVE:
                s = "MotionEvent.ACTION_HOVER_MOVE";
                break;
            case MotionEvent.ACTION_SCROLL:
                s = "MotionEvent.ACTION_SCROLL";
                break;
            case MotionEvent.ACTION_HOVER_ENTER:
                s = "MotionEvent.ACTION_HOVER_ENTER";
                break;
            case MotionEvent.ACTION_HOVER_EXIT:
                s = "MotionEvent.ACTION_HOVER_EXIT";
                break;
            case MotionEvent.ACTION_BUTTON_PRESS:
                s = "MotionEvent.ACTION_BUTTON_PRESS";
                break;
            case MotionEvent.ACTION_BUTTON_RELEASE:
                s = "MotionEvent.ACTION_BUTTON_RELEASE";
                break;
        }
        return s;
    }

    public void onTouchEvent(MotionEvent event) {
        final int action = event.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() == 1 && bSingleFingerGesture) {
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
        //broadcastText(getMovementEventAsString(action & MotionEvent.ACTION_MASK)+"\nbSingleFingerGesture: "+ (bSingleFingerGesture?"TRUE":"FALSE") + "\nPointer count: " + event.getPointerCount());

    }
}
