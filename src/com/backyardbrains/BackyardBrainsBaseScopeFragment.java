package com.backyardbrains;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
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
import com.backyardbrains.events.AudioServiceConnectionEvent;
import com.backyardbrains.view.BYBZoomButton;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static com.backyardbrains.utls.LogUtils.LOGD;
import static com.backyardbrains.utls.LogUtils.LOGE;
import static com.backyardbrains.utls.LogUtils.LOGW;
import static com.backyardbrains.utls.LogUtils.makeLogTag;

public abstract class BackyardBrainsBaseScopeFragment extends BaseFragment {

    private String TAG = makeLogTag(BackyardBrainsBaseScopeFragment.class);

    @BindView(R.id.fl_container) FrameLayout flGL;
    @BindView(R.id.tv_signal) TextView tvSignal;
    @BindView(R.id.tv_time) TextView tvTime;
    @BindView(R.id.v_time_scale) View vTimeScale;
    @BindView(R.id.ibtn_zoom_in_h) ImageButton ibtnZoomInHorizontally;
    @BindView(R.id.ibtn_zoom_out_h) ImageButton ibtnZoomOutHorizontally;
    @BindView(R.id.ibtn_zoom_in_v) ImageButton ibtnZoomInVertically;
    @BindView(R.id.ibtn_zoom_out_v) ImageButton ibtnZoomOutVertically;

    private Unbinder unbinder;
    private BYBBaseRenderer renderer;
    protected SharedPreferences settings = null;

    protected InteractiveGLSurfaceView glSurface = null;
    protected BYBZoomButton zoomInButtonH, zoomOutButtonH, zoomInButtonV, zoomOutButtonV;

    protected float[] bufferWithXs = BYBBaseRenderer.initTempBuffer();

    //////////////////////////////////////////////////////////////////////////////
    //                       Lifecycle overrides
    //////////////////////////////////////////////////////////////////////////////

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LOGD(TAG, "onCreateView()");

        final View rootView = inflater.inflate(getLayoutID(), container, false);
        unbinder = ButterKnife.bind(this, rootView);

        setupUI();

        readSettings();

