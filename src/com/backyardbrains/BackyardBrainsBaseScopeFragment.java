package com.backyardbrains;

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

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.backyardbrains.audio.AudioService;
import com.backyardbrains.drawing.BYBBaseRenderer;
import com.backyardbrains.drawing.InteractiveGLSurfaceView;


import com.backyardbrains.drawing.ThresholdRenderer;

public class BackyardBrainsBaseScopeFragment extends Fragment{
    public String TAG = "BackyardBrainsBaseScopeFragment";

    protected Context                           context;

    protected InteractiveGLSurfaceView          mAndroidSurface	    =           null;
    protected FrameLayout                       mainscreenGLLayout  =           null;
    protected SharedPreferences                 settings		    =           null;

    protected TextView                          msView;
    protected TextView							mVView;
//    protected TextView							debugText;
    protected ImageView                         msLine;

    protected BYBBaseRenderer                   renderer            = null;

    protected int                               layoutID;

    protected Class rendererClass  = null;

    public BackyardBrainsBaseScopeFragment(){
        super();
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
    }
    // ----------------------------------------------------------------------------------------
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(layoutID, container, false);
        getSettings();
        mainscreenGLLayout = (FrameLayout) rootView.findViewById(R.id.glContainer);
//        debugText = (TextView)rootView.findViewById(R.id.debugText);
//        debugText.bringToFront();
        ViewTreeObserver vto = mainscreenGLLayout.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mainscreenGLLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int width  = mainscreenGLLayout.getMeasuredWidth();
//                int height = mainscreenGLLayout.getMeasuredHeight();
                setupMsLineView(width);
            }
        });
        Log.w(TAG, String.format("glContainer width : %d",mainscreenGLLayout.getWidth() ));
        setupLabels(rootView);
        readSettings();
        reassignSurfaceView();
        
        return rootView;
    }
    @Override
    public void onStart() {
        super.onStart();
    }
    @Override
    public void onResume() {
        super.onResume();
        registerReceivers(true);
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
        registerReceivers(false);
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
                mAndroidSurface = new InteractiveGLSurfaceView(context, renderer);
                //mScaleListener.setRenderer(renderer);
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
        msLine = (ImageView) v.findViewById(R.id.millisecondsViewLine);
    }
    // ----------------------------------------------------------------------------------------
    public void setDisplayedMilliseconds(Float ms) {
        msView.setText(ms.toString());
    }
    // ----------------------------------------------------------------------------------------
    public void setupMsLineView(int width) {
            int w = width / 3;
            msLine.getLayoutParams().width = w;
            msLine.requestLayout();
            Log.d(TAG, String.format("msLine width : %d",w ));
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
    public void registerReceivers(boolean bRegister) {
        registerReceiverUpdateMilliseconds(bRegister);
        registerReceiverUpdateMillivolts(bRegister);
        registerReceiverMillivoltsViewSize(bRegister);
        registerReceiverUpdateDebugView(bRegister);
        registerReceiverAudioServiceBind(bRegister);

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
