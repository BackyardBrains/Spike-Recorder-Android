package com.backyardbrains.events;

import android.support.annotation.NonNull;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class PlayAudioFileEvent {

    private final String filePath;

    public PlayAudioFileEvent(@NonNull String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }
}