        return rootView;
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        LOGD(TAG, "onDestroyView()");
        unbinder.unbind();
    }

    @Override public void onStart() {
        super.onStart();
        LOGD(TAG, "onStart()");
        if (glSurface != null) glSurface.onResume();
    }

    @Override public void onResume() {
        super.onResume();
        LOGD(TAG, "onResume()");
        registerReceivers(true);
        readSettings();
    }

    @Override public void onPause() {
        super.onPause();
        LOGD(TAG, "onPause()");
        registerReceivers(false);
        saveSettings();
    }

    @Override public void onStop() {
        super.onStop();
        LOGD(TAG, "onStop()");
        saveSettings();
        if (glSurface != null) glSurface.onPause();
    }

    @Override public void onDestroy() {
        LOGD(TAG, "onDestroy()");
        destroyRenderer();
        super.onDestroy();
    }

    public void destroyRenderer() {
        if (renderer != null) {
            renderer.close();
            renderer = null;
        }
    }

    protected abstract BYBBaseRenderer createRenderer(@NonNull BaseFragment fragment, @NonNull float[] preparedBuffer);

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

    /**
     * Initializes the surface view for drawing
     */
    protected void reassignSurfaceView() {
        LOGD(TAG, "reassignSurfaceView");
        if (getContext() != null) {
            glSurface = null;

            if (flGL != null) {
                flGL.removeAllViews();
                setUseAverager();
                if (renderer != null) {
                    saveSettings();
                    renderer = null;
                }
                try {
                    renderer = createRenderer(this, bufferWithXs);
                } catch (Exception ex) {
                    LOGE(TAG, "Renderer creation failed - " + ex.getMessage());
                }
                if (glSurface != null) glSurface = null;
                glSurface = new InteractiveGLSurfaceView(getContext());
                glSurface.setRenderer(renderer);
                flGL.addView(glSurface);
            }
            readSettings();
        }
    }

    // Sets whether audio service should use averager or not
    private void setUseAverager() {
        final AudioService provider = getAudioService();
        if (provider != null) {
            provider.setUseAverager(shouldUseAverager());
        } else {
            LOGW(TAG, "AudioService is null");
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

    //////////////////////////////////////////////////////////////////////////////
    //                                  Utils
    //////////////////////////////////////////////////////////////////////////////

    protected boolean getIsPlaybackMode() {
        return getAudioService() != null && getAudioService().isPlaybackMode();
    }

    protected boolean getIsPlaying() {
        return getAudioService() != null && getAudioService().isAudioPlaying();
    }

    //////////////////////////////////////////////////////////////////////////////
    //                                 Settings
    //////////////////////////////////////////////////////////////////////////////

    protected void getSettings() {
        if (settings == null) settings = getActivity().getPreferences(BackyardBrainsMain.MODE_PRIVATE);
    }

    protected void readSettings() {
        getSettings();
        if (settings != null && renderer != null) renderer.readSettings(settings, TAG);
    }

    protected void saveSettings() {
        getSettings();
        if (settings != null) renderer.saveSettings(settings, TAG);
    }

    //////////////////////////////////////////////////////////////////////////////
    //                            Event Bus
    //////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("unused") @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioServiceConnectionEvent(AudioServiceConnectionEvent event) {
        if (event.isConnected()) setUseAverager();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // -----------------------------------------  BROADCASTING LISTENERS
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ----------------------------------------- BROADCAST RECEIVERS CLASS
    private class UpdateMillisecondsReceiver extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            //LOGD(TAG, "BYBUpdateMillisecondsReciever broadcast received!");
            tvTime.setText(intent.getStringExtra("millisecondsDisplayedString"));
        }
    }

    private class UpdateMillivoltReceiver extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            //LOGD(TAG, "BYBUpdateMillivoltReciever broadcast received!");
            tvSignal.setText(intent.getStringExtra("millivoltsDisplayedString"));
        }
    }

    private class SetMillivoltViewSizeReceiver extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG, "BYBMillivoltsViewSize broadcast received");
            tvSignal.setHeight(intent.getIntExtra("millivoltsViewNewSize", tvSignal.getHeight()));
        }
    }

    private class UpdateDebugTextViewListener extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG, "updateDebugView broadcast received!");
            //if(intent.hasExtra("text")) {
            //    debugText.setText(intent.getStringExtra("text"));
            //}
        }
    }

    private class AudioServiceBindListener extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG, "BYBAudioServiceBind broadcast received!");
            if (intent.hasExtra("isBind")) {
                if (!intent.getBooleanExtra("isBind", true)) {
                    destroyRenderer();
                }
            }
        }
    }

    private class ShowZoomUIListener extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG, "BYBShowZoomUI broadcast received!");
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
        LOGD(TAG, "registerReceivers()");
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
                getContext().registerReceiver(upmillirec, intentFilter);
            } else {
                getContext().unregisterReceiver(upmillirec);
            }
        }
    }

    private void registerReceiverUpdateMillivolts(boolean reg) {
        if (getContext() != null) {
            if (reg) {
                IntentFilter intentFilterVolts = new IntentFilter("BYBUpdateMillivoltReciever");
                upmillivolt = new UpdateMillivoltReceiver();
                getContext().registerReceiver(upmillivolt, intentFilterVolts);
            } else {
                getContext().unregisterReceiver(upmillivolt);
            }
        }
    }

    private void registerReceiverMillivoltsViewSize(boolean reg) {
        if (getContext() != null) {
            if (reg) {
                IntentFilter intentFilterVoltSize = new IntentFilter("BYBMillivoltsViewSize");
                milliVoltSize = new SetMillivoltViewSizeReceiver();
                getContext().registerReceiver(milliVoltSize, intentFilterVoltSize);
            } else {
                getContext().unregisterReceiver(milliVoltSize);
            }
        }
    }

    private void registerReceiverUpdateDebugView(boolean reg) {
        if (getContext() != null) {
            if (reg) {
                IntentFilter intentUpdateDebugTextFilter = new IntentFilter("updateDebugView");
                updateDebugTextViewListener = new UpdateDebugTextViewListener();
                getContext().registerReceiver(updateDebugTextViewListener, intentUpdateDebugTextFilter);
            } else {
                getContext().unregisterReceiver(updateDebugTextViewListener);
            }
        }
    }

    private void registerReceiverAudioServiceBind(boolean reg) {
        if (getContext() != null) {
            if (reg) {
                IntentFilter intentAudioServiceBindFilter = new IntentFilter("BYBAudioServiceBind");
                audioServiceBindListener = new AudioServiceBindListener();
                getContext().registerReceiver(audioServiceBindListener, intentAudioServiceBindFilter);
            } else {
                getContext().unregisterReceiver(audioServiceBindListener);
            }
        }
    }

    private void registerReceiverShowZoomUI(boolean reg) {
        if (getContext() != null) {
            if (reg) {
                IntentFilter intentFilter = new IntentFilter("BYBShowZoomUI");
                showZoomUIListener = new ShowZoomUIListener();
                getContext().registerReceiver(showZoomUIListener, intentFilter);
            } else {
                getContext().unregisterReceiver(showZoomUIListener);
            }
        }
    }
}
