package com.backyardbrains.events;

import android.support.annotation.NonNull;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class OpenRecordingOptionsEvent {

    private final String filePath;

    public OpenRecordingOptionsEvent(@NonNull String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }
}
