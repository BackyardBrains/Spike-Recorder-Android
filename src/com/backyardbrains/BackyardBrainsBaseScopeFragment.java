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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.backyardbrains.audio.AudioService;
import com.backyardbrains.drawing.BYBBaseRenderer;
import com.backyardbrains.drawing.InteractiveGLSurfaceView;
import com.backyardbrains.view.BYBZoomButton;

import static com.backyardbrains.utls.LogUtils.LOGD;
import static com.backyardbrains.utls.LogUtils.LOGE;
import static com.backyardbrains.utls.LogUtils.LOGW;
import static com.backyardbrains.utls.LogUtils.makeLogTag;

public abstract class BackyardBrainsBaseScopeFragment extends Fragment {

    private String TAG = makeLogTag(BackyardBrainsBaseScopeFragment.class);

    @BindView(R.id.fl_container) FrameLayout flGL;
    @BindView(R.id.tv_signal) TextView tvSignal;
    @BindView(R.id.tv_time) TextView tvTime;
    @BindView(R.id.v_time_scale) View vTimeScale;
    @BindView(R.id.ibtn_zoom_in_h) ImageButton ibtnZoomInHorizontally;
    @BindView(R.id.ibtn_zoom_out_h) ImageButton ibtnZoomOutHorizontally;
    @BindView(R.id.ibtn_zoom_in_v) ImageButton ibtnZoomInVertically;
    @BindView(R.id.ibtn_zoom_out_v) ImageButton ibtnZoomOutVertically;

    protected Context context;
    private Unbinder unbinder;
    private BYBBaseRenderer renderer;
    protected SharedPreferences settings = null;

    protected InteractiveGLSurfaceView glSurface = null;
    protected BYBZoomButton zoomInButtonH, zoomOutButtonH, zoomInButtonV, zoomOutButtonV;

    protected float[] bufferWithXs = BYBBaseRenderer.initTempBuffer();

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- FRAGMENT LIFECYCLE
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getContext();

        final View rootView = inflater.inflate(getLayoutID(), container, false);
        unbinder = ButterKnife.bind(this, rootView);

        readSettings();

        setupUI();

        return rootView;
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override public void onStart() {
        super.onStart();
        if (glSurface != null) glSurface.onResume();
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
        if (glSurface != null) glSurface.onPause();
        glSurface = null;
    }

    @Override public void onDestroy() {
        destroyRenderer();
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
            glSurface = null;

            if (flGL != null) {
                flGL.removeAllViews();
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
                if (glSurface != null) glSurface = null;
                glSurface = new InteractiveGLSurfaceView(context, renderer);
                LOGD(TAG, "AFTER new InteractiveGLSurfaceView():" + (System.currentTimeMillis() - start));
                flGL.addView(glSurface);
            }
            readSettings();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- UI
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private void setupUI() {
        reassignSurfaceView();
        setupZoomButtons();
        showZoomUI(!((BackyardBrainsMain) getActivity()).isTouchSupported());
    }

    private void setupZoomButtons() {
        if (getContext() != null) {
            zoomInButtonH = new BYBZoomButton(getContext(), ibtnZoomInHorizontally, R.drawable.plus_button_active,
                R.drawable.plus_button, InteractiveGLSurfaceView.MODE_ZOOM_IN_H);
            zoomOutButtonH = new BYBZoomButton(getContext(), ibtnZoomOutHorizontally, R.drawable.minus_button_active,
                R.drawable.minus_button, InteractiveGLSurfaceView.MODE_ZOOM_OUT_H);
            zoomInButtonV = new BYBZoomButton(getContext(), ibtnZoomInVertically, R.drawable.plus_button_active,
                R.drawable.plus_button, InteractiveGLSurfaceView.MODE_ZOOM_IN_V);
            zoomOutButtonV = new BYBZoomButton(getContext(), ibtnZoomOutVertically, R.drawable.minus_button_active,
                R.drawable.minus_button, InteractiveGLSurfaceView.MODE_ZOOM_OUT_V);
        }
    }

    private void showZoomUI(boolean bShow) {
        zoomInButtonV.setVisibility(bShow);
        zoomOutButtonV.setVisibility(bShow);
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
        return getAudioService() != null && getAudioService().isPlaybackMode();
    }

    public boolean getIsPlaying() {
        return getAudioService() != null && getAudioService().isAudioPlayerPlaying();
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
    private class UpdateMillisecondsReceiver extends BroadcastReceiver {
        @Override public void onReceive(android.content.Context context, android.content.Intent intent) {
            tvTime.setText(intent.getStringExtra("millisecondsDisplayedString"));
        }
    }

    private class UpdateMillivoltReceiver extends BroadcastReceiver {
        @Override public void onReceive(android.content.Context context, android.content.Intent intent) {
            tvSignal.setText(intent.getStringExtra("millivoltsDisplayedString"));
        }
    }

    private class SetMillivoltViewSizeReceiver extends BroadcastReceiver {
        @Override public void onReceive(android.content.Context context, android.content.Intent intent) {
            tvSignal.setHeight(intent.getIntExtra("millivoltsViewNewSize", tvSignal.getHeight()));
        }
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
    private UpdateMillisecondsReceiver upmillirec;
    private SetMillivoltViewSizeReceiver milliVoltSize;
    private UpdateMillivoltReceiver upmillivolt;
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
                upmillirec = new UpdateMillisecondsReceiver();
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
                upmillivolt = new UpdateMillivoltReceiver();
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
