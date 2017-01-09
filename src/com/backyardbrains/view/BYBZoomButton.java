package com.backyardbrains.view;

import android.content.Context;
import android.content.Intent;
import android.util.StringBuilderPrinter;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;

import com.backyardbrains.drawing.InteractiveGLSurfaceView;

/**
 * Created by roy on 08-11-16.
 */

public class BYBZoomButton {

    public ImageButton button;
    protected boolean bActive;
    protected int activeResId;
    protected int inactiveResId;
    //String broadcastAction;
    int broadcastExtra;
    public static final String broadcastAction = "zoomButtonMode";

    protected Context context = null;
    public BYBZoomButton(Context context, ImageButton button, int activeResId, int inactiveResId, int broadcastExtra){
        this.button = button;
      //  this.broadcastAction = broadcastAction;
        this.broadcastExtra = broadcastExtra;
        bActive = false;
        this.context = context;
        this.activeResId = activeResId;
        this.inactiveResId = inactiveResId;
        this.button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if ((event.getAction() & MotionEvent.ACTION_MASK ) == MotionEvent.ACTION_UP) {
                    setActive(false, true);
                    return true;
                }else if ((event.getAction() & MotionEvent.ACTION_MASK ) == MotionEvent.ACTION_DOWN) {
                    setActive(true, false);
                    return true;
                }
                return false;
            }
        });
    }
    public void setVisibility(boolean bShow){
        button.setVisibility(bShow?View.VISIBLE:View.GONE);
    }
    public void setActive(boolean a, boolean bBroadcast){
        if(a != bActive) {
            bActive = a;
            button.setImageResource(bActive ? activeResId : inactiveResId);
        }
        if(bBroadcast && context != null){
            Intent i = new Intent();
            i.setAction(broadcastAction);
            i.putExtra("zoomMode",broadcastExtra);
            context.sendBroadcast(i);
        }
    }
    public boolean isActive(){
        return bActive;
    }

}
