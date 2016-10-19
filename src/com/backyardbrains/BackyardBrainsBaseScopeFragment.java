package com.backyardbrains;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.backyardbrains.audio.AudioService;
import com.backyardbrains.drawing.BYBBaseRenderer;
import com.backyardbrains.drawing.ContinuousGLSurfaceView;


import com.backyardbrains.drawing.ThresholdRenderer;
import com.backyardbrains.view.ScaleListener;
import com.backyardbrains.view.SingleFingerGestureDetector;
import com.backyardbrains.view.TwoDimensionScaleGestureDetector;

public class BackyardBrainsBaseScopeFragment extends Fragment{
    public String TAG = "BackyardBrainsBaseScopeFragment";
    protected TwoDimensionScaleGestureDetector  mScaleDetector;
    protected ScaleListener                     mScaleListener;
    protected SingleFingerGestureDetector       singleFingerGestureDetector = null;
    protected Context                           context;

    protected ContinuousGLSurfaceView           mAndroidSurface	    =           null;
    protected FrameLayout                       mainscreenGLLayout  =           null;
    protected SharedPreferences                 settings		    =           null;

    protected TextView                          msView;
    protected TextView							mVView;

    protected BYBBaseRenderer                   renderer            = null;
    public static final int						LIVE_MODE		    = 0;
    public static final int						PLAYBACK_MODE	    = 1;

    protected int                               layoutID;
    protected int								mode			    = 0;
    protected Class rendererClass  = null;

