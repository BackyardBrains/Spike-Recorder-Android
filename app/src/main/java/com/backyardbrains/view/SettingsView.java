package com.backyardbrains.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.backyardbrains.R;
import com.backyardbrains.drawing.Colors;
import com.backyardbrains.filters.BandFilter;
import com.backyardbrains.filters.NotchFilter;
import java.util.Arrays;

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
        void onSpeakersMuteChanged(boolean mute);

        /**
         * Listener that is invoked when band filter is changed.
         *
         * @param filter Set filter.
         */
        void onBandFilterChanged(@Nullable BandFilter filter);

        /**
         * Listener that is invoked when notch filter is set/unset.
         *
         * @param filter Set filter.
         */
        void onNotchFilterChanged(@Nullable NotchFilter filter);

        /**
         * Listener that is invoked when channel at specified {@code channelIndex} changes color to specified {@code color}.
         *
         * @param channelIndex Index of the channel which color has changed.
         * @param color New channel color.
         */
        void onChannelColorChanged(int channelIndex, @Size(4) float[] color);

        /**
         * Listener that is invoked when channel at specified {@code channelIndex} is shown.
         *
         * @param channelIndex Index of the shown channel.
         * @param color New channel color.
         */
        void onChannelShown(int channelIndex, float[] color);

        /**
         * Listener that is invoked when channel at specified {@code channelIndex} is hidden.
         *
         * @param channelIndex Index of the hidden channel.
         */
        void onChannelHidden(int channelIndex);
    }

    private OnSettingChangeListener listener;

    private final CompoundButton.OnCheckedChangeListener onMuteSpeakersChangeListener =
        (buttonView, isChecked) -> triggerOnSpeakersMuteChange(isChecked);

    private final FilterSettingsView.OnFilterSetListener onFilterSetListener =
        new FilterSettingsView.OnFilterSetListener() {
            @Override public void onBandFilterSet(@Nullable BandFilter filter) {
                triggerOnFilterChange(filter);
            }

            @Override public void onNotchFilterSet(@Nullable NotchFilter filter) {
                triggerOnNotchFilterChange(filter);
            }
        };

    private final ChannelSettingsView.OnColorChangeListener onColorChangeListener = this::triggerOnColorChange;

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

    @SuppressWarnings("unused") @TargetApi(Build.VERSION_CODES.LOLLIPOP)
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
     * Sets up mute speakers setting.
     */
    public void setupMuteSpeakers(boolean mute) {
        cbMuteSpeakers.setOnCheckedChangeListener(null);
        cbMuteSpeakers.setChecked(mute);
        cbMuteSpeakers.setOnCheckedChangeListener(onMuteSpeakersChangeListener);
    }

    /**
     * Sets up filter settings.
     */
    public void setupFilters(@NonNull BandFilter bandFilter, double maxCutOffFreq, NotchFilter notchFilter) {
        vFilterSettings.setBandFilter(bandFilter, maxCutOffFreq);
        vFilterSettings.setNotchFilter(notchFilter);
    }

    /**
     * Sets up channel settings.
     */
    public void setupChannels(int channelCount, float[][] channelColors) {
        llChannelsContainer.removeAllViews();

        ChannelSettingsView channelSettingsView;
        for (int i = 0; i < channelCount; i++) {
            channelSettingsView = new ChannelSettingsView(getContext());
            channelSettingsView.setOnColorChangeListener(onColorChangeListener);
            channelSettingsView.setChannelIndex(i);
            channelSettingsView.setChannelColor(channelColors[i]);
            llChannelsContainer.addView(channelSettingsView);
        }
    }

    // Triggers OnSettingChangeListener.onSpeakersMuteChanged() method
    void triggerOnSpeakersMuteChange(boolean isChecked) {
        if (listener != null) listener.onSpeakersMuteChanged(isChecked);
    }

    // Triggers OnSettingChangeListener.onBandFilterChanged() method
    void triggerOnFilterChange(@Nullable BandFilter filter) {
        if (listener != null) listener.onBandFilterChanged(filter);
    }

    // Triggers OnSettingChangeListener.onNotchFilterChanged() method
    void triggerOnNotchFilterChange(@Nullable NotchFilter filter) {
        if (listener != null) listener.onNotchFilterChanged(filter);
    }

    // Triggers OnSettingChangeListener.onChannelColorChanged() or OnSettingChangeListener.onChannelHidden() methods
    void triggerOnColorChange(@Size(4) float[] prevColor, @Size(4) float[] newColor, int channelIndex) {
        if (Arrays.equals(Colors.BLACK, newColor)) {
            if (listener != null) listener.onChannelHidden(channelIndex);
        } else if (Arrays.equals(Colors.BLACK, prevColor)) {
            if (listener != null) listener.onChannelShown(channelIndex, newColor);
        } else {
            if (listener != null) listener.onChannelColorChanged(channelIndex, newColor);
        }
    }

    // Inflates view layout
    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_settings, this);
        ButterKnife.bind(this);

        setupUI();
    }

    // Initial UI setup
    private void setupUI() {
        cbMuteSpeakers.setOnCheckedChangeListener(onMuteSpeakersChangeListener);
        vFilterSettings.setOnFilterChangeListener(onFilterSetListener);
    }
}
