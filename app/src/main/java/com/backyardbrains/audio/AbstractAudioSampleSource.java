package com.backyardbrains.audio;

import android.support.annotation.Nullable;
import com.backyardbrains.data.processing.AbstractSampleSource;

/**
 * @author Tihomir Leka <ticapeca at gmail.com.
 */
public abstract class AbstractAudioSampleSource extends AbstractSampleSource {

    public AbstractAudioSampleSource(@Nullable OnSamplesReceivedListener listener) {
        super(listener);
    }

    @Override public int getType() {
        return Type.AUDIO;
    }
}
