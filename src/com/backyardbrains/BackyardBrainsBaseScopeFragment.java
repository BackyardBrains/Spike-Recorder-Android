package com.backyardbrains;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.backyardbrains.audio.AudioService;
import com.backyardbrains.drawing.BYBBaseRenderer;
import com.backyardbrains.drawing.InteractiveGLSurfaceView;
import com.backyardbrains.view.BYBZoomButton;
import java.util.Locale;

import static com.backyardbrains.utls.LogUtils.LOGD;
import static com.backyardbrains.utls.LogUtils.LOGE;
import static com.backyardbrains.utls.LogUtils.LOGW;
import static com.backyardbrains.utls.LogUtils.makeLogTag;

public abstract class BackyardBrainsBaseScopeFragment extends Fragment {

    private String TAG = makeLogTag(BackyardBrainsBaseScopeFragment.class);

    protected Context context;

    protected InteractiveGLSurfaceView mAndroidSurface = null;
    protected FrameLayout mainscreenGLLayout = null;
    protected SharedPreferences settings = null;

    protected TextView msView;
    protected TextView mVView;

    protected ImageView msLine;

    private BYBBaseRenderer renderer;

    protected BYBZoomButton zoomInButtonH, zoomOutButtonH, zoomInButtonV, zoomOutButtonV;
    protected View zoomButtonsHolder = null;

