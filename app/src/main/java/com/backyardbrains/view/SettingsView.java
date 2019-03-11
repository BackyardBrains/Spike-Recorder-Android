package com.backyardbrains.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.backyardbrains.R;
import com.backyardbrains.filters.Filter;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class SettingsView extends FrameLayout {

    @BindView(R.id.cb_mute_speakers) CheckBox cbMuteSpeakers;
    @BindView(R.id.v_filter_settings) FilterSettingsView vFilterSettings;
    @BindView(R.id.ll_channels_container) LinearLayout llChannelsContainer;

    /**
     * Interface definition for a callback to be invoked when one of the settings changes.
     */
    public interface OnSettingChangeListener {

        /**
         * Listener that is invoked when option of speakers mute is changed.
         *
         * @param mute Whether speakers should be muted or not.
         */
        void onSpeakersMuteChange(boolean mute);

        /**
         * Listener that is invoked when filter is changed.
         *
         * @param filter Set filter.
         */
        void onFilterChange(@NonNull Filter filter);
    }

    private OnSettingChangeListener listener;

    private final FilterSettingsView.OnFilterSetListener onFilterSetListener =
        new FilterSettingsView.OnFilterSetListener() {
            @Override public void onFilterSet(@NonNull Filter filter) {
                triggerOnFilterChange(filter);
            }

            @Override public void onNotchFilterSet(@Nullable Filter filter) {

            }
        };

    public SettingsView(Context context) {
        this(context, null);
    }

    public SettingsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SettingsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SettingsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        init();
    }

    /**
     * Registers a callback to be invoked when one of the settings has been changed.
     *
     * @param listener The callback that will be run. This value may be {@code null}.
     */
    public void setOnSettingChangeListener(@Nullable OnSettingChangeListener listener) {
        this.listener = listener;
    }

    /**
     * Sets up filter settings.
     */
    public void setupFilters(@NonNull Filter filter, double maxCutOffFreq) {
        vFilterSettings.setFilter(filter, maxCutOffFreq);
    }

    // Triggers OnSettingChangeListener.onFilterChange() method
    void triggerOnFilterChange(@Nullable Filter filter) {
        if (listener != null) listener.onFilterChange(filter);
    }

    // Inflates view layout
    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_settings, this);
        ButterKnife.bind(this);

        setupUI();
    }

    // Initial UI setup
    private void setupUI() {
        vFilterSettings.setOnFilterChangeListener(onFilterSetListener);
    }
}
