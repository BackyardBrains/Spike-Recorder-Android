package com.backyardbrains.dsp.audio;

import android.content.Context;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioRouting;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.collection.ArraySet;
import android.util.SparseArray;
import com.backyardbrains.utils.AudioUtils;
import com.backyardbrains.utils.ObjectUtils;
import java.util.Arrays;
import java.util.Set;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class AudioHelper {

    private static final String TAG = makeLogTag(AudioHelper.class);

    private final SparseArray<AudioDeviceInfo> connectedDevices = new SparseArray<>();

    private AudioManager audioManager;
    private AudioDeviceCallback deviceCallback;
    private AudioDeviceInfo activeDevice;
    private AudioRecord audioRecord;
    private MicrophoneSignalSource micSource;

    public AudioHelper() {
        audioRecord = AudioUtils.createAudioRecord();
        if (audioRecord != null) {
            setupAudioRecord(audioRecord);

            micSource = new MicrophoneSignalSource(audioRecord);
        }
    }

    /**
     * Starts the helper.
     */
    public void start(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager = context.getSystemService(AudioManager.class);
            if (audioManager != null) {
                if (deviceCallback == null) {
                    deviceCallback = new AudioDeviceCallback() {
                        @Override public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
                            addDevices(addedDevices);
                        }

                        @Override public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
                            removeDevices(removedDevices);
                        }
                    };
                }
                audioManager.registerAudioDeviceCallback(deviceCallback, null);
            }
        }
    }

    /**
     * Stops the helper.
     */
    public void stop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (audioManager != null) audioManager.unregisterAudioDeviceCallback(deviceCallback);
        }
        audioRecord = null;
        if (micSource != null) micSource.requestStop();
        micSource = null;
    }

    /**
     * Returns active audio device, or {@code null} if none is active.
     */
    @Nullable public MicrophoneSignalSource getAudioDevice() {
        return micSource;
    }

    /**
     * Determines the
     */
    @SuppressWarnings("WeakerAccess") @RequiresApi(api = Build.VERSION_CODES.M) void addDevices(
        AudioDeviceInfo[] devices) {
        // add new audio devices to local collection
        for (AudioDeviceInfo device : devices) {
            // device needs to be source and of specific type
            if (device.isSource() && isSupportedType(device.getType())) {
                // if device is not already in the list add it
                if (this.connectedDevices.get(device.getId()) == null) {
                    this.connectedDevices.put(device.getId(), device);
                }
            }
        }
    }

    @SuppressWarnings("WeakerAccess") @RequiresApi(api = Build.VERSION_CODES.M) void removeDevices(
        AudioDeviceInfo[] devices) {
        // remove audio devices from local collection
        final Set<Integer> removedKeys = new ArraySet<>();
        for (AudioDeviceInfo device : devices)
            removedKeys.add(device.getId());
        int len = this.connectedDevices.size();
        for (int i = len - 1; i >= 0; i--) {
            if (removedKeys.contains(this.connectedDevices.valueAt(i).getId())) this.connectedDevices.removeAt(i);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M) private boolean isSupportedType(int type) {
        return type == AudioDeviceInfo.TYPE_WIRED_HEADSET || type == AudioDeviceInfo.TYPE_USB_DEVICE || (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && type == AudioDeviceInfo.TYPE_USB_HEADSET)
            || type == AudioDeviceInfo.TYPE_BUILTIN_MIC;
    }

    private void setupAudioRecord(@NonNull AudioRecord recorder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            recorder.addOnRoutingChangedListener(
                (AudioRouting.OnRoutingChangedListener) router -> updateAudioRecorder(router.getRoutedDevice()), null);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            recorder.addOnRoutingChangedListener(audioRecord -> updateAudioRecorder(audioRecord.getRoutedDevice()),
                null);
        }
    }

    @SuppressWarnings("WeakerAccess") @RequiresApi(api = Build.VERSION_CODES.M) void updateAudioRecorder(
        @Nullable AudioDeviceInfo routedDevice) {
        if (routedDevice == null) return;
        if (ObjectUtils.equals(routedDevice, activeDevice)) return;

        activeDevice = routedDevice;

        int sampleRate = AudioUtils.DEFAULT_SAMPLE_RATE;
        int channelMask = AudioUtils.DEFAULT_CHANNEL_IN_MASK;
        int encoding = AudioUtils.DEFAULT_ENCODING;
        int len = routedDevice.getSampleRates().length;
        int[] values = routedDevice.getSampleRates();
        if (len > 0) {
            boolean exists = false;
            for (int sr : values) {
                if (sampleRate == sr) {
                    exists = true;
                    break;
                }
            }
            if (!exists) sampleRate = values[len - 1];
        }
        len = routedDevice.getChannelMasks().length;
        values = routedDevice.getChannelMasks();
        if (len > 0) {
            boolean exists = false;
            for (int cm : values) {
                if (channelMask == cm) {
                    exists = true;
                    break;
                }
            }
            if (!exists) channelMask = values[len - 1];
        }
        len = routedDevice.getEncodings().length;
        values = routedDevice.getEncodings();
        if (len > 0) {
            boolean exists = false;
            for (int e : values) {
                if (encoding < e) {
                    exists = true;
                    break;
                }
            }
            if (!exists) encoding = values[len - 1];
        }

        // current AudioRecord configuration is not supported by the routed device so we need to update
        if (audioRecord != null && (audioRecord.getSampleRate() != sampleRate
            || audioRecord.getChannelConfiguration() != channelMask || audioRecord.getAudioFormat() != encoding)) {
            final AudioRecord ar = AudioUtils.createAudioRecord(sampleRate, channelMask, encoding,
                AudioRecord.getMinBufferSize(sampleRate, channelMask, encoding));
            if (ar != null) {
                if (micSource != null) audioRecord = micSource.updateRecorder(ar);
                setupAudioRecord(audioRecord);

                LOGD(TAG, "==============================");
                printDevices(new AudioDeviceInfo[] { routedDevice });
                LOGD(TAG, ">>>>>>>>>>>>>>>>>>>>>>>");
                LOGD(TAG, "SAMPLE RATE: " + audioRecord.getSampleRate());
                LOGD(TAG, "CHANNEL MASK: " + audioRecord.getChannelConfiguration());
                LOGD(TAG, "ENCODING: " + audioRecord.getAudioFormat());
                LOGD(TAG, "==============================");
            } else {
                // TODO: 10-Apr-19 INFORM USER THAT CONNECTING TO MIC FAILED AND THAT APP SHOULD BE RESTARTED
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M) private void printDevices(AudioDeviceInfo[] devices) {
        for (AudioDeviceInfo adi : devices) {
            LOGD(TAG, "ID: " + adi.getId());
            LOGD(TAG, "PRODUCT NAME: " + adi.getProductName());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) LOGD(TAG, "ADDRESS: " + adi.getAddress());
            LOGD(TAG, "SAMPLE RATES: " + Arrays.toString(adi.getSampleRates()));
            LOGD(TAG, "CHANNEL COUNTS: " + Arrays.toString(adi.getChannelCounts()));
            LOGD(TAG, "CHANNEL INDEX MASKS: " + Arrays.toString(adi.getChannelIndexMasks()));
            LOGD(TAG, "CHANNEL MASKS: " + Arrays.toString(adi.getChannelMasks()));
            LOGD(TAG, "ENCODING: " + Arrays.toString(adi.getEncodings()));
            LOGD(TAG, "TYPE: " + adi.getType());
            LOGD(TAG, "SINK: " + adi.isSink());
            LOGD(TAG, "SOURCE: " + adi.isSource());
        }
    }
}