    protected float[] bufferWithXs = BYBBaseRenderer.initTempBuffer();

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- FRAGMENT LIFECYCLE
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getContext();
        View rootView = inflater.inflate(getLayoutID(), container, false);
        getSettings();
        mainscreenGLLayout = (FrameLayout) rootView.findViewById(R.id.glContainer);
        setupLabels(rootView);
        ViewTreeObserver vto = mainscreenGLLayout.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() {
                mainscreenGLLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int width = mainscreenGLLayout.getMeasuredWidth();
                setupMsLineView(width);
            }
        });

        readSettings();
        reassignSurfaceView();
        zoomButtonsHolder = rootView.findViewById(R.id.zoomButtonsHolderLayout);
        setupZoomButtons(rootView);
        showZoomUI(!((BackyardBrainsMain) getActivity()).isTouchSupported());

        return rootView;
    }

    @Override public void onStart() {
        super.onStart();
        if (mAndroidSurface != null) mAndroidSurface.onResume();
    }

    @Override public void onResume() {
        super.onResume();
        registerReceivers(true);
        readSettings();
    }

    @Override public void onPause() {
        super.onPause();
        registerReceivers(false);
        saveSettings();
    }

    @Override public void onStop() {
        super.onStop();
        saveSettings();
        if (mAndroidSurface != null) mAndroidSurface.onPause();
        mAndroidSurface = null;
    }

    @Override public void onDestroy() {
        destroyRenderer();
        //        registerReceivers(false);
        //        if(updateUIListener != null){
        //            registerUpdateUIReceiver(false);
        //        }
        super.onDestroy();
    }

    public void destroyRenderer() {
        if (renderer != null) {
            renderer.close();
            renderer = null;
        }
    }

    protected abstract BYBBaseRenderer createRenderer(@NonNull Context context, @NonNull float[] preparedBuffer);

    protected BYBBaseRenderer getRenderer() {
        return renderer;
    }

    protected abstract @LayoutRes int getLayoutID();

    protected boolean shouldUseAverager() {
        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- GL
    ////////////////////////////////////////////////////////////////////////////////////////////////
    protected void reassignSurfaceView() {
        LOGD(TAG, "reassignSurfaceView");
        if (getContext() != null) {
            AudioService as = ((BackyardBrainsApplication) context).getmAudioService();
            mAndroidSurface = null;

            if (mainscreenGLLayout != null) {
                mainscreenGLLayout.removeAllViews();
                if (as != null) {
                    as.setUseAverager(shouldUseAverager());
                } else {
                    LOGW(TAG, "AudioService is null");
                }
                if (renderer != null) {
                    saveSettings();
                    renderer = null;
                }
                long start = System.currentTimeMillis();
                LOGD(TAG, "START");
                try {
                    renderer = createRenderer(context, bufferWithXs);//(BYBBaseRenderer) rendererClass.newInstance();
                    LOGD(TAG, "AFTER createRenderer():" + (System.currentTimeMillis() - start));
                } catch (Exception ex) {
                    LOGE(TAG, "Renderer creation failed - " + ex.getMessage());
                }
                if (mAndroidSurface != null) mAndroidSurface = null;
                mAndroidSurface = new InteractiveGLSurfaceView(context, renderer);
                LOGD(TAG, "AFTER new InteractiveGLSurfaceView():" + (System.currentTimeMillis() - start));
                mainscreenGLLayout.addView(mAndroidSurface);
            }
            readSettings();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- UI
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
        LOGD(TAG, String.format(Locale.getDefault(), "setupMsLineView  %d: ", width));
        if (getActivity() != null && msLine != null) {
            DisplayMetrics metrics = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
            int w = (int) Math.floor(width * metrics.density);// /2;
            msLine.getLayoutParams().width = metrics.widthPixels / 2;
            msLine.requestLayout();
        }
    }

    // ----------------------------------------------------------------------------------------
    protected void setupZoomButtons(View rootView) {
        if (getContext() != null) {
            zoomInButtonH = new BYBZoomButton(getContext(), (ImageButton) rootView.findViewById(R.id.zoomInButtonH),
                R.drawable.plus_button_active, R.drawable.plus_button, InteractiveGLSurfaceView.MODE_ZOOM_IN_H);
            zoomOutButtonH = new BYBZoomButton(getContext(), (ImageButton) rootView.findViewById(R.id.zoomOutButtonH),
                R.drawable.minus_button_active, R.drawable.minus_button, InteractiveGLSurfaceView.MODE_ZOOM_OUT_H);
            zoomInButtonV = new BYBZoomButton(getContext(), (ImageButton) rootView.findViewById(R.id.zoomInButtonV),
                R.drawable.plus_button_active, R.drawable.plus_button, InteractiveGLSurfaceView.MODE_ZOOM_IN_V);
            zoomOutButtonV = new BYBZoomButton(getContext(), (ImageButton) rootView.findViewById(R.id.zoomOutButtonV),
                R.drawable.minus_button_active, R.drawable.minus_button, InteractiveGLSurfaceView.MODE_ZOOM_OUT_V);
        }
    }

    public void showZoomUI(boolean bShow) {

        zoomButtonsHolder.setVisibility(bShow ? View.VISIBLE : View.GONE);

        zoomInButtonH.setVisibility(bShow);
        zoomOutButtonH.setVisibility(bShow);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- UTILS
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public AudioService getAudioService() {
        if (getContext() != null) {
            BackyardBrainsApplication app = ((BackyardBrainsApplication) getContext());
            if (app != null) {
                return app.mAudioService;
            }
        }
        return null;
    }

    public boolean getIsPlaybackMode() {
        if (getAudioService() != null) {
            return getAudioService().isPlaybackMode();
        }
        return false;
    }

    public boolean getIsPlaying() {
        if (getAudioService() != null) {
            return getAudioService().isAudioPlayerPlaying();
        }
        return false;
    }

    @Override public Context getContext() {
        if (context == null) {
            FragmentActivity act = getActivity();
            if (act == null) {
                return null;
            }
            context = act.getApplicationContext();
        }
        return context;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- SETTINGS
    ////////////////////////////////////////////////////////////////////////////////////////////////
    protected void getSettings() {
        if (settings == null) {
            settings = getActivity().getPreferences(BackyardBrainsMain.MODE_PRIVATE);
        }
    }

    // ----------------------------------------------------------------------------------------
    protected void readSettings() {
        getSettings();
        if (settings != null) {
            if (renderer != null) {
                renderer.readSettings(settings, TAG);
            }
        }
    }

    // ----------------------------------------------------------------------------------------
    protected void saveSettings() {
        getSettings();
        if (settings != null) {
            renderer.saveSettings(settings, TAG);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // -----------------------------------------  BROADCASTING LISTENERS
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- BROADCAST RECEIVERS CLASS
    private class UpdateMillisecondsReciever extends BroadcastReceiver {
        @Override public void onReceive(android.content.Context context, android.content.Intent intent) {
            msView.setText(intent.getStringExtra("millisecondsDisplayedString"));
        }

        ;
    }

    private class UpdateMillivoltReciever extends BroadcastReceiver {
        @Override public void onReceive(android.content.Context context, android.content.Intent intent) {
            mVView.setText(intent.getStringExtra("millivoltsDisplayedString"));
        }

        ;
    }

    private class SetMillivoltViewSizeReceiver extends BroadcastReceiver {
        @Override public void onReceive(android.content.Context context, android.content.Intent intent) {
            mVView.setHeight(intent.getIntExtra("millivoltsViewNewSize", mVView.getHeight()));
        }

        ;
    }

    private class UpdateDebugTextViewListener extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            //            if(intent.hasExtra("text")) {
            //                debugText.setText(intent.getStringExtra("text"));
            //            }
        }
    }

    private class AudioServiceBindListener extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("isBind")) {
                if (!intent.getBooleanExtra("isBind", true)) {
                    destroyRenderer();
                }
            }
        }
    }

    private class ShowZoomUIListener extends BroadcastReceiver {
        @Override public void onReceive(android.content.Context context, android.content.Intent intent) {
            if (intent.hasExtra("showUI")) {
                boolean bShow = intent.getBooleanExtra("showUI", false);
                showZoomUI(bShow);
            }
        }
    }

    // ----------------------------------------- RECEIVERS INSTANCES
    private UpdateMillisecondsReciever upmillirec;
    private SetMillivoltViewSizeReceiver milliVoltSize;
    private UpdateMillivoltReciever upmillivolt;
    private UpdateDebugTextViewListener updateDebugTextViewListener;
    private AudioServiceBindListener audioServiceBindListener;
    private ShowZoomUIListener showZoomUIListener;

    // ----------------------------------------- REGISTER RECEIVERS
    public void registerReceivers(boolean bRegister) {
        registerReceiverUpdateMilliseconds(bRegister);
        registerReceiverUpdateMillivolts(bRegister);
        registerReceiverMillivoltsViewSize(bRegister);
        registerReceiverUpdateDebugView(bRegister);
        registerReceiverAudioServiceBind(bRegister);
        registerReceiverShowZoomUI(bRegister);
    }

    private void registerReceiverUpdateMilliseconds(boolean reg) {
        if (getContext() != null) {
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
        if (getContext() != null) {
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
        if (getContext() != null) {
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
        if (getContext() != null) {
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
        if (getContext() != null) {
            if (reg) {
                IntentFilter intentAudioServiceBindFilter = new IntentFilter("BYBAudioServiceBind");
                audioServiceBindListener = new AudioServiceBindListener();
                context.registerReceiver(audioServiceBindListener, intentAudioServiceBindFilter);
            } else {
                context.unregisterReceiver(audioServiceBindListener);
            }
        }
    }

    private void registerReceiverShowZoomUI(boolean reg) {
        if (getContext() != null) {
            if (reg) {
                IntentFilter intentFilter = new IntentFilter("BYBShowZoomUI");
                showZoomUIListener = new ShowZoomUIListener();
                context.registerReceiver(showZoomUIListener, intentFilter);
            } else {
                context.unregisterReceiver(showZoomUIListener);
            }
        }
    }
}
