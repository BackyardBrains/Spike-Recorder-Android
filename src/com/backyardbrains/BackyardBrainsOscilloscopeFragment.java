package com.backyardbrains;

import com.backyardbrains.audio.AudioService;
import com.backyardbrains.audio.TriggerAverager;
import com.backyardbrains.drawing.BYBBaseRenderer;
import com.backyardbrains.drawing.InteractiveGLSurfaceView;
import com.backyardbrains.drawing.ThresholdRenderer;
import com.backyardbrains.drawing.WaveformRenderer;
import com.backyardbrains.view.BYBThresholdHandle;
import com.backyardbrains.view.ScaleListener;
import com.backyardbrains.view.SingleFingerGestureDetector;
import com.backyardbrains.view.TwoDimensionScaleGestureDetector;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SeekBar;

public class BackyardBrainsOscilloscopeFragment extends BackyardBrainsPlayLiveScopeFragment{
    // ----------------------------------------------------------------------------------------
    public BackyardBrainsOscilloscopeFragment(){
        super();
        TAG				= BackyardBrainsMain.BYB_OSCILLOSCOPE_FRAGMENT; //"BackyardBrainsOscilloscopeFragment";
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- FRAGMENT LIFECYCLE
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onStart() {
        super.onStart();
    }
    @Override
    public void onPause() {
        super.onPause();
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- UI
    ////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // -----------------------------------------  BROADCASTING LISTENERS
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- BROADCAST RECEIVERS CLASS

}
