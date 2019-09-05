package com.backyardbrains.events;

import androidx.annotation.NonNull;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class FindSpikesEvent {

    private final String filePath;

    public FindSpikesEvent(@NonNull String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }
}
