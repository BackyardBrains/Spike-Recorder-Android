package com.backyardbrains.events;

import android.support.annotation.NonNull;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
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
