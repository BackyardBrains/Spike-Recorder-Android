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

public class BYBToggleButton {

    public ImageButton button;
    protected boolean bActive;
    protected int activeResId;
    protected int inactiveResId;
    String broadcastAction;
    int broadcastExtra;
    protected Context context = null;
    protected BYBExclusiveToggleGroup group = null;
    public BYBToggleButton(Context context, ImageButton button, int activeResId, int inactiveResId, String broadcastAction, int broadcastExtra){
        this.button = button;
        this.broadcastAction = broadcastAction;
        this.broadcastExtra = broadcastExtra;
        bActive = false;
        this.context = context;
        this.activeResId = activeResId;
        this.inactiveResId = inactiveResId;
        this.button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if ((event.getAction() & MotionEvent.ACTION_MASK ) == MotionEvent.ACTION_UP) {
                    setActive(true);
                }
                return true;
            }
        });
    }
    public void setActive(boolean a){
        if(a != bActive) {
            bActive = a;
            button.setImageResource(bActive ? activeResId : inactiveResId);
            if (group != null) {
                group.updateGroup(this);
            }
        }
            if(bActive && context != null){
                Intent i = new Intent();
                i.setAction(broadcastAction);
                i.putExtra("nonTouchMode",broadcastExtra);
                context.sendBroadcast(i);
            }

    }
    public boolean isActive(){
        return bActive;
    }
    public void toggle(){
        setActive(!bActive);
    }
    public void setGroup(BYBExclusiveToggleGroup g){
        group = g;
    }
}
