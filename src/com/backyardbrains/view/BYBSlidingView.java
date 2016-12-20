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

public class BYBSlidingView implements Animation.AnimationListener{
    private View mView;
    
    private boolean bAnimating;
    private Animation animShow;
    private Animation animHide;
    private String name;
    private final static String TAG = "BYBSlidingView";
    public BYBSlidingView(View view, Context context, String name, int animShowR, int animHideR){
        setup(view,context, name, animShowR, animHideR);
    }
    public BYBSlidingView(View view, Context context, String name){
        setup(view,context, name, R.anim.slide_in_top,R.anim.slide_out_top);
    }
    private void setup(View view, Context context, String name, int animShowR, int animHideR){
        animShow = AnimationUtils.loadAnimation(context, animShowR);
        animShow.setAnimationListener(this);
        animHide = AnimationUtils.loadAnimation(context, animHideR);
        animHide.setAnimationListener(this);

        this.name = name;
        mView = view;

//        mView.bringToFront();

        bAnimating = false;
    }
    public boolean show(boolean bShow) {
       // Log.w(TAG + name, "show: " + (bShow?"true":"false")+ "  isShowing: "+ (isShowing()?"true":"false"));
        if(mView == null)return false;
        if((isShowing() != bShow ) && !bAnimating) {
            mView.startAnimation(bShow?animShow:animHide);
            return true;
        }
        return false;

    }
    public boolean toggle(){
        return show(!isShowing());
    }
    public boolean isShowing(){
        return mView != null && mView.getVisibility() == View.VISIBLE;
    }
    //////////////////////////////////////////////////////////////////////////////
    //                      Animation Callbacks
    //////////////////////////////////////////////////////////////////////////////
    public void 	onAnimationStart(Animation animation){
        if(mView == null)return;
        bAnimating = true;
//        mView.bringToFront();
        if(animation == animHide){
//            mView.setEnabled(false);
//            mView.setClickable(false);
        }else if (animation == animShow){
            mView.setVisibility(View.VISIBLE);
        }
    }
    public void onAnimationEnd(Animation animation){
        if(mView == null)return;
        bAnimating = false;
        if(animation == animShow){
//            mView.setEnabled(true);
//            mView.setClickable(true);
        }else if(animation == animHide){
            mView.setVisibility(View.GONE);
        }
//        mView.bringToFront();
    }
    public void 	onAnimationRepeat(Animation animation){}

}
