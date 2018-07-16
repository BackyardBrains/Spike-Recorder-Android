package com.backyardbrains.events;

import android.support.annotation.NonNull;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
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