    public BackyardBrainsBaseScopeFragment(){
        mode = LIVE_MODE;
        layoutID = R.layout.base_scope_layout;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public Context getContext(){
        if(context == null){
            FragmentActivity act = getActivity();
            if(act == null) {
                return null;
            }
            context = act.getApplicationContext();
        }
        return context;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- FRAGMENT LIFECYCLE
    ////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getContext()!= null) {
            mScaleListener = new ScaleListener();
            mScaleDetector = new TwoDimensionScaleGestureDetector(context, mScaleListener);
            singleFingerGestureDetector = new SingleFingerGestureDetector();
        } else {
            //// //Log.d(TAG, "onCreate failed, context == null");
        }
    }
    // ----------------------------------------------------------------------------------------
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(layoutID, container, false);
        getSettings();
        mainscreenGLLayout = (FrameLayout) rootView.findViewById(R.id.glContainer);
        setupLabels(rootView);
        return rootView;
    }
    @Override
    public void onStart() {
        super.onStart();
        readSettings();
        setupMsLineView();
        reassignSurfaceView();
    }
    @Override
    public void onResume() {
        super.onResume();
        registerReceivers();
        if (mAndroidSurface != null) {
            mAndroidSurface.onResume();
        }
        readSettings();

    }
    @Override
    public void onPause() {
        super.onPause();
        if (mAndroidSurface != null) {
            mAndroidSurface.onPause();
        }
        unregisterReceivers();
        saveSettings();

    }
    @Override
    public void onStop() {
        super.onStop();
        saveSettings();
        mAndroidSurface = null;
    }
    @Override
    public void onDestroy() {
        destroyRenderer();
        super.onDestroy();
    }
    public void destroyRenderer() {
        if (renderer != null) {
            renderer.close();
            renderer = null;
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- GL
    ////////////////////////////////////////////////////////////////////////////////////////////////
    protected void reassignSurfaceView() {
        Log.d(TAG, "reassignSurfaceView");
        if( rendererClass == null){
            Log.w(TAG, "RendererClass is null. It need to be assigned in the subclass constructor!!");
            return;
        }
        if (getContext()!= null) {
            AudioService as = ((BackyardBrainsApplication) context).getmAudioService();
            mAndroidSurface = null;

            if (mainscreenGLLayout != null) {
                mainscreenGLLayout.removeAllViews();
                if (as != null) {
                    as.setUseAverager(rendererClass.equals(ThresholdRenderer.class));
                }else{
                    Log.w(TAG, "AudioService is null");
                }
                if (renderer != null) {
                    saveSettings();
                    renderer = null;
                }
                if (renderer == null) {
                    try {
                        renderer = (BYBBaseRenderer) rendererClass.newInstance();
//                        ((BYBBaseRenderer)
                        renderer.setup(context);
                    } catch (InstantiationException ex) {
                        Log.d(TAG, rendererClass.getName()+  ex.getMessage());
                    } catch (IllegalAccessException ex) {
                        Log.d(TAG, rendererClass.getName()+  ex.getMessage());
                    }catch (Exception ex){
                        Log.d(TAG, rendererClass.getName()+  ex.getMessage());
                    }

                }
                if (mAndroidSurface != null) {
                    mAndroidSurface = null;
                }
                mAndroidSurface = new ContinuousGLSurfaceView(context, renderer);
                mScaleListener.setRenderer(renderer);
                mainscreenGLLayout.addView(mAndroidSurface);
            }
            readSettings();
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- UI SETUPS
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void setupLabels(View v) {
        msView = (TextView) v.findViewById(R.id.millisecondsView);
        mVView = (TextView) v.findViewById(R.id.mVLabelView);
    }

    // ----------------------------------------------------------------------------------------
    public void setDisplayedMilliseconds(Float ms) {
        msView.setText(ms.toString());
    }

    // ----------------------------------------------------------------------------------------
    public void setupMsLineView() {
        // TODO: ms Line via openGL line--> renderer
// if (getView() != null) {
// ImageView msLineView = new ImageView(getActivity());
// Bitmap bmp = BitmapFactory.decodeResource(context.getResources(),
// R.drawable.msline);
// int width = getView().getWidth() / 3;
// int height = 2;
// Bitmap resizedbitmap = Bitmap.createScaledBitmap(bmp, width, height, false);
// msLineView.setImageBitmap(resizedbitmap);
// msLineView.setBackgroundColor(Color.BLACK);
// msLineView.setScaleType(ScaleType.CENTER);
//
// LayoutParams rl = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
// LayoutParams.WRAP_CONTENT);
// rl.setMargins(0, 0, 0, 20);
// rl.addRule(RelativeLayout.ABOVE, R.id.millisecondsView);
// rl.addRule(RelativeLayout.CENTER_HORIZONTAL);
//
// RelativeLayout parentLayout = (RelativeLayout)
// getView().findViewById(R.id.parentLayout);
// parentLayout.addView(msLineView, rl);
// }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- TOUCH
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public boolean onTouchEvent(MotionEvent event) {
        if(mode == PLAYBACK_MODE  && renderer != null && singleFingerGestureDetector != null){
            singleFingerGestureDetector.onTouchEvent(event);
            if(singleFingerGestureDetector.hasChanged()){
                renderer.addToGlOffset(singleFingerGestureDetector.getDX(), singleFingerGestureDetector.getDY());
            }
        }
        mScaleDetector.onTouchEvent(event);
        if (mAndroidSurface != null) {
            return mAndroidSurface.onTouchEvent(event);
        }
        return false;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- SETTINGS
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private void getSettings() {
        if (settings == null) {
            settings = getActivity().getPreferences(BackyardBrainsMain.MODE_PRIVATE);
        }
    }
    // ----------------------------------------------------------------------------------------
    protected void readSettings() {
        getSettings();
        if (settings != null) {
            if (renderer != null) {
                renderer.readSettings(settings,TAG);
            }
        }
    }
    // ----------------------------------------------------------------------------------------
    protected void saveSettings() {
        getSettings();
        if (settings != null) {
            renderer.saveSettings(settings,TAG);
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // -----------------------------------------  BROADCASTING LISTENERS
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- BROADCAST RECEIVERS CLASS
    private class UpdateMillisecondsReciever extends BroadcastReceiver {
        @Override
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            msView.setText(intent.getStringExtra("millisecondsDisplayedString"));
        };
    }
    private class UpdateMillivoltReciever extends BroadcastReceiver {
        @Override
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            mVView.setText(intent.getStringExtra("millivoltsDisplayedString"));
        };
    }
    private class SetMillivoltViewSizeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            mVView.setHeight(intent.getIntExtra("millivoltsViewNewSize", mVView.getHeight()));
        };
    }
    private class UpdateDebugTextViewListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
        }
    }
    private class AudioServiceBindListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("isBind")) {
                if (!intent.getBooleanExtra("isBind", true)) {
                    destroyRenderer();
                }
            }
        }
    }

    // ----------------------------------------- RECEIVERS INSTANCES
    private UpdateMillisecondsReciever upmillirec;
    private SetMillivoltViewSizeReceiver milliVoltSize;
    private UpdateMillivoltReciever upmillivolt;
    private UpdateDebugTextViewListener updateDebugTextViewListener;
    private AudioServiceBindListener audioServiceBindListener;

    // ----------------------------------------- REGISTER RECEIVERS
    public void registerReceivers() {
        registerReceivers(true);
    }
    public void registerReceivers(boolean bRegister) {

        registerReceiverUpdateMilliseconds(bRegister);
        registerReceiverUpdateMillivolts(bRegister);
        registerReceiverMillivoltsViewSize(bRegister);
        registerReceiverUpdateDebugView(bRegister);
        registerReceiverAudioServiceBind(bRegister);

    }
    public void unregisterReceivers() {
        registerReceivers(false);
    }
    private void registerReceiverUpdateMilliseconds(boolean reg) {
        if(getContext() != null) {
            if (reg) {
                IntentFilter intentFilter = new IntentFilter("BYBUpdateMillisecondsReciever");
                upmillirec = new UpdateMillisecondsReciever();
                context.registerReceiver(upmillirec, intentFilter);
            } else {
                context.unregisterReceiver(upmillirec);
            }
        }
    }
    private void registerReceiverUpdateMillivolts(boolean reg) {
        if(getContext() != null) {
            if (reg) {
                IntentFilter intentFilterVolts = new IntentFilter("BYBUpdateMillivoltReciever");
                upmillivolt = new UpdateMillivoltReciever();
                context.registerReceiver(upmillivolt, intentFilterVolts);
            } else {
                context.unregisterReceiver(upmillivolt);
            }
        }
    }
    private void registerReceiverMillivoltsViewSize(boolean reg) {
        if(getContext() != null) {
            if (reg) {
                IntentFilter intentFilterVoltSize = new IntentFilter("BYBMillivoltsViewSize");
                milliVoltSize = new SetMillivoltViewSizeReceiver();
                context.registerReceiver(milliVoltSize, intentFilterVoltSize);
            } else {
                context.unregisterReceiver(milliVoltSize);
            }
        }
    }
    private void registerReceiverUpdateDebugView(boolean reg) {
        if(getContext() != null) {
            if (reg) {
                IntentFilter intentUpdateDebugTextFilter = new IntentFilter("updateDebugView");
                updateDebugTextViewListener = new UpdateDebugTextViewListener();
                context.registerReceiver(updateDebugTextViewListener, intentUpdateDebugTextFilter);
            } else {
                context.unregisterReceiver(updateDebugTextViewListener);
            }
        }
    }
    private void registerReceiverAudioServiceBind(boolean reg) {
        if(getContext() != null) {
            if (reg) {
                IntentFilter intentAudioServiceBindFilter = new IntentFilter("BYBAudioServiceBind");
                audioServiceBindListener = new AudioServiceBindListener();
                context.registerReceiver(audioServiceBindListener, intentAudioServiceBindFilter);
            } else {
                context.unregisterReceiver(audioServiceBindListener);
            }
        }
    }
}
