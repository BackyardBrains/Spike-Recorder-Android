package com.backyardbrains.dsp;

import android.support.annotation.NonNull;
import android.support.v4.util.ArraySet;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.SignalAveragingTriggerType;
import java.util.Arrays;
import java.util.Set;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public final class SignalConfiguration {

    // Lock used when reading/writing channel configuration
    //private static final Object lock = new Object();

    private static SignalConfiguration INSTANCE;

    private int sampleRate;
    private int channelCount;
    private boolean[] channelConfig;
    private int visibleChannelCount;
    private int selectedChannel;
    private boolean signalAveraging;
    private @SignalAveragingTriggerType int signalAveragingTriggerType;

    /**
     * Interface definition for a callback to be invoked when one of signal properties changes.
     */
    public interface OnSignalPropertyChangeListener {
        /**
         * Called when signal sample rate changes.
         *
         * @param sampleRate The new sample rate.
         */
        void onSampleRateChanged(int sampleRate);

        /**
         * Called when number of channels changes.
         *
         * @param channelCount The new number of channels.
         */
        void onChannelCountChanged(int channelCount);

        /**
         * Called when channel is added/removed.
         *
         * @param channelConfig Array of booleans indicating which channel is on and which is off.
         */
        void onChannelConfigChanged(boolean[] channelConfig);

        /**
         * Called when selected channel changes.
         *
         * @param channelIndex Index of the selected channel.
         */
        void onChannelSelectionChanged(int channelIndex);

        /**
         * Called when signal averaging is turned on or off.
         *
         * @param signalAveraging Whether signal averaging is turned on/off.
         */
        void onSignalAveragingChanged(boolean signalAveraging);

        /**
         * Called when signal averaging trigger type is changed.
         *
         * @param averagingTriggerType Trigger type that is set. One of {@link SignalAveragingTriggerType} values
         */
        void onSignalAveragingTriggerTypeChanged(@SignalAveragingTriggerType int averagingTriggerType);
    }

    private Set<OnSignalPropertyChangeListener> onSignalPropertyChangeListeners;

    // Private constructor through which we create singleton instance
    private SignalConfiguration() {
        sampleRate = AudioUtils.DEFAULT_SAMPLE_RATE;
        channelCount = AudioUtils.DEFAULT_CHANNEL_COUNT;
        channelConfig = Arrays.copyOf(AudioUtils.DEFAULT_CHANNEL_CONFIG, AudioUtils.DEFAULT_CHANNEL_CONFIG.length);
        visibleChannelCount = AudioUtils.DEFAULT_CHANNEL_COUNT;
        selectedChannel = 0;
        signalAveraging = false;
        signalAveragingTriggerType = SignalAveragingTriggerType.THRESHOLD;
    }

    /**
     * Returns singleton instance of {@link SignalConfiguration} with default configuration.
     */
    public static SignalConfiguration get() {
        if (INSTANCE == null) {
            synchronized (SignalConfiguration.class) {
                if (INSTANCE == null) INSTANCE = new SignalConfiguration();
            }
        }
        return INSTANCE;
    }

    /**
     * Adds a listener to the set of listeners to be invoked when one of signal properties changes.
     *
     * @param listener the listener to be added to the current set of listeners.
     */
    public void addOnSignalPropertyChangeListener(@NonNull OnSignalPropertyChangeListener listener) {
        if (onSignalPropertyChangeListeners == null) {
            onSignalPropertyChangeListeners = new ArraySet<>();
        }
        onSignalPropertyChangeListeners.add(listener);
    }

    /**
     * Removes a listener from the set listening to signal property change.
     *
     * @param listener the listener to be removed from the current set of listeners.
     */
    public void removeOnSignalPropertyChangeListener(@NonNull OnSignalPropertyChangeListener listener) {
        if (onSignalPropertyChangeListeners == null) return;
        onSignalPropertyChangeListeners.remove(listener);
        if (onSignalPropertyChangeListeners.size() == 0) onSignalPropertyChangeListeners = null;
    }

    /**
     * Returns sample rate of the processed signal.
     */
    public int getSampleRate() {
        return sampleRate;
    }

    /**
     * Sets sample rate of the processed signal.
     */
    void setSampleRate(int sampleRate) {
        if (sampleRate <= 0 || this.sampleRate == sampleRate) return;

        this.sampleRate = sampleRate;

        if (onSignalPropertyChangeListeners != null) {
            for (OnSignalPropertyChangeListener listener : onSignalPropertyChangeListeners) {
                listener.onSampleRateChanged(sampleRate);
            }
        }
    }

    /**
     * Returns channel count of the processed signal.
     */
    public int getChannelCount() {
        return channelCount;
    }

    /**
     * Sets channel count of the processed signal.
     */
    void setChannelCount(int channelCount) {
        if (channelCount < 1 || this.channelCount == channelCount) return;

        this.channelCount = channelCount;

        // reset channel config so data is consistent and make all channels visible by default
        channelConfig = new boolean[channelCount];
        Arrays.fill(channelConfig, true);
        visibleChannelCount = channelCount;

        // reset selected channel
        selectedChannel = 0;

        if (onSignalPropertyChangeListeners != null) {
            for (OnSignalPropertyChangeListener listener : onSignalPropertyChangeListeners) {
                listener.onChannelCountChanged(channelCount);
                listener.onChannelConfigChanged(channelConfig);
                listener.onChannelSelectionChanged(selectedChannel);
            }
        }
    }

    /**
     * Returns number of channels that are ON.
     */
    public int getVisibleChannelCount() {
        return visibleChannelCount;
    }

    /**
     * Returns {@code true} if channel at specified {@code channelIndex} is ON. If
     *
     * @throws IndexOutOfBoundsException Exception is thrown is specified {@code channelIndex} is out of bounds.
     */
    public boolean isChannelVisible(int channelIndex) throws IndexOutOfBoundsException {
        if (channelIndex < 0 || channelIndex >= channelCount) throw new IndexOutOfBoundsException();
        if (channelConfig == null) return false;

        return channelConfig[channelIndex];
    }

    /**
     * Sets array of booleans indicating which channel is ON and which is OFF.
     */
    void setChannelConfig(boolean[] channelConfig) {
        if (channelConfig.length != channelCount || Arrays.equals(this.channelConfig, channelConfig)) return;

        this.channelConfig = Arrays.copyOf(channelConfig, channelConfig.length);

        // reset visible channel count so data is consistent
        visibleChannelCount = 0;
        for (boolean visible : channelConfig) if (visible) visibleChannelCount++;

        if (onSignalPropertyChangeListeners != null) {
            for (OnSignalPropertyChangeListener listener : onSignalPropertyChangeListeners) {
                listener.onChannelConfigChanged(channelConfig);
            }
        }
    }

    /**
     * If {@code visible} is {@code true} channel at specified {@code channelIndex} is turned ON. If it's {@code false}
     * the channel is turned OFF.
     *
     * @throws IndexOutOfBoundsException Exception is thrown is specified {@code channelIndex} is out of bounds.
     */
    void setChannelVisible(int channelIndex, boolean visible) throws IndexOutOfBoundsException {
        if (channelIndex < 0 || channelIndex >= channelCount) throw new IndexOutOfBoundsException();
        if (channelConfig == null || channelConfig[channelIndex] == visible) return;

        channelConfig[channelIndex] = visible;

        // reset visible channel count so data is consistent
        visibleChannelCount = visible ? visibleChannelCount + 1 : visibleChannelCount - 1;

        if (onSignalPropertyChangeListeners != null) {
            for (OnSignalPropertyChangeListener listener : onSignalPropertyChangeListeners) {
                listener.onChannelConfigChanged(channelConfig);
            }
        }
    }

    /**
     * Returns index of selected channel of the processed signal.
     */
    public int getSelectedChannel() {
        return selectedChannel;
    }

    /**
     * Sets selected channel of the processed signal.
     *
     * @throws IndexOutOfBoundsException Exception is thrown is specified {@code channelIndex} is out of bounds.
     */
    void setSelectedChannel(int channelIndex) throws IndexOutOfBoundsException {
        if (channelIndex < 0 || channelIndex >= channelCount) throw new IndexOutOfBoundsException();

        selectedChannel = channelIndex;

        if (onSignalPropertyChangeListeners != null) {
            for (OnSignalPropertyChangeListener listener : onSignalPropertyChangeListeners) {
                listener.onChannelSelectionChanged(selectedChannel);
            }
        }
    }

    /**
     * Whether processed signal is being averaged or not.
     */
    public boolean isSignalAveraging() {
        return signalAveraging;
    }

    /**
     * Sets whether processed is being averaged or not.
     */
    void setSignalAveraging(boolean signalAveraging) {
        this.signalAveraging = signalAveraging;

        if (onSignalPropertyChangeListeners != null) {
            for (OnSignalPropertyChangeListener listener : onSignalPropertyChangeListeners) {
                listener.onSignalAveragingChanged(signalAveraging);
            }
        }
    }

    /**
     * Returns type of the trigger used for signal average triggering.
     */
    public @SignalAveragingTriggerType int getSignalAveragingTriggerType() {
        return signalAveragingTriggerType;
    }

    /**
     * Sets signal averaging trigger type. One of {@link SignalAveragingTriggerType} values.
     */
    void setSignalAveragingTriggerType(@SignalAveragingTriggerType int triggerType) {
        this.signalAveragingTriggerType = triggerType;

        if (onSignalPropertyChangeListeners != null) {
            for (OnSignalPropertyChangeListener listener : onSignalPropertyChangeListeners) {
                listener.onSignalAveragingTriggerTypeChanged(triggerType);
            }
        }
    }
}
