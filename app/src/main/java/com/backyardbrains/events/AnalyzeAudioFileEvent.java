package com.backyardbrains.events;

import androidx.annotation.NonNull;
import com.backyardbrains.analysis.AnalysisType;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class AnalyzeAudioFileEvent {

    private final String filePath;
    private final @AnalysisType int type;

    public AnalyzeAudioFileEvent(@NonNull String filePath, @AnalysisType int type) {
        this.filePath = filePath;
        this.type = type;
    }

    public String getFilePath() {
        return filePath;
    }

    public @AnalysisType int getType() {
        return type;
    }
}
