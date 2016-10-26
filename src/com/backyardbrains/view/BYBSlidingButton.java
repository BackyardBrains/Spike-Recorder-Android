package com.backyardbrains.view;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;

import com.backyardbrains.R;

/**
 * Created by roy on 25-10-16.
 */

public class BYBSlidingButton  implements Animation.AnimationListener{
    private View button;
    
    private boolean bAnimating;
    private Animation animShow;
    private Animation animHide;


    public BYBSlidingButton(View b, Context context){
        animShow = AnimationUtils.loadAnimation(context, R.anim.slide_in_top);
        animShow.setAnimationListener(this);
        animHide = AnimationUtils.loadAnimation(context, R.anim.slide_out_top);
        animHide.setAnimationListener(this);

        button = b;

        button.bringToFront();

        bAnimating = false;

    }
    public boolean show(boolean bShow) {
        if(button == null)return false;
        if((isShowing() != bShow ) && !bAnimating) {
            button.startAnimation(bShow?animShow:animHide);
            return true;
        }
        return false;

    }
    public boolean toggle(){
        return show(!isShowing());
    }
    public boolean isShowing(){
        return button != null && button.getVisibility() == View.VISIBLE;
    }
    //////////////////////////////////////////////////////////////////////////////
    //                      Animation Callbacks
    //////////////////////////////////////////////////////////////////////////////
    public void 	onAnimationStart(Animation animation){
        if(button == null)return;
        bAnimating = true;
        button.bringToFront();
        if(animation == animHide){
            button.setEnabled(false);
            button.setClickable(false);
        }else if (animation == animShow){
            button.setVisibility(View.VISIBLE);
        }
    }
    public void onAnimationEnd(Animation animation){
        if(button == null)return;
        bAnimating = false;
        if(animation == animShow){
            button.setEnabled(true);
            button.setClickable(true);
        }else if(animation == animHide){
            button.setVisibility(View.GONE);
        }
        button.bringToFront();
    }
    public void 	onAnimationRepeat(Animation animation){}

}
